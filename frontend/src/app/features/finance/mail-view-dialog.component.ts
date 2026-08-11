import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { MailLog } from '../../core/models';

/** Read-only view of a sent Finance-wing mail, opened from the Sent Mail History. */
@Component({
  selector: 'app-mail-view-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>outgoing_mail</mat-icon> Sent mail
    </h2>
    <mat-dialog-content class="body">
      <div class="meta">
        <span class="chip"><mat-icon>schedule</mat-icon> {{ m.sentAt | date: 'dd-MM-yyyy HH:mm' }}</span>
        <span class="chip" [class.warn]="m.stub"><mat-icon>{{ m.stub ? 'info' : 'check_circle' }}</mat-icon> {{ m.status }}</span>
        <span class="chip" *ngIf="m.attachment"><mat-icon>attach_file</mat-icon> {{ m.attachment }}</span>
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
    .subj { font-weight: 600; }
    .msg { white-space: pre-wrap; background: #f7f9f8; border: 1px solid #e3ebe6; border-radius: 8px; padding: 8px 10px; }
    .muted { color: #99a; }
  `],
})
export class MailViewDialogComponent {
  m = inject<MailLog>(MAT_DIALOG_DATA);
}
