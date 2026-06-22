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

import { DataService } from '../../core/data.service';
import { Center, FinanceRow } from '../../core/models';
import { SearchSelectComponent } from '../../shared/search-select.component';
import { LocationPickerComponent } from '../../shared/location-picker.component';

@Component({
  selector: 'app-finance-table',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule, SearchSelectComponent,
    LocationPickerComponent,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule,
    MatTooltipModule, MatSnackBarModule,
  ],
  templateUrl: './finance-table.component.html',
  styleUrl: './finance-table.component.scss',
})
export class FinanceTableComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);

  cols = ['select', 'serialNo', 'studentName', 'district', 'taluk', 'gramPanchayat',
    'centerName', 'docs', 'send'];

  rows = signal<FinanceRow[]>([]);
  centers = signal<Center[]>([]);
  filters = signal<{ districts: string[]; taluks: string[]; gramPanchayats: string[] }>(
    { districts: [], taluks: [], gramPanchayats: [] });
  gpEmail = signal<string>('');

  filter: { district?: string; taluk?: string; gramPanchayat?: string; centerId?: string } = {};
  selected = new Set<string>();
  selectedIds = signal<string[]>([]);

  allSelected = computed(() => this.rows().length > 0 && this.selectedIds().length === this.rows().length);
  someSelected = computed(() => this.selectedIds().length > 0 && !this.allSelected());

  constructor() {
    this.data.financeFilters().subscribe((f) => this.filters.set(f));
    this.data.centers().subscribe((c) => this.centers.set(c));
    this.fetch();
  }

  fetch(): void {
    this.data.financeStudents(this.filter).subscribe((r) => {
      this.rows.set(r);
      this.selected.clear();
      this.syncSelected();
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

  sendBulk(): void {
    this.dispatch(this.selectedIds());
  }

  private dispatch(studentIds: string[]): void {
    if (!studentIds.length) return;
    this.data.sendMail({ studentIds }).subscribe({
      next: (res) => {
        const sent = Object.values(res).filter((v) => v.startsWith('SENT')).length;
        this.snack.open(`Mail dispatched: ${sent}/${studentIds.length} sent`, 'OK', { duration: 3500 });
      },
      error: (e) => this.snack.open(e?.error?.message || 'Send failed', 'OK', { duration: 3500 }),
    });
  }
}
