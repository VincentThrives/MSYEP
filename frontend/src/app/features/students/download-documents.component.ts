import { AfterViewInit, Component, ViewChild, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { DataService } from '../../core/data.service';
import { LocationPickerComponent } from '../../shared/location-picker.component';
import { SearchSelectComponent } from '../../shared/search-select.component';
import { Center, Student, STUDENT_DOC_SLOTS } from '../../core/models';

@Component({
  selector: 'app-download-documents',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatPaginatorModule, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatInputModule, MatCheckboxModule, MatSnackBarModule,
    LocationPickerComponent, SearchSelectComponent,
  ],
  templateUrl: './download-documents.component.html',
  styleUrl: './download-documents.component.scss',
})
export class DownloadDocumentsComponent implements AfterViewInit {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);

  cols = ['select', 'reg', 'name', 'center', 'status'];
  centers = signal<Center[]>([]);
  ds = new MatTableDataSource<Student>([]);
  filter: { district?: string; taluk?: string; gramPanchayat?: string; centerId?: string } = {};
  docType = 'All';
  docTypeOptions = [{ value: 'All', label: 'All' }, ...STUDENT_DOC_SLOTS.map((s) => ({ value: s.type, label: s.label }))];

  selected = new Set<string>();
  selectedCount = signal(0);
  busy = signal(false);

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  allSelected = computed(() => this.ds.data.length > 0 && this.selectedCount() === this.ds.data.length);

  constructor() {
    this.data.centers().subscribe((c) => this.centers.set(c));
    this.fetch();
  }

  ngAfterViewInit(): void {
    this.ds.paginator = this.paginator;
  }

  fetch(): void {
    this.data.studentsFilter(this.filter as any).subscribe((s) => {
      this.ds.data = s;
      this.selected.clear();
      this.sync();
    });
  }

  applySearch(term: string): void {
    this.ds.filter = (term || '').trim().toLowerCase();
  }

  reset(): void {
    this.filter = {};
    this.docType = 'All';
    this.ds.filter = '';
    this.fetch();
  }

  toggleAll(on: boolean): void {
    if (on) this.ds.data.forEach((s) => s.id && this.selected.add(s.id));
    else this.selected.clear();
    this.sync();
  }

  toggleOne(id: string, on: boolean): void {
    on ? this.selected.add(id) : this.selected.delete(id);
    this.sync();
  }

  private sync(): void {
    this.selectedCount.set(this.selected.size);
  }

  download(): void {
    if (!this.selected.size) {
      this.snack.open('Select at least one student', 'OK', { duration: 2500 });
      return;
    }
    this.busy.set(true);
    this.data.studentDocumentsPdf([...this.selected], this.docType).subscribe({
      next: (blob) => {
        this.busy.set(false);
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = 'student-documents.pdf'; a.click();
        URL.revokeObjectURL(url);
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(e?.error?.message || 'Download failed', 'OK', { duration: 3000 });
      },
    });
  }

  centerName(id?: string): string {
    return this.centers().find((c) => c.id === id)?.name || '';
  }
}
