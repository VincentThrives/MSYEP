import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { DataService } from '../../core/data.service';
import { MailLog } from '../../core/models';

/** Read-only view of a sent Finance-wing mail, opened from the Sent Mail History. */
@Component({
  selector: 'app-mail-view-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatSnackBarModule],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>outgoing_mail</mat-icon> Sent mail
    </h2>
    <mat-dialog-content class="body">
      <div class="meta">
        <span class="chip"><mat-icon>schedule</mat-icon> {{ m.sentAt | date: 'dd-MM-yyyy HH:mm' }}</span>
        <span class="chip" [class.warn]="m.stub"><mat-icon>{{ m.stub ? 'info' : 'check_circle' }}</mat-icon> {{ m.status }}</span>
      </div>

      <div class="field" *ngIf="m.attachmentNames?.length || m.attachment">
        <span class="lbl">Attachments <span class="muted">— click to open</span></span>
        <div class="atts" *ngIf="m.attachmentNames?.length; else plainAtt">
          <button *ngFor="let a of m.attachmentNames; let i = index" type="button" class="attpill" (click)="openAttachment(i)">
            <mat-icon>picture_as_pdf</mat-icon> {{ a }}
          </button>
        </div>
        <ng-template #plainAtt><span class="pill">{{ m.attachment }}</span></ng-template>
      </div>

      <div class="field">
        <span class="lbl">To</span>
        <div class="recips" *ngIf="m.recipients?.length; else noRecip">
          <span class="pill" *ngFor="let r of m.recipients">{{ r }}</span>
        </div>
        <ng-template #noRecip><span class="muted">— auto-mapped GP / none —</span></ng-template>
      </div>

      <div class="field" *ngIf="m.gramPanchayat">
        <span class="lbl">Gram Panchayat</span>
        <div>{{ m.gramPanchayat }}<span *ngIf="m.taluk"> · {{ m.taluk }}</span><span *ngIf="m.district"> · {{ m.district }}</span></div>
      </div>

      <div class="field">
        <span class="lbl">Subject</span>
        <div class="subj">{{ m.subject }}</div>
      </div>

      <div class="field" *ngIf="m.body">
        <span class="lbl">Message</span>
        <div class="msg">{{ m.body }}</div>
      </div>

      <div class="field">
        <span class="lbl">Students ({{ m.studentNames?.length || 0 }})</span>
        <div class="recips">
          <span class="pill s" *ngFor="let n of m.studentNames">{{ n || '—' }}</span>
        </div>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-flat-button color="primary" mat-dialog-close>Close</button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2 { display: flex; align-items: center; gap: 8px; }
    .body { display: flex; flex-direction: column; gap: 14px; min-width: 340px; }
    .meta { display: flex; flex-wrap: wrap; gap: 8px; }
    .chip { display: inline-flex; align-items: center; gap: 4px; background: #eef3f0; color: #1b5e20;
      padding: 4px 10px; border-radius: 16px; font-size: 12px; font-weight: 600; }
    .chip mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .chip.warn { background: #fff4e5; color: #8a5a00; }
    .field { display: flex; flex-direction: column; gap: 4px; }
    .lbl { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: .04em; color: #789; }
    .recips { display: flex; flex-wrap: wrap; gap: 6px; }
    .pill { background: #e8f0ea; padding: 3px 10px; border-radius: 14px; font-size: 13px; }
    .pill.s { background: #eef2f7; }
    .atts { display: flex; flex-wrap: wrap; gap: 8px; }
    .attpill { display: inline-flex; align-items: center; gap: 6px; background: #e8f0fb; color: #14448c;
      border: 1px solid #cfe0f7; border-radius: 8px; padding: 5px 12px; font-size: 13px; font-weight: 600; cursor: pointer; }
    .attpill:hover { background: #d8e7fb; }
    .attpill mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .subj { font-weight: 600; }
    .msg { white-space: pre-wrap; background: #f7f9f8; border: 1px solid #e3ebe6; border-radius: 8px; padding: 8px 10px; }
    .muted { color: #99a; }
  `],
})
export class MailViewDialogComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);
  m = inject<MailLog>(MAT_DIALOG_DATA);

  /** Open the exact PDF that was attached to this sent mail. */
  openAttachment(index: number): void {
    this.data.mailAttachment(this.m.id, index).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (e) => this.snack.open(e?.error?.message || 'Could not open attachment', 'OK', { duration: 3000 }),
    });
  }
}
