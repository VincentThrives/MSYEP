import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
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
import { AuthService } from '../../core/auth.service';
import { LocationPickerComponent } from '../../shared/location-picker.component';
import { SearchSelectComponent } from '../../shared/search-select.component';
import {
  CASTES, Center, COURSE_CATALOG, GENDERS, HOBBIES, STUDENT_DOC_SLOTS, Student,
  StudentRegistrationResult, YES_NO, Zone,
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
  private auth = inject(AuthService);
  private snack = inject(MatSnackBar);
  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);

  cols = ['name', 'register', 'batch', 'gender', 'district', 'actions'];
  genders = GENDERS;
  castes = CASTES;
  yesNo = YES_NO;
  hobbies = HOBBIES;
  catalog = COURSE_CATALOG;
  docSlots = STUDENT_DOC_SLOTS;
  year = new Date().getFullYear();

  centers = signal<Center[]>([]);
  zones = signal<Zone[]>([]);
  editing = signal(false);
  saving = signal(false);
  result = signal<StudentRegistrationResult | null>(null);
  form: Student = this.blank();
  private files: Record<string, File> = {};

  /** Zone/Center are locked to the login's own scope for ZONE / CENTER users. */
  lockZone = false;
  lockCenter = false;

  /** Wizard sections in order — used for progress + auto-advance. */
  readonly sections = [
    { key: 'account', label: 'Account' },
    { key: 'personal', label: 'Personal Details' },
    { key: 'education', label: 'Education & Job' },
    { key: 'address', label: 'Address' },
    { key: 'allotments', label: 'MSYEP Allotments' },
    { key: 'documents', label: 'Documents' },
  ];
  tabIndex = signal(0);
  private advancedFrom = new Set<string>();

  constructor() {
    this.data.centers().subscribe((c) => this.centers.set(c));
    this.data.zones().subscribe((z) => this.zones.set(z));
    // A CENTER login is locked to its own zone+center; a ZONE login to its own zone.
    const role = this.auth.role();
    const u = this.auth.user();
    if (role === 'CENTER') { this.lockZone = true; this.lockCenter = true; }
    else if (role === 'ZONE') { this.lockZone = true; }
    this.scopeZoneId = u?.zoneId;
    this.scopeCenterId = u?.centerId;
    // Create Student opens straight into the form — no listing table here.
    this.newStudent();
    // Jump to the section requested from the side nav (?tab=0..5), and follow changes.
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const raw = params.get('tab');
        if (raw === null) return;
        const i = Number(raw);
        if (Number.isInteger(i) && i >= 0 && i < this.sections.length) this.tabIndex.set(i);
      });
  }

  private scopeZoneId?: string;
  private scopeCenterId?: string;

  blank(): Student {
    return { name: '', state: 'Karnataka', hobbies: [], interestedCourses: [] };
  }

  newStudent(): void {
    this.form = this.blank();
    // Pre-fill the locked scope so ZONE/CENTER users don't (and can't) pick another.
    if (this.lockZone && this.scopeZoneId) this.form.zoneId = this.scopeZoneId;
    if (this.lockCenter && this.scopeCenterId) this.form.centerId = this.scopeCenterId;
    this.files = {};
    this.result.set(null);
    this.advancedFrom.clear();
    this.tabIndex.set(0);
    this.editing.set(true);
  }

  /** Centers available for the currently-selected zone. */
  centersForZone(): Center[] {
    const z = this.form.zoneId;
    return z ? this.centers().filter((c) => c.zoneId === z) : this.centers();
  }

  onZone(zoneId: string): void {
    this.form.zoneId = zoneId;
    // clear the center if it no longer belongs to the chosen zone
    if (this.form.centerId && !this.centers().some((c) => c.id === this.form.centerId && c.zoneId === zoneId)) {
      this.form.centerId = undefined;
    }
    this.onFormActivity();
  }

  onCenter(centerId: string): void {
    this.form.centerId = centerId;
    // keep zoneId consistent with the chosen center
    const c = this.centers().find((x) => x.id === centerId);
    if (c?.zoneId) this.form.zoneId = c.zoneId;
    this.onFormActivity();
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
    if (!this.form.zoneId || !this.form.centerId) {
      this.snack.open('Zone and Center are required (Address tab)', 'OK', { duration: 3500 });
      this.tabIndex.set(3); // jump to Address
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
      },
      error: (e: any) => {
        this.saving.set(false);
        this.snack.open('Saved, but a document upload failed: ' + (e?.error?.message || ''), 'OK', { duration: 4000 });
        this.editing.set(false);
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

  // ---- Section progress / wizard ----

  private has(v: unknown): boolean {
    if (Array.isArray(v)) return v.length > 0;
    return v !== undefined && v !== null && String(v).trim() !== '';
  }

  /** Boolean per tracked field in a section. */
  private sectionFields(key: string): boolean[] {
    const f = this.form;
    switch (key) {
      case 'account':
        return f.id
          ? [this.has(f.name), this.has(f.email), this.has(f.phone)]
          : [this.has(f.name), this.has(f.email), this.has(f.phone), this.has(f.userId), this.has(f.password)];
      case 'personal':
        return [this.has(f.gender), this.has(f.dateOfBirth), this.has(f.caste)];
      case 'education':
        return [
          this.has(f.educationalQualification), this.has(f.admissionYear), this.has(f.interestedInternship),
          this.has(f.technicalSkills), this.has(f.careerGoal), this.has(f.hobbies), this.has(f.interestedCourses),
        ];
      case 'address':
        return [
          this.has(f.zoneId), this.has(f.centerId), this.has(f.state), this.has(f.district), this.has(f.taluk),
          this.has(f.gramPanchayat), this.has(f.pincode), this.has(f.postalAddress),
        ];
      case 'allotments':
        return [this.has(f.courseJoiningDate), this.has(f.collegeName)];
      case 'documents':
        return this.docSlots.map((s) => !!this.selectedName(s.type) || !!this.existingDocName(s.type));
      default:
        return [];
    }
  }

  /** 0–100 completion for a section. */
  progress(key: string): number {
    const arr = this.sectionFields(key);
    if (!arr.length) return 0;
    return Math.round((arr.filter(Boolean).length / arr.length) * 100);
  }

  isComplete(key: string): boolean {
    return this.progress(key) === 100;
  }

  /** Overall completion across all sections. */
  overall(): number {
    const all = this.sections.flatMap((s) => this.sectionFields(s.key));
    return all.length ? Math.round((all.filter(Boolean).length / all.length) * 100) : 0;
  }

  goTo(index: number): void {
    if (index >= 0 && index < this.sections.length) this.tabIndex.set(index);
  }

  /** Called on any input/change in the form — auto-advance once a section hits 100%. */
  onFormActivity(): void {
    const i = this.tabIndex();
    const key = this.sections[i]?.key;
    if (!key) return;
    if (this.progress(key) === 100 && !this.advancedFrom.has(key) && i < this.sections.length - 1) {
      this.advancedFrom.add(key);
      const next = i + 1;
      // brief pause so the user sees the section turn green before moving on
      setTimeout(() => this.tabIndex.set(next), 450);
    }
  }
}
