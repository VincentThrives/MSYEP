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
import { MailLog, Zone } from '../../core/models';
import { MailComposeDialogComponent } from '../../shared/mail-compose-dialog.component';
import { MailViewDialogComponent } from '../finance/mail-view-dialog.component';

interface ZoneRow { id: string; name: string; org: string; email: string; }

/** Zone Mail — send the Franchise Certificate + MOU to selected zones and view the sent-mail history. */
@Component({
  selector: 'app-zone-mail',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule, MatCheckboxModule,
    MatTooltipModule, MatFormFieldModule, MatInputModule, MatSnackBarModule, MatDialogModule,
  ],
  template: `
    <div class="head"><h1>Zone Mail — Franchise Documents</h1></div>

    <div class="bar">
      <button mat-flat-button color="primary" [disabled]="!selectedIds().length" (click)="sendSelected()">
        <mat-icon>send</mat-icon> Send Certificate + MOU
      </button>
      <span class="sel" *ngIf="selectedIds().length">{{ selectedIds().length }} zone(s) selected</span>
      <span class="hint" *ngIf="!rows().length">No zones with an email on file yet.</span>
    </div>

    <mat-form-field appearance="outline" class="search" *ngIf="rows().length">
      <mat-label>Search zones</mat-label>
      <mat-icon matPrefix>search</mat-icon>
      <input matInput [ngModel]="search()" (ngModelChange)="search.set($event)" placeholder="name, organization, email…" />
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
      <ng-container matColumnDef="name">
        <th mat-header-cell *matHeaderCellDef>Zone</th>
        <td mat-cell *matCellDef="let r">{{ r.name }}</td>
      </ng-container>
      <ng-container matColumnDef="org">
        <th mat-header-cell *matHeaderCellDef>Organization</th>
        <td mat-cell *matCellDef="let r">{{ r.org || '—' }}</td>
      </ng-container>
      <ng-container matColumnDef="email">
        <th mat-header-cell *matHeaderCellDef>Email</th>
        <td mat-cell *matCellDef="let r">{{ r.email }}</td>
      </ng-container>
      <ng-container matColumnDef="send">
        <th mat-header-cell *matHeaderCellDef>Send</th>
        <td mat-cell *matCellDef="let r">
          <button mat-icon-button color="primary" matTooltip="Send to this zone" (click)="sendOne(r)">
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
        <ng-container matColumnDef="zones">
          <th mat-header-cell *matHeaderCellDef>Zones</th>
          <td mat-cell *matCellDef="let m">{{ m.studentNames?.length || 0 }}</td>
        </ng-container>
        <ng-container matColumnDef="status">
          <th mat-header-cell *matHeaderCellDef>Status</th>
          <td mat-cell *matCellDef="let m"><span class="badge" [class.stub]="m.stub">{{ m.status }}</span></td>
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
    .search { width: 100%; max-width: 420px; display: block; margin-bottom: 4px; }
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
export class ZoneMailComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);
  private dialog = inject(MatDialog);

  cols = ['select', 'name', 'org', 'email', 'send'];
  histCols = ['sentAt', 'recipients', 'zones', 'status'];

  rows = signal<ZoneRow[]>([]);
  search = signal('');
  filteredRows = computed(() => {
    const q = this.search().toLowerCase().trim();
    const rs = this.rows();
    return q ? rs.filter((r) => `${r.name} ${r.org} ${r.email}`.toLowerCase().includes(q)) : rs;
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
    this.data.zones().subscribe({
      next: (zs: Zone[]) => this.rows.set(
        zs.map((z) => ({
          id: z.id!, name: z.name,
          org: (z as any).organizationName || (z as any).franchiseeName || (z as any).ownerName || '',
          email: ((z as any).contactEmail || (z as any).email || '').trim(),
        })).filter((r) => r.id && r.email),
      ),
      error: (e) => this.snack.open(e?.error?.message || 'Could not load zones', 'OK', { duration: 3000 }),
    });
  }

  private loadHistory(): void {
    this.data.zoneMailHistory().subscribe({
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

  sendOne(r: ZoneRow): void { this.dispatch([r.id], [r.email]); }
  sendSelected(): void {
    const ids = this.selectedIds();
    const emails = this.rows().filter((r) => this.selected.has(r.id)).map((r) => r.email);
    this.dispatch(ids, emails);
  }

  private dispatch(zoneIds: string[], recipients: string[]): void {
    if (!zoneIds.length) return;
    const ref = this.dialog.open(MailComposeDialogComponent, {
      width: '600px', maxWidth: '92vw',
      data: {
        recipients, targetCount: zoneIds.length, targetNoun: 'zone',
        attachmentLabel: 'Certificate + MOU',
        defaultSubject: 'KP-MSYEP Franchise — Certificate & MOU',
        defaultBody: 'Dear Franchise,\n\nPlease find attached your KP-MSYEP Franchise Certificate and the signed MOU.\n\nRegards,\nYKTK · KP-MSYEP',
      },
    });
    ref.afterClosed().subscribe((res?: { subject: string; body: string }) => {
      if (!res) return;
      this.data.zoneSendMail({ zoneIds, subject: res.subject, body: res.body }).subscribe({
        next: (r) => {
          const sent = Object.values(r).filter((v) => v.startsWith('SENT')).length;
          this.snack.open(`Mail dispatched: ${sent}/${zoneIds.length}`, 'OK', { duration: 3500 });
          this.loadHistory();
        },
        error: (e) => this.snack.open(e?.error?.message || 'Send failed', 'OK', { duration: 3500 }),
      });
    });
  }

  openMail(m: MailLog): void {
    this.dialog.open(MailViewDialogComponent, { data: m, width: '600px', maxWidth: '92vw' });
  }
}
