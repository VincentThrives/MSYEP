import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { DataService } from '../../core/data.service';
import { Center, FinanceRow, GramPanchayat, MailLog } from '../../core/models';
import { SearchSelectComponent } from '../../shared/search-select.component';
import { MultiSearchSelectComponent } from '../../shared/multi-search-select.component';
import { LocationPickerComponent } from '../../shared/location-picker.component';
import { SendMailDialogComponent } from './send-mail-dialog.component';
import { MailViewDialogComponent } from './mail-view-dialog.component';

@Component({
  selector: 'app-finance-table',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule, SearchSelectComponent,
    MultiSearchSelectComponent, LocationPickerComponent,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule,
    MatTooltipModule, MatSnackBarModule, MatDialogModule,
  ],
  templateUrl: './finance-table.component.html',
  styleUrl: './finance-table.component.scss',
})
export class FinanceTableComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);
  private dialog = inject(MatDialog);

  cols = ['select', 'serialNo', 'studentName', 'district', 'taluk', 'gramPanchayat',
    'centerName', 'docs', 'send'];

  rows = signal<FinanceRow[]>([]);
  centers = signal<Center[]>([]);
  filters = signal<{ districts: string[]; taluks: string[]; gramPanchayats: string[] }>(
    { districts: [], taluks: [], gramPanchayats: [] });
  gpEmail = signal<string>('');

  /** GP mail IDs managed under Finance → Mail; shown as a searchable multi-select. */
  gpMails = signal<GramPanchayat[]>([]);
  selectedMails = signal<string[]>([]);

  /** Sent-mail history — click a row to open the full mail. */
  mailHistory = signal<MailLog[]>([]);
  historyCols = ['sentAt', 'recipients', 'gp', 'students', 'status', 'del'];

  filter: { district?: string; taluk?: string; gramPanchayat?: string; centerId?: string } = {};
  selected = new Set<string>();
  selectedIds = signal<string[]>([]);

  allSelected = computed(() => this.rows().length > 0 && this.selectedIds().length === this.rows().length);
  someSelected = computed(() => this.selectedIds().length > 0 && !this.allSelected());

  constructor() {
    this.data.financeFilters().subscribe({
      next: (f) => this.filters.set(f),
      error: (e) => this.err('filters', e),
    });
    this.data.centers().subscribe({ next: (c) => this.centers.set(c), error: (e) => this.err('centers', e) });
    this.loadGpMails();
    this.loadHistory();
    this.fetch();
  }

  private err(what: string, e: any): void {
    this.snack.open(`Finance: couldn't load ${what} — ${e?.error?.message || e?.message || 'request failed'}`,
      'OK', { duration: 5000 });
  }

  private loadGpMails(): void {
    this.data.gramPanchayats().subscribe({
      next: (r) => this.gpMails.set(r.filter((g) => (g.email || '').trim())),
      error: (e) => this.err('GP mail IDs', e),
    });
  }

  private loadHistory(): void {
    this.data.financeMailHistory().subscribe({
      next: (h) => this.mailHistory.set(h || []),
      error: (e) => this.err('mail history', e),
    });
  }

  /** Open a past sent mail in a read-only dialog. */
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

  /**
   * Options for the mail multi-select — a memoized computed so the array reference is STABLE
   * between change-detection cycles. A plain function here returns a new array every call, which
   * (bound to a signal input) drives an endless change-detection loop that freezes the page.
   */
  mailOptions = computed(() => this.gpMails().map((g) => ({
    email: g.email!,
    label: g.name ? `${g.name} — ${g.email}` : g.email!,
  })));

  fetch(): void {
    this.data.financeStudents(this.filter).subscribe({
      next: (r) => {
        this.rows.set(r);
        this.selected.clear();
        this.syncSelected();
      },
      error: (e) => this.err('students', e),
    });
  }

  onGpChange(): void {
    if (this.filter.gramPanchayat) {
      this.data.gpEmail(this.filter.gramPanchayat).subscribe((r) =>
        this.gpEmail.set(r.email && r.email !== 'null' ? r.email : ''));
    } else {
      this.gpEmail.set('');
    }
    this.fetch();
  }

  clear(): void {
    this.filter = {};
    this.gpEmail.set('');
    this.fetch();
  }

  toggleAll(checked: boolean): void {
    if (checked) this.rows().forEach((r) => this.selected.add(r.studentId));
    else this.selected.clear();
    this.syncSelected();
  }

  toggleOne(id: string, checked: boolean): void {
    checked ? this.selected.add(id) : this.selected.delete(id);
    this.syncSelected();
  }

  private syncSelected(): void {
    this.selectedIds.set([...this.selected]);
  }

  sendOne(r: FinanceRow): void {
    this.dispatch([r.studentId]);
  }

  /** Send selected students' documents — to the picked GP mail IDs, or each student's auto-mapped GP if none picked. */
  sendSelected(): void {
    this.dispatch(this.selectedIds(), this.selectedMails().length ? this.selectedMails() : undefined);
  }

  /** Download the GP Blue Print financial packet for the current Gram Panchayat filter. */
  downloadGpBlueprint(): void {
    if (!this.filter.gramPanchayat) {
      this.snack.open('Select a Gram Panchayat filter first', 'OK', { duration: 2500 });
      return;
    }
    this.data.gpBlueprintPdf({
      gramPanchayat: this.filter.gramPanchayat, taluk: this.filter.taluk, district: this.filter.district,
    }).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = `GP-Blueprint-${this.filter.gramPanchayat}.pdf`; a.click();
        setTimeout(() => URL.revokeObjectURL(url), 30_000);
      },
      error: (e) => this.snack.open(e?.error?.message || 'Could not build GP blueprint', 'OK', { duration: 3000 }),
    });
  }

  private dispatch(studentIds: string[], recipientEmails?: string[]): void {
    if (!studentIds.length) {
      this.snack.open('Select at least one student row first', 'OK', { duration: 2500 });
      return;
    }
    // Show a preview (subject, body, recipients, attachment + student count) before sending.
    const ref = this.dialog.open(SendMailDialogComponent, {
      width: '600px', maxWidth: '92vw',
      data: {
        recipients: recipientEmails ?? [],
        studentCount: studentIds.length,
        gramPanchayat: this.filter.gramPanchayat, taluk: this.filter.taluk, district: this.filter.district,
      },
    });
    ref.afterClosed().subscribe((result?: { subject: string; body: string }) => {
      if (!result) return; // cancelled
      this.doDispatch(studentIds, recipientEmails, result.subject, result.body);
    });
  }

  private doDispatch(studentIds: string[], recipientEmails: string[] | undefined, subject: string, body: string): void {
    // Attach the GP Blue Print packet to the GP mail when a Gram Panchayat is filtered.
    this.data.sendMail({
      studentIds, recipientEmails, subject, body,
      gramPanchayat: this.filter.gramPanchayat, taluk: this.filter.taluk, district: this.filter.district,
    }).subscribe({
      next: (res) => {
        const sent = Object.values(res).filter((v) => v.startsWith('SENT')).length;
        const to = recipientEmails?.length ? ` to ${recipientEmails.length} mail ID(s)` : '';
        this.snack.open(`Mail dispatched${to}: ${sent}/${studentIds.length} sent`, 'OK', { duration: 3500 });
        this.loadHistory();
      },
      error: (e) => this.snack.open(e?.error?.message || 'Send failed', 'OK', { duration: 3500 }),
    });
  }
}
