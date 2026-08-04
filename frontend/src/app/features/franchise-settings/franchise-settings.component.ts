import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { DataService } from '../../core/data.service';

@Component({
  selector: 'app-franchise-settings',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatSnackBarModule],
  template: `
    <h1>Franchise Settings</h1>
    <p class="lead">One-time assets applied to every Franchise Certificate &amp; MOU.</p>

    <div class="card">
      <div class="card-head">
        <mat-icon>draw</mat-icon>
        <div>
          <h2>Giver (YKTK) Signature</h2>
          <p>Stamped on the signature block and footer of every franchise document.</p>
        </div>
        <span class="pill" [class.on]="status()?.giverSignature">
          {{ status()?.giverSignature ? 'Uploaded' : 'Not set' }}
        </span>
      </div>

      <div class="preview" *ngIf="previewUrl() as u">
        <img [src]="u" alt="Giver signature preview" />
      </div>

      <div class="actions">
        <button mat-flat-button color="primary" (click)="picker.click()" [disabled]="busy()">
          <mat-icon>upload</mat-icon> {{ status()?.giverSignature ? 'Replace' : 'Upload' }} signature
        </button>
        <button mat-stroked-button *ngIf="status()?.giverSignature" (click)="remove()" [disabled]="busy()">
          <mat-icon>delete</mat-icon> Remove
        </button>
        <input #picker type="file" hidden accept="image/png,image/jpeg" (change)="onFile($event)" />
      </div>
      <p class="hint"><mat-icon>info</mat-icon> PNG with a transparent background works best. Max 2&nbsp;MB.</p>
    </div>

    <div class="card">
      <div class="card-head">
        <mat-icon>approval</mat-icon>
        <div>
          <h2>YKTK Approval Signature</h2>
          <p>Stamped as the "Approved by / Admin" sign on every Center Batch Approval PDF.</p>
        </div>
        <span class="pill" [class.on]="status()?.approvalSignature">
          {{ status()?.approvalSignature ? 'Uploaded' : 'Not set' }}
        </span>
      </div>

      <div class="preview" *ngIf="approvalPreviewUrl() as u">
        <img [src]="u" alt="Approval signature preview" />
      </div>

      <div class="actions">
        <button mat-flat-button color="primary" (click)="apicker.click()" [disabled]="busy()">
          <mat-icon>upload</mat-icon> {{ status()?.approvalSignature ? 'Replace' : 'Upload' }} signature
        </button>
        <button mat-stroked-button *ngIf="status()?.approvalSignature" (click)="removeApproval()" [disabled]="busy()">
          <mat-icon>delete</mat-icon> Remove
        </button>
        <input #apicker type="file" hidden accept="image/png,image/jpeg" (change)="onApprovalFile($event)" />
      </div>
      <p class="hint"><mat-icon>info</mat-icon> PNG with a transparent background works best. Max 2&nbsp;MB.</p>
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
    .lead { color: #6b6b6b; margin: 0 0 20px; }
    .card { background: #fff; border: 1px solid #e6e6e6; border-radius: 14px; padding: 20px; margin-bottom: 18px; max-width: 620px; }
    .card.muted { opacity: .85; }
    .card-head { display: flex; align-items: flex-start; gap: 14px; }
    .card-head > mat-icon { color: #0E5132; background: #eef7f1; border-radius: 10px; padding: 8px; width: 40px; height: 40px; font-size: 24px; }
    .card-head h2 { margin: 0; font-size: 16px; }
    .card-head p { margin: 2px 0 0; font-size: 13px; color: #777; }
    .pill { margin-left: auto; font-size: 11px; font-weight: 700; letter-spacing: .04em; text-transform: uppercase;
      padding: 4px 10px; border-radius: 999px; background: #f0eee9; color: #8a8478; white-space: nowrap; }
    .pill.on { background: #e9f3ec; color: #0E5132; }
    .preview { margin: 16px 0 4px; padding: 14px; border: 1px dashed #d8d8d8; border-radius: 10px;
      background: #fbfbfa; display: inline-block; }
    .preview img { max-height: 90px; max-width: 320px; display: block; }
    .actions { display: flex; gap: 10px; margin-top: 16px; flex-wrap: wrap; }
    .hint { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #999; margin: 14px 0 0; }
    .hint mat-icon { font-size: 16px; width: 16px; height: 16px; }
  `],
})
export class FranchiseSettingsComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);

  status = signal<{ giverSignature: boolean; approvalSignature: boolean } | null>(null);
  previewUrl = signal<string | null>(null);
  approvalPreviewUrl = signal<string | null>(null);
  busy = signal(false);

  constructor() {
    this.data.franchiseSettings().subscribe((s) => this.status.set(s));
  }

  onFile(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      this.snack.open('File exceeds 2 MB limit', 'OK', { duration: 3000 });
      return;
    }
    this.busy.set(true);
    this.previewUrl.set(URL.createObjectURL(file));
    this.data.uploadGiverSignature(file).subscribe({
      next: (s) => {
        this.busy.set(false);
        this.patch(s);
        this.snack.open('Giver signature saved — it will appear on all franchise documents', 'OK', { duration: 3500 });
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(e?.error?.message || 'Upload failed', 'OK', { duration: 3000 });
      },
    });
  }

  remove(): void {
    this.busy.set(true);
    this.data.removeGiverSignature().subscribe({
      next: (s) => { this.busy.set(false); this.patch(s); this.previewUrl.set(null); },
      error: () => this.busy.set(false),
    });
  }

  onApprovalFile(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      this.snack.open('File exceeds 2 MB limit', 'OK', { duration: 3000 });
      return;
    }
    this.busy.set(true);
    this.approvalPreviewUrl.set(URL.createObjectURL(file));
    this.data.uploadApprovalSignature(file).subscribe({
      next: (s) => {
        this.busy.set(false);
        this.patch(s);
        this.snack.open('Approval signature saved — it will appear on all Batch Approval PDFs', 'OK', { duration: 3500 });
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(e?.error?.message || 'Upload failed', 'OK', { duration: 3000 });
      },
    });
  }

  removeApproval(): void {
    this.busy.set(true);
    this.data.removeApprovalSignature().subscribe({
      next: (s) => { this.busy.set(false); this.patch(s); this.approvalPreviewUrl.set(null); },
      error: () => this.busy.set(false),
    });
  }

  /** Merge a partial status update (each endpoint returns only its own flag). */
  private patch(s: Partial<{ giverSignature: boolean; approvalSignature: boolean }>): void {
    this.status.update((cur) => ({
      giverSignature: s.giverSignature ?? cur?.giverSignature ?? false,
      approvalSignature: s.approvalSignature ?? cur?.approvalSignature ?? false,
    }));
  }
}
