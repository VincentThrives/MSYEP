import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { DataService } from '../../core/data.service';

/**
 * Admin Signature — one signature, uploaded here, used as the giver/approval sign on EVERY generated
 * PDF: Franchise Certificate & MOU, Center Batch Approval, and the GP requisition + invoice.
 */
@Component({
  selector: 'app-franchise-settings',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatProgressBarModule, MatSnackBarModule],
  template: `
    <h1>Admin Signature</h1>
    <p class="lead">One signature, applied everywhere. Upload it once and it appears as the YKTK / Admin
      (giver) signature on every generated PDF — no redeploy needed.</p>

    <div class="card">
      <div class="card-head">
        <mat-icon>draw</mat-icon>
        <div>
          <h2>Signature</h2>
          <p>Used on: Franchise Certificate &amp; MOU, Center Batch Approval, and GP requisition &amp; invoice.</p>
        </div>
        <span class="pill" [class.on]="custom()">{{ custom() ? 'Custom uploaded' : 'Default' }}</span>
      </div>

      <mat-progress-bar *ngIf="busy()" mode="indeterminate"></mat-progress-bar>

      <div class="preview" *ngIf="previewUrl() as u">
        <img [src]="u" alt="Current admin signature" />
      </div>

      <div class="actions">
        <button mat-flat-button color="primary" (click)="picker.click()" [disabled]="busy()">
          <mat-icon>upload</mat-icon> {{ custom() ? 'Replace signature' : 'Upload signature' }}
        </button>
        <button mat-stroked-button *ngIf="custom()" (click)="reset()" [disabled]="busy()">
          <mat-icon>restart_alt</mat-icon> Reset to default
        </button>
        <input #picker type="file" hidden accept="image/png,image/jpeg" (change)="onFile($event)" />
      </div>

      <ul class="tips">
        <li><mat-icon>info</mat-icon> A PNG with a transparent background looks best — but a clear photo of a
          signature on white paper also works (the background is auto-removed).</li>
        <li><mat-icon>bolt</mat-icon> Takes effect immediately — every new PDF uses the latest signature.</li>
        <li><mat-icon>photo_size_select_large</mat-icon> Max 2&nbsp;MB.</li>
      </ul>
    </div>

    <div class="card muted">
      <div class="card-head">
        <mat-icon>verified_user</mat-icon>
        <div>
          <h2>Signing Certificate (PAdES)</h2>
          <p>A .pfx / .p12 certificate + password to cryptographically sign every document.</p>
        </div>
        <span class="pill">Pending</span>
      </div>
      <p class="hint"><mat-icon>info</mat-icon> Send the certificate file &amp; password to your developer to enable cryptographic signing.</p>
    </div>
  `,
  styles: [`
    :host { display: block; padding: 8px 4px; }
    h1 { margin: 0 0 4px; }
    .lead { color: #6b6b6b; margin: 0 0 20px; max-width: 640px; }
    .card { background: #fff; border: 1px solid #e6e6e6; border-radius: 14px; padding: 20px; margin-bottom: 18px; max-width: 640px; }
    .card.muted { opacity: .85; }
    .card-head { display: flex; align-items: flex-start; gap: 14px; }
    .card-head > mat-icon { color: #0E5132; background: #eef7f1; border-radius: 10px; padding: 8px; width: 40px; height: 40px; font-size: 24px; }
    .card-head h2 { margin: 0; font-size: 16px; }
    .card-head p { margin: 2px 0 0; font-size: 13px; color: #777; }
    .pill { margin-left: auto; font-size: 11px; font-weight: 700; letter-spacing: .04em; text-transform: uppercase;
      padding: 4px 10px; border-radius: 999px; background: #f0eee9; color: #8a8478; white-space: nowrap; }
    .pill.on { background: #e9f3ec; color: #0E5132; }
    mat-progress-bar { margin: 16px 0 0; border-radius: 4px; }
    .preview { margin: 16px 0 4px; padding: 14px; border: 1px dashed #d8d8d8; border-radius: 10px;
      background: #fbfbfa; display: inline-block; }
    .preview img { max-height: 90px; max-width: 320px; display: block; }
    .actions { display: flex; gap: 10px; margin-top: 16px; flex-wrap: wrap; }
    .tips { list-style: none; padding: 0; margin: 16px 0 0; }
    .tips li { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #999; margin-bottom: 6px; }
    .tips mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .hint { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #999; margin: 14px 0 0; }
    .hint mat-icon { font-size: 16px; width: 16px; height: 16px; }
  `],
})
export class FranchiseSettingsComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);

  custom = signal(false);
  previewUrl = signal<string | null>(null);
  busy = signal(false);

  constructor() {
    this.data.adminSignatureStatus().subscribe((s) => this.custom.set(!!s?.custom));
    this.loadPreview();
  }

  private loadPreview(): void {
    this.data.adminSignatureImage().subscribe({
      next: (blob) => {
        const old = this.previewUrl();
        if (old) URL.revokeObjectURL(old);
        this.previewUrl.set(URL.createObjectURL(blob));
      },
      error: () => this.previewUrl.set(null),
    });
  }

  onFile(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      this.snack.open('File exceeds the 2 MB limit', 'OK', { duration: 3000 });
      return;
    }
    this.busy.set(true);
    this.data.uploadAdminSignature(file).subscribe({
      next: (s) => {
        this.busy.set(false);
        this.custom.set(!!s?.custom);
        this.loadPreview();
        this.snack.open('Signature updated — it now applies to all PDFs.', 'OK', { duration: 3500 });
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(e?.error?.message || 'Upload failed', 'OK', { duration: 3000 });
      },
    });
  }

  reset(): void {
    if (!confirm('Reset to the default signature? This affects all PDFs.')) return;
    this.busy.set(true);
    this.data.resetAdminSignature().subscribe({
      next: (s) => {
        this.busy.set(false);
        this.custom.set(!!s?.custom);
        this.loadPreview();
        this.snack.open('Reverted to the default signature.', 'OK', { duration: 3000 });
      },
      error: () => this.busy.set(false),
    });
  }
}
