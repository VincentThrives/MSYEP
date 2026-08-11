import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface MailComposeData {
  recipients: string[];
  targetCount: number;
  targetNoun: string;       // e.g. 'zone', 'center'
  attachmentLabel: string;  // e.g. 'Certificate + MOU', 'Batch Approval PDF'
  defaultSubject: string;
  defaultBody: string;
}

/** Channel-agnostic compose/preview dialog (recipients, subject, body) before sending. */
@Component({
  selector: 'app-mail-compose-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Review mail before sending</h2>
    <mat-dialog-content class="body">
      <div class="meta">
        <div class="chip"><mat-icon>group</mat-icon> {{ d.targetCount }} {{ d.targetNoun }}{{ d.targetCount === 1 ? '' : 's' }}</div>
        <div class="chip"><mat-icon>attach_file</mat-icon> {{ d.attachmentLabel }} attached</div>
      </div>

      <div class="field">
        <span class="lbl">To ({{ d.recipients.length }} mail ID{{ d.recipients.length === 1 ? '' : 's' }})</span>
        <div class="recips" *ngIf="d.recipients.length; else none">
          <span class="pill" *ngFor="let r of d.recipients">{{ r }}</span>
        </div>
        <ng-template #none><span class="muted">No email on file for the selected {{ d.targetNoun }}(s).</span></ng-template>
      </div>

      <mat-form-field appearance="outline" class="full">
        <mat-label>Subject</mat-label>
        <input matInput [(ngModel)]="subject" />
      </mat-form-field>
      <mat-form-field appearance="outline" class="full">
        <mat-label>Message body</mat-label>
        <textarea matInput rows="8" [(ngModel)]="body"></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" (click)="send()"><mat-icon>send</mat-icon> Send mail</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .body { display: flex; flex-direction: column; gap: 12px; min-width: 440px; max-width: 560px; }
    .meta { display: flex; gap: 10px; flex-wrap: wrap; }
    .chip { display: inline-flex; align-items: center; gap: 6px; background: #e9f3ec; color: #0E5132;
      border-radius: 999px; padding: 5px 12px; font-size: 13px; font-weight: 600; }
    .chip mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .field .lbl { font-size: 12px; color: #6b6b6b; display: block; margin-bottom: 4px; }
    .recips { display: flex; gap: 6px; flex-wrap: wrap; }
    .pill { background: #f0eee9; border-radius: 6px; padding: 3px 8px; font-size: 13px; }
    .muted { color: #888; font-size: 13px; }
    .full { width: 100%; }
  `],
})
export class MailComposeDialogComponent {
  private ref = inject(MatDialogRef<MailComposeDialogComponent>);
  d = inject<MailComposeData>(MAT_DIALOG_DATA);
  subject = this.d.defaultSubject;
  body = this.d.defaultBody;

  send(): void {
    this.ref.close({ subject: this.subject.trim(), body: this.body });
  }
}
