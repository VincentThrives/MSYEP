import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { DataService } from '../../core/data.service';
import { Center, COURSES, Student } from '../../core/models';
import { LocationPickerComponent } from '../../shared/location-picker.component';
import { SearchSelectComponent } from '../../shared/search-select.component';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatSnackBarModule, LocationPickerComponent,
    SearchSelectComponent,
  ],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.scss',
})
export class StudentListComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);

  cols = ['name', 'course', 'district', 'gp', 'phone', 'actions'];
  courses = COURSES;
  students = signal<Student[]>([]);
  centers = signal<Center[]>([]);
  editing = signal(false);
  form: Student = this.blank();

  constructor() {
    this.load();
    this.data.centers().subscribe((c) => this.centers.set(c));
  }

  load(): void {
    this.data.students().subscribe((s) => this.students.set(s));
  }

  blank(): Student {
    return { name: '' };
  }

  newStudent(): void {
    this.form = this.blank();
    this.editing.set(true);
  }

  edit(s: Student): void {
    this.form = { ...s };
    this.editing.set(true);
  }

  save(): void {
    if (!this.form.name) {
      this.snack.open('Name is required', 'OK', { duration: 2500 });
      return;
    }
    const obs = this.form.id
      ? this.data.updateStudent(this.form.id, this.form)
      : this.data.createStudent(this.form);
    obs.subscribe({
      next: () => {
        this.snack.open('Student saved', 'OK', { duration: 2000 });
        this.editing.set(false);
        this.load();
      },
      error: (e) => this.snack.open(e?.error?.message || 'Save failed', 'OK', { duration: 3000 }),
    });
  }

  remove(s: Student): void {
    if (!s.id || !confirm(`Delete student "${s.name}"?`)) return;
    this.data.deleteStudent(s.id).subscribe(() => {
      this.snack.open('Student deleted', 'OK', { duration: 2000 });
      this.load();
    });
  }
}
