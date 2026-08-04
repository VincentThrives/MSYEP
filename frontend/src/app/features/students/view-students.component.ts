import { AfterViewInit, Component, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { DataService } from '../../core/data.service';
import { AuthService } from '../../core/auth.service';
import { LocationPickerComponent } from '../../shared/location-picker.component';
import { SearchSelectComponent } from '../../shared/search-select.component';
import { CASTES, Center, Student } from '../../core/models';

@Component({
  selector: 'app-view-students',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatPaginatorModule, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatInputModule, MatSnackBarModule, MatTooltipModule,
    LocationPickerComponent, SearchSelectComponent,
  ],
  templateUrl: './view-students.component.html',
  styleUrl: './view-students.component.scss',
})
export class ViewStudentsComponent implements AfterViewInit {
  private data = inject(DataService);
  private auth = inject(AuthService);
  private snack = inject(MatSnackBar);
  private router = inject(Router);

  /** Admins can view/download any student's resume without the ₹90 payment gate. */
  isAdmin = (): boolean => {
    const r = this.auth.role();
    return r === 'ADMIN' || r === 'SUPER_ADMIN';
  };

  cols = ['reg', 'name', 'details', 'center', 'status', 'view'];
  castes = CASTES;
  centers = signal<Center[]>([]);
  ds = new MatTableDataSource<Student>([]);
  filter: { district?: string; taluk?: string; gramPanchayat?: string; centerId?: string; caste?: string } = {};

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor() {
    this.data.centers().subscribe((c) => this.centers.set(c));
    this.fetch();
  }

  ngAfterViewInit(): void {
    this.ds.paginator = this.paginator;
  }

  fetch(): void {
    this.data.studentsFilter(this.filter as any).subscribe((s) => (this.ds.data = s));
  }

  applySearch(term: string): void {
    this.ds.filter = (term || '').trim().toLowerCase();
  }

  reset(): void {
    this.filter = {};
    this.ds.filter = '';
    this.fetch();
  }

  exportExcel(): void {
    this.data.studentsExportExcel(this.filter as any).subscribe((blob) => this.save(blob, 'students.xlsx'));
  }

  centerName(id?: string): string {
    return this.centers().find((c) => c.id === id)?.name || '';
  }

  editStudent(s: Student): void {
    // Open the Create/Edit form pre-loaded with this student's saved data (Save then updates it).
    this.router.navigate(['/app/students'], { queryParams: { edit: s.id } });
  }

  /** Admin: open the student's resume PDF in a new tab (no payment gate). */
  viewResume(s: Student): void {
    this.data.cvBlobFor(s.id!, true).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (e) => this.snack.open(e?.error?.message || 'Could not open resume', 'OK', { duration: 3000 }),
    });
  }

  /** Admin: download the student's resume PDF (no payment gate). */
  downloadResume(s: Student): void {
    this.data.cvBlobFor(s.id!, false).subscribe({
      next: (blob) => this.save(blob, `${(s.name || 'student').replace(/\s+/g, '_')}-Resume.pdf`),
      error: (e) => this.snack.open(e?.error?.message || 'Download failed', 'OK', { duration: 3000 }),
    });
  }

  private save(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = name; a.click();
    URL.revokeObjectURL(url);
  }
}
