import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { DataService } from '../../core/data.service';

export interface SendMailDialogData {
  recipients: string[];
  studentCount: number;
  gramPanchayat?: string;
  taluk?: string;
  district?: string;
}

/** Preview a GP mail (subject, body, recipients, attachment + student count) before sending. */
@Component({
  selector: 'app-send-mail-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSnackBarModule,
  ],
  template: `
    <h2 mat-dialog-title>Review mail before sending</h2>
    <mat-dialog-content class="body">
      <div class="meta">
        <div class="chip"><mat-icon>group</mat-icon> {{ d.studentCount }} student(s) attached</div>
        <div class="chip"><mat-icon>attach_file</mat-icon> GP Blue Print PDF attached</div>
      </div>

      <div class="field">
        <span class="lbl">To (Gram Panchayat mail ID{{ d.recipients.length === 1 ? '' : 's' }})</span>
        <div class="recips" *ngIf="d.recipients.length; else auto">
          <span class="pill" *ngFor="let r of d.recipients">{{ r }}</span>
        </div>
        <ng-template #auto><span class="muted">Auto-resolved from each student's mapped Gram Panchayat email.</span></ng-template>
      </div>

      <mat-form-field appearance="outline" class="full">
        <mat-label>Subject</mat-label>
        <input matInput [(ngModel)]="subject" />
      </mat-form-field>

      <mat-form-field appearance="outline" class="full">
        <mat-label>Message body</mat-label>
        <textarea matInput rows="8" [(ngModel)]="body"></textarea>
      </mat-form-field>

      <button mat-stroked-button (click)="previewPdf()" [disabled]="busy()">
        <mat-icon>picture_as_pdf</mat-icon> Preview attached PDF
      </button>
      <span class="hint" *ngIf="!d.gramPanchayat">
        <mat-icon>info</mat-icon> Select a Gram Panchayat filter to preview / attach its Blue Print PDF.
      </span>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" (click)="send()">
        <mat-icon>send</mat-icon> Send mail
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .body { display: flex; flex-direction: column; gap: 12px; min-width: 460px; max-width: 560px; }
    .meta { display: flex; gap: 10px; flex-wrap: wrap; }
    .chip { display: inline-flex; align-items: center; gap: 6px; background: #e9f3ec; color: #0E5132;
      border-radius: 999px; padding: 5px 12px; font-size: 13px; font-weight: 600; }
    .chip mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .field .lbl { font-size: 12px; color: #6b6b6b; display: block; margin-bottom: 4px; }
    .recips { display: flex; gap: 6px; flex-wrap: wrap; }
    .pill { background: #f0eee9; border-radius: 6px; padding: 3px 8px; font-size: 13px; }
    .muted { color: #888; font-size: 13px; }
    .full { width: 100%; }
    .hint { display: flex; align-items: center; gap: 6px; color: #b26a00; font-size: 12px; margin-top: 6px; }
    .hint mat-icon { font-size: 16px; width: 16px; height: 16px; }
  `],
})
export class SendMailDialogComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);
  private ref = inject(MatDialogRef<SendMailDialogComponent>);
  d = inject<SendMailDialogData>(MAT_DIALOG_DATA);
  busy = signal(false);

  subject = 'MSYEP — “Kaushalya Patha” student documents & GP Blue Print';
  body = `Dear Gram Panchayat,\n\n`
    + `Please find attached the “Kaushalya Patha” MSYEP student documents along with the GP Blue Print `
    + `packet for ${this.d.studentCount} student(s)`
    + (this.d.gramPanchayat ? ` under ${this.d.gramPanchayat} Gram Panchayat` : '') + `.\n\n`
    + `Kindly approve the same at your end.\n\nRegards,\nMSYEP Finance`;

  previewPdf(): void {
    if (!this.d.gramPanchayat) {
      this.snack.open('Select a Gram Panchayat filter first', 'OK', { duration: 2500 });
      return;
    }
    this.busy.set(true);
    this.data.gpBlueprintPdf({
      gramPanchayat: this.d.gramPanchayat, taluk: this.d.taluk, district: this.d.district,
    }).subscribe({
      next: (blob) => {
        this.busy.set(false);
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(e?.error?.message || 'Could not build PDF preview', 'OK', { duration: 3000 });
      },
    });
  }

  send(): void {
    this.ref.close({ subject: this.subject.trim(), body: this.body });
  }
}
