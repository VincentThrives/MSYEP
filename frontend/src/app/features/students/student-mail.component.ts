import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { DataService } from '../../core/data.service';
import { MailLog, Student } from '../../core/models';
import { MailComposeDialogComponent } from '../../shared/mail-compose-dialog.component';
import { MailViewDialogComponent } from '../finance/mail-view-dialog.component';

interface StudentRow { id: string; name: string; regNo: string; email: string; phone: string; center: string; }

/** Students Mail — send the document packet to selected students and view the sent-mail history. */
@Component({
  selector: 'app-student-mail',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule, MatCheckboxModule,
    MatTooltipModule, MatFormFieldModule, MatInputModule, MatSnackBarModule, MatDialogModule,
  ],
  template: `
    <div class="head"><h1>Students Mail — Documents</h1></div>

    <div class="bar">
      <button mat-flat-button color="primary" [disabled]="!selectedIds().length" (click)="sendSelected()">
        <mat-icon>send</mat-icon> Send documents
      </button>
      <span class="sel" *ngIf="selectedIds().length">{{ selectedIds().length }} student(s) selected</span>
      <span class="hint" *ngIf="!rows().length">No students with an email on file yet.</span>
    </div>

    <mat-form-field appearance="outline" class="search" *ngIf="rows().length">
      <mat-label>Search students</mat-label>
      <mat-icon matPrefix>search</mat-icon>
      <input matInput [ngModel]="search()" (ngModelChange)="search.set($event)" placeholder="name, reg no, email, phone…" />
    </mat-form-field>

    <table mat-table [dataSource]="filteredRows()" class="mat-elevation-z1 full" *ngIf="rows().length">
      <ng-container matColumnDef="select">
        <th mat-header-cell *matHeaderCellDef>
          <mat-checkbox [checked]="allChecked()" (change)="toggleAll($event.checked)"></mat-checkbox>
        </th>
        <td mat-cell *matCellDef="let r">
          <mat-checkbox [checked]="selected.has(r.id)" (change)="toggleOne(r.id, $event.checked)"></mat-checkbox>
        </td>
      </ng-container>
      <ng-container matColumnDef="reg">
        <th mat-header-cell *matHeaderCellDef>Reg. No</th>
        <td mat-cell *matCellDef="let r">{{ r.regNo || '—' }}</td>
      </ng-container>
      <ng-container matColumnDef="name">
        <th mat-header-cell *matHeaderCellDef>Student</th>
        <td mat-cell *matCellDef="let r">{{ r.name }}</td>
      </ng-container>
      <ng-container matColumnDef="email">
        <th mat-header-cell *matHeaderCellDef>Email</th>
        <td mat-cell *matCellDef="let r">{{ r.email }}</td>
      </ng-container>
      <ng-container matColumnDef="phone">
        <th mat-header-cell *matHeaderCellDef>Phone</th>
        <td mat-cell *matCellDef="let r">{{ r.phone || '—' }}</td>
      </ng-container>
      <ng-container matColumnDef="center">
        <th mat-header-cell *matHeaderCellDef>Center / College</th>
        <td mat-cell *matCellDef="let r">{{ r.center || '—' }}</td>
      </ng-container>
      <ng-container matColumnDef="send">
        <th mat-header-cell *matHeaderCellDef>Send</th>
        <td mat-cell *matCellDef="let r">
          <button mat-icon-button color="primary" matTooltip="Send to this student" (click)="sendOne(r)">
            <mat-icon>send</mat-icon>
          </button>
        </td>
      </ng-container>
      <tr mat-header-row *matHeaderRowDef="cols"></tr>
      <tr mat-row *matRowDef="let row; columns: cols"></tr>
    </table>

    <div class="history" *ngIf="mailHistory().length">
      <h3><mat-icon>history</mat-icon> Sent Mail History</h3>
      <p class="hint">Click a row to open the full sent mail.</p>
      <table mat-table [dataSource]="mailHistory()" class="mat-elevation-z1 full hist">
        <ng-container matColumnDef="sentAt">
          <th mat-header-cell *matHeaderCellDef>Sent</th>
          <td mat-cell *matCellDef="let m">{{ m.sentAt | date: 'dd-MM-yyyy HH:mm' }}</td>
        </ng-container>
        <ng-container matColumnDef="recipients">
          <th mat-header-cell *matHeaderCellDef>Recipients</th>
          <td mat-cell *matCellDef="let m">{{ m.recipients?.join(', ') || '—' }}</td>
        </ng-container>
        <ng-container matColumnDef="students">
          <th mat-header-cell *matHeaderCellDef>Students</th>
          <td mat-cell *matCellDef="let m">{{ m.studentNames?.length || 0 }}</td>
        </ng-container>
        <ng-container matColumnDef="attach">
          <th mat-header-cell *matHeaderCellDef>Attachment</th>
          <td mat-cell *matCellDef="let m">{{ m.attachment || '—' }}</td>
        </ng-container>
        <ng-container matColumnDef="status">
          <th mat-header-cell *matHeaderCellDef>Status</th>
          <td mat-cell *matCellDef="let m"><span class="badge" [class.stub]="m.stub">{{ m.status }}</span></td>
        </ng-container>
        <ng-container matColumnDef="del">
          <th mat-header-cell *matHeaderCellDef></th>
          <td mat-cell *matCellDef="let m">
            <button mat-icon-button color="warn" matTooltip="Delete entry" (click)="deleteMail(m); $event.stopPropagation()">
              <mat-icon>delete</mat-icon>
            </button>
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="histCols"></tr>
        <tr mat-row *matRowDef="let row; columns: histCols" class="hist-row" (click)="openMail(row)"></tr>
      </table>
    </div>
  `,
  styles: [`
    .head h1 { color: #0E5132; }
    .bar { display: flex; align-items: center; gap: 14px; margin: 8px 0 16px; flex-wrap: wrap; }
    .sel { color: #0E5132; font-weight: 600; }
    .hint { color: #789; font-size: 13px; }
    .search { width: 100%; max-width: 460px; display: block; margin-bottom: 4px; }
    table.full { width: 100%; }
    .history { margin-top: 28px; }
    .history h3 { display: flex; align-items: center; gap: 6px; color: #1b5e20; margin: 0 0 2px; }
    .history h3 mat-icon { font-size: 20px; width: 20px; height: 20px; }
    .hist-row { cursor: pointer; }
    .hist-row:hover { background: #f2f7f4; }
    .badge { background: #e8f5e9; color: #1b5e20; padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: 600; }
    .badge.stub { background: #fff4e5; color: #8a5a00; }
  `],
})
export class StudentMailComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);
  private dialog = inject(MatDialog);

  cols = ['select', 'reg', 'name', 'email', 'phone', 'center', 'send'];
  histCols = ['sentAt', 'recipients', 'students', 'attach', 'status', 'del'];

  rows = signal<StudentRow[]>([]);
  search = signal('');
  filteredRows = computed(() => {
    const q = this.search().toLowerCase().trim();
    const rs = this.rows();
    return q ? rs.filter((r) => `${r.name} ${r.regNo} ${r.email} ${r.phone}`.toLowerCase().includes(q)) : rs;
  });
  mailHistory = signal<MailLog[]>([]);
  selected = new Set<string>();
  selectedIds = signal<string[]>([]);
  allChecked = computed(() => {
    const fr = this.filteredRows();
    return fr.length > 0 && fr.every((r) => this.selected.has(r.id));
  });

  constructor() {
    this.load();
    this.loadHistory();
  }

  private load(): void {
    this.data.students().subscribe({
      next: (ss: Student[]) => this.rows.set(
        ss.map((s) => ({
          id: s.id!, name: s.name, regNo: (s as any).registerNo || '',
          email: ((s as any).email || '').trim(), phone: (s as any).phone || '',
          center: (s as any).collegeName || (s as any).centerName || '',
        })).filter((r) => r.id && r.email),
      ),
      error: (e) => this.snack.open(e?.error?.message || 'Could not load students', 'OK', { duration: 3000 }),
    });
  }

  private loadHistory(): void {
    this.data.studentMailHistory().subscribe({
      next: (h) => this.mailHistory.set(h || []),
      error: () => {},
    });
  }

  toggleAll(checked: boolean): void {
    const fr = this.filteredRows();
    if (checked) fr.forEach((r) => this.selected.add(r.id));
    else fr.forEach((r) => this.selected.delete(r.id));
    this.selectedIds.set([...this.selected]);
  }
  toggleOne(id: string, checked: boolean): void {
    checked ? this.selected.add(id) : this.selected.delete(id);
    this.selectedIds.set([...this.selected]);
  }

  sendOne(r: StudentRow): void { this.dispatch([r.id], [r.email]); }
  sendSelected(): void {
    const ids = this.selectedIds();
    const emails = this.rows().filter((r) => this.selected.has(r.id)).map((r) => r.email);
    this.dispatch(ids, emails);
  }

  private dispatch(studentIds: string[], recipients: string[]): void {
    if (!studentIds.length) return;
    const ref = this.dialog.open(MailComposeDialogComponent, {
      width: '600px', maxWidth: '92vw',
      data: {
        recipients, targetCount: studentIds.length, targetNoun: 'student',
        attachmentLabel: 'Document packet',
        defaultSubject: 'MSYEP — Your Documents',
        defaultBody: 'Dear Student,\n\nPlease find attached your MSYEP document packet.\n\nRegards,\nYKTK · KP-MSYEP',
      },
    });
    ref.afterClosed().subscribe((res?: { subject: string; body: string }) => {
      if (!res) return;
      this.data.studentSendMail({ studentIds, subject: res.subject, body: res.body }).subscribe({
        next: (r) => {
          const sent = Object.values(r).filter((v) => v.startsWith('SENT')).length;
          this.snack.open(`Mail dispatched: ${sent}/${studentIds.length}`, 'OK', { duration: 3500 });
          this.loadHistory();
        },
        error: (e) => this.snack.open(e?.error?.message || 'Send failed', 'OK', { duration: 3500 }),
      });
    });
  }

  openMail(m: MailLog): void {
    this.dialog.open(MailViewDialogComponent, { data: m, width: '600px', maxWidth: '92vw' });
  }

  deleteMail(m: MailLog): void {
    if (!confirm('Delete this sent-mail entry? This cannot be undone.')) return;
    this.data.deleteMailLog(m.id).subscribe({
      next: () => { this.snack.open('Entry deleted', 'OK', { duration: 2000 }); this.loadHistory(); },
      error: (e) => this.snack.open(e?.error?.message || 'Delete failed', 'OK', { duration: 3000 }),
    });
  }
}
