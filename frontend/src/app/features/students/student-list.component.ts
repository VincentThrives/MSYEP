import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { forkJoin, of, Observable } from 'rxjs';

import { DataService } from '../../core/data.service';
import { LocationPickerComponent } from '../../shared/location-picker.component';
import { SearchSelectComponent } from '../../shared/search-select.component';
import {
  CASTES, Center, COURSE_CATALOG, GENDERS, HOBBIES, STUDENT_DOC_SLOTS, Student,
  StudentRegistrationResult, YES_NO,
} from '../../core/models';

@Component({
  selector: 'app-student-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatTabsModule, MatCheckboxModule,
    MatSnackBarModule, LocationPickerComponent, SearchSelectComponent,
  ],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.scss',
})
export class StudentListComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);

  cols = ['name', 'register', 'batch', 'gender', 'district', 'actions'];
  genders = GENDERS;
  castes = CASTES;
  yesNo = YES_NO;
  hobbies = HOBBIES;
  catalog = COURSE_CATALOG;
  docSlots = STUDENT_DOC_SLOTS;
  year = new Date().getFullYear();

  students = signal<Student[]>([]);
  centers = signal<Center[]>([]);
  editing = signal(false);
  saving = signal(false);
  result = signal<StudentRegistrationResult | null>(null);
  form: Student = this.blank();
  private files: Record<string, File> = {};

  constructor() {
    this.load();
    this.data.centers().subscribe((c) => this.centers.set(c));
  }

  load(): void {
    this.data.students().subscribe((s) => this.students.set(s));
  }

  blank(): Student {
    return { name: '', state: 'Karnataka', hobbies: [], interestedCourses: [] };
  }

  newStudent(): void {
    this.form = this.blank();
    this.files = {};
    this.result.set(null);
    this.editing.set(true);
  }

  edit(s: Student): void {
    this.form = { ...s, hobbies: [...(s.hobbies || [])], interestedCourses: [...(s.interestedCourses || [])] };
    this.files = {};
    this.editing.set(true);
  }

  toggle(list: 'hobbies' | 'interestedCourses', value: string, on: boolean): void {
    const set = new Set(this.form[list] || []);
    on ? set.add(value) : set.delete(value);
    this.form[list] = [...set];
  }

  isChecked(list: 'hobbies' | 'interestedCourses', value: string): boolean {
    return (this.form[list] || []).includes(value);
  }

  onFile(type: string, ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (file.size > 500 * 1024) {
      this.snack.open('File exceeds 500 KB limit', 'OK', { duration: 3000 });
      input.value = '';
      return;
    }
    this.files[type] = file;
  }

  selectedName(type: string): string | null {
    return this.files[type]?.name ?? null;
  }

  existingDocName(type: string): string | null {
    return this.form.documents?.find((d) => d.type === type)?.filename ?? null;
  }

  save(): void {
    if (!this.form.name) {
      this.snack.open('Student name is required', 'OK', { duration: 2500 });
      return;
    }
    if (!this.form.id && (!this.form.userId || !this.form.password)) {
      this.snack.open('User ID and Password are required to create the student login', 'OK', { duration: 3500 });
      return;
    }
    this.saving.set(true);
    if (this.form.id) {
      this.data.updateStudent(this.form.id, this.form).subscribe({
        next: (s) => this.afterSave(s.id!, null),
        error: (e) => this.fail(e),
      });
    } else {
      this.data.createStudent(this.form).subscribe({
        next: (res) => this.afterSave(res.student.id!, res),
        error: (e) => this.fail(e),
      });
    }
  }

  private afterSave(id: string, res: StudentRegistrationResult | null): void {
    const slots = Object.entries(this.files);
    const uploads: Observable<unknown> = slots.length
      ? forkJoin(slots.map(([type, file]) =>
          this.data.uploadStudentDocument(id, type, this.labelFor(type), file)))
      : of(null);
    uploads.subscribe({
      next: () => {
        this.saving.set(false);
        this.editing.set(false);
        if (res) this.result.set(res);
        this.snack.open(res ? res.note : 'Student saved', 'OK', { duration: 4000 });
        this.load();
      },
      error: (e: any) => {
        this.saving.set(false);
        this.snack.open('Saved, but a document upload failed: ' + (e?.error?.message || ''), 'OK', { duration: 4000 });
        this.editing.set(false);
        this.load();
      },
    });
  }

  private fail(e: any): void {
    this.saving.set(false);
    this.snack.open(e?.error?.message || 'Save failed', 'OK', { duration: 3000 });
  }

  private labelFor(type: string): string {
    return this.docSlots.find((s) => s.type === type)?.label ?? type;
  }

  centerName(id?: string): string {
    return this.centers().find((c) => c.id === id)?.name || '';
  }

  remove(s: Student): void {
    if (!s.id || !confirm(`Delete student "${s.name}"?`)) return;
    this.data.deleteStudent(s.id).subscribe(() => {
      this.snack.open('Student deleted', 'OK', { duration: 2000 });
      this.load();
    });
  }
}
