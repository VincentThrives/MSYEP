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
  template: `
    <h1>Finance Wing — Student FW Documents</h1>

    <div class="filters">
      <app-location-picker
        [district]="filter.district" (districtChange)="filter.district = $event; fetch()"
        [taluk]="filter.taluk" (talukChange)="filter.taluk = $event; fetch()"
        [gramPanchayat]="filter.gramPanchayat"
        (gramPanchayatChange)="filter.gramPanchayat = $event; onGpChange()"></app-location-picker>
      <app-search-select label="Center" [options]="centers()" valueKey="id" labelKey="name"
        [emptyOption]="true" emptyLabel="All" [value]="filter.centerId"
        (valueChange)="filter.centerId = $event; fetch()"></app-search-select>
      <button mat-stroked-button (click)="clear()"><mat-icon>clear</mat-icon> Clear</button>
    </div>

    <div class="gp-email" *ngIf="filter.gramPanchayat">
      <mat-icon>mail</mat-icon>
      <span>GP email auto-loaded: <strong>{{ gpEmail() || '— not mapped —' }}</strong></span>
    </div>

    <div class="bulk" *ngIf="selectedIds().length">
      {{ selectedIds().length }} selected
      <button mat-flat-button color="primary" (click)="sendBulk()">
        <mat-icon>send</mat-icon> Send mail to GP for selected
      </button>
    </div>

    <table mat-table [dataSource]="rows()" class="mat-elevation-z1 full">
      <ng-container matColumnDef="select">
        <th mat-header-cell *matHeaderCellDef>
          <mat-checkbox [checked]="allSelected()" [indeterminate]="someSelected()"
            (change)="toggleAll($event.checked)"></mat-checkbox>
        </th>
        <td mat-cell *matCellDef="let r">
          <mat-checkbox [checked]="selected.has(r.studentId)"
            (change)="toggleOne(r.studentId, $event.checked)"></mat-checkbox>
        </td>
      </ng-container>
      <ng-container matColumnDef="serialNo">
        <th mat-header-cell *matHeaderCellDef>#</th>
        <td mat-cell *matCellDef="let r">{{ r.serialNo }}</td>
      </ng-container>
      <ng-container matColumnDef="studentName">
        <th mat-header-cell *matHeaderCellDef>Student Name</th>
        <td mat-cell *matCellDef="let r">{{ r.studentName }}</td>
      </ng-container>
      <ng-container matColumnDef="district">
        <th mat-header-cell *matHeaderCellDef>District</th>
        <td mat-cell *matCellDef="let r">{{ r.district }}</td>
      </ng-container>
      <ng-container matColumnDef="taluk">
        <th mat-header-cell *matHeaderCellDef>Taluk</th>
        <td mat-cell *matCellDef="let r">{{ r.taluk }}</td>
      </ng-container>
      <ng-container matColumnDef="gramPanchayat">
        <th mat-header-cell *matHeaderCellDef>Gram Panchayat</th>
        <td mat-cell *matCellDef="let r">{{ r.gramPanchayat }}</td>
      </ng-container>
      <ng-container matColumnDef="centerName">
        <th mat-header-cell *matHeaderCellDef>Center</th>
        <td mat-cell *matCellDef="let r">{{ r.centerName }}</td>
      </ng-container>
      <ng-container matColumnDef="docs">
        <th mat-header-cell *matHeaderCellDef>View / Download</th>
        <td mat-cell *matCellDef="let r">
          <button mat-icon-button [disabled]="!r.documentCount"
            matTooltip="{{ r.documentCount }} document(s)">
            <mat-icon>{{ r.documentCount ? 'download' : 'block' }}</mat-icon>
          </button>
        </td>
      </ng-container>
      <ng-container matColumnDef="send">
        <th mat-header-cell *matHeaderCellDef>Send Mail</th>
        <td mat-cell *matCellDef="let r">
          <button mat-icon-button color="primary"
            matTooltip="Send to GP: {{ r.gramPanchayatEmail || 'not mapped' }}"
            [disabled]="!r.gramPanchayatEmail" (click)="sendOne(r)">
            <mat-icon>send</mat-icon>
          </button>
        </td>
      </ng-container>
      <tr mat-header-row *matHeaderRowDef="cols"></tr>
      <tr mat-row *matRowDef="let row; columns: cols"></tr>
    </table>
    <p *ngIf="!rows().length" class="empty">
      No students for the selected filters. Choose Taluk / District / Gram Panchayat / Center above.
    </p>
  `,
  styles: [`
    h1 { margin: 0 0 16px; }
    .filters { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
    .filters mat-form-field { min-width: 170px; }
    .gp-email { display: flex; align-items: center; gap: 8px; background: #fff3e0;
      border: 1px solid #ffe0b2; border-radius: 6px; padding: 8px 12px; margin: 8px 0; }
    .bulk { display: flex; align-items: center; gap: 14px; margin: 10px 0; }
    .full { width: 100%; }
    .empty { color: #999; margin-top: 16px; }
  `],
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
