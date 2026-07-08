import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { DataService } from '../../core/data.service';
import { Center, Staff, Zone } from '../../core/models';
import { LocationPickerComponent } from '../../shared/location-picker.component';
import { SearchSelectComponent } from '../../shared/search-select.component';

@Component({
  selector: 'app-staff-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatTabsModule, MatSnackBarModule,
    LocationPickerComponent, SearchSelectComponent,
  ],
  templateUrl: './staff-list.component.html',
  styleUrl: './staff-list.component.scss',
})
export class StaffListComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);
  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);

  cols = ['name', 'designation', 'phone', 'zone', 'district', 'actions'];
  staff = signal<Staff[]>([]);
  zones = signal<Zone[]>([]);
  centers = signal<Center[]>([]);
  editing = signal(false);
  form: Staff = this.blank();

  // ---- Wizard ----
  readonly sections = [
    { key: 'details', label: 'Staff Details' },
    { key: 'assignment', label: 'Assignment & Location' },
  ];
  tabIndex = signal(0);

  // ---- View filters ----
  fDistrict = signal('');
  fTaluk = signal('');
  fGp = signal('');
  fSearch = signal('');

  filtered = computed(() => {
    const d = this.fDistrict(), t = this.fTaluk(), g = this.fGp();
    const q = this.fSearch().trim().toLowerCase();
    return this.staff().filter((s) =>
      (!d || s.district === d) &&
      (!t || s.taluk === t) &&
      (!g || s.gramPanchayat === g) &&
      (!q || [s.name, s.designation, s.phone, s.email].some((v) => (v || '').toLowerCase().includes(q))));
  });

  districtOptions = computed(() => this.distinct(this.staff().map((s) => s.district)));
  talukOptions = computed(() =>
    this.distinct(this.staff().filter((s) => !this.fDistrict() || s.district === this.fDistrict()).map((s) => s.taluk)));
  gpOptions = computed(() =>
    this.distinct(this.staff().filter((s) =>
      (!this.fDistrict() || s.district === this.fDistrict()) &&
      (!this.fTaluk() || s.taluk === this.fTaluk())).map((s) => s.gramPanchayat)));

  constructor() {
    this.load();
    this.data.zones().subscribe((z) => this.zones.set(z));
    this.data.centers().subscribe((c) => this.centers.set(c));
    // Side-nav "Add Staff" opens the form via ?new=1; "View Staff" shows the list.
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((p) => (p.get('new') !== null ? this.newStaff() : this.editing.set(false)));
  }

  load(): void {
    this.data.staff().subscribe((s) => this.staff.set(s));
  }

  blank(): Staff {
    return { name: '' };
  }

  zoneName(id?: string): string {
    return this.zones().find((z) => z.id === id)?.name || '';
  }

  newStaff(): void {
    this.form = this.blank();
    this.tabIndex.set(0);
    this.editing.set(true);
  }

  edit(s: Staff): void {
    this.form = { ...s };
    this.tabIndex.set(0);
    this.editing.set(true);
  }

  // ---- Wizard helpers ----
  private has(v: unknown): boolean {
    return v !== undefined && v !== null && String(v).trim() !== '';
  }
  private sectionFields(key: string): boolean[] {
    const f = this.form;
    switch (key) {
      case 'details':
        return [this.has(f.name), this.has(f.designation), this.has(f.phone), this.has(f.email)];
      case 'assignment':
        return [this.has(f.zoneId), this.has(f.centerId), this.has(f.district), this.has(f.taluk), this.has(f.gramPanchayat)];
      default:
        return [];
    }
  }
  progress(key: string): number {
    const a = this.sectionFields(key);
    return a.length ? Math.round((a.filter(Boolean).length / a.length) * 100) : 0;
  }
  isComplete(key: string): boolean { return this.progress(key) === 100; }
  overall(): number {
    const a = this.sections.flatMap((s) => this.sectionFields(s.key));
    return a.length ? Math.round((a.filter(Boolean).length / a.length) * 100) : 0;
  }
  goTo(i: number): void { if (i >= 0 && i < this.sections.length) this.tabIndex.set(i); }
  get isLastTab(): boolean { return this.tabIndex() === this.sections.length - 1; }

  // ---- Filter helpers ----
  private distinct(vals: (string | undefined)[]): string[] {
    return [...new Set(vals.filter((v): v is string => !!v && v.trim() !== ''))].sort();
  }
  clearFilters(): void {
    this.fDistrict.set(''); this.fTaluk.set(''); this.fGp.set(''); this.fSearch.set('');
  }

  save(): void {
    if (!this.form.name) {
      this.snack.open('Name is required', 'OK', { duration: 2500 });
      return;
    }
    const obs = this.form.id
      ? this.data.updateStaff(this.form.id, this.form)
      : this.data.createStaff(this.form);
    obs.subscribe({
      next: () => {
        this.snack.open('Staff saved', 'OK', { duration: 2000 });
        this.editing.set(false);
        this.load();
      },
      error: (e) => this.snack.open(e?.error?.message || 'Save failed', 'OK', { duration: 3000 }),
    });
  }

  remove(s: Staff): void {
    if (!s.id || !confirm(`Delete staff "${s.name}"?`)) return;
    this.data.deleteStaff(s.id).subscribe(() => {
      this.snack.open('Staff deleted', 'OK', { duration: 2000 });
      this.load();
    });
  }
}
