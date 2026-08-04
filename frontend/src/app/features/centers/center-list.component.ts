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
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { forkJoin, of, Observable } from 'rxjs';

import { DataService } from '../../core/data.service';
import { LocationService } from '../../core/location.service';
import { SearchSelectComponent } from '../../shared/search-select.component';
import {
  academicYears, Center, CenterRegistrationResult, CENTER_DOC_GROUPS, CENTER_DOC_SLOTS,
  CENTER_TYPES, MONTHS, Zone,
} from '../../core/models';

@Component({
  selector: 'app-center-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatSnackBarModule, MatTooltipModule,
    MatTabsModule, MatChipsModule, MatCheckboxModule, MatAutocompleteModule, SearchSelectComponent,
  ],
  templateUrl: './center-list.component.html',
  styleUrl: './center-list.component.scss',
})
export class CenterListComponent {
  private data = inject(DataService);
  private location = inject(LocationService);
  private snack = inject(MatSnackBar);
  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);

  districts = signal<string[]>([]);
  taluks = signal<string[]>([]);
  gps = signal<string[]>([]);

  cols = ['name', 'code', 'batch', 'type', 'district', 'actions'];
  centerTypes = CENTER_TYPES;
  months = MONTHS;
  academicYearList = academicYears();
  docSlots = CENTER_DOC_SLOTS;
  docGroups = CENTER_DOC_GROUPS;
  year = new Date().getFullYear();

  slotsIn(group: string) {
    return this.docSlots.filter((s) => s.group === group);
  }

  centers = signal<Center[]>([]);
  zones = signal<Zone[]>([]);
  editing = signal(false);
  saving = signal(false);
  result = signal<CenterRegistrationResult | null>(null);
  form: Center = this.blank();
  private files: Record<string, File> = {};

  // ---- Wizard ----
  readonly sections = [
    { key: 'academic', label: 'Academic & Type' },
    { key: 'location', label: 'Center Location' },
    { key: 'details', label: 'Center Details' },
    { key: 'courses', label: 'Course Details' },
    { key: 'mou', label: 'MOU Details' },
    { key: 'documents', label: 'Documents' },
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
    return this.centers().filter((c) =>
      (!d || c.district === d) &&
      (!t || c.taluk === t) &&
      (!g || c.gramPanchayat === g) &&
      (!q || [c.name, c.code, c.centerType, c.principalName].some((v) => (v || '').toLowerCase().includes(q))));
  });
  districtOptions = computed(() => this.distinctVals(this.centers().map((c) => c.district)));
  talukOptions = computed(() =>
    this.distinctVals(this.centers().filter((c) => !this.fDistrict() || c.district === this.fDistrict()).map((c) => c.taluk)));
  gpOptions = computed(() =>
    this.distinctVals(this.centers().filter((c) =>
      (!this.fDistrict() || c.district === this.fDistrict()) &&
      (!this.fTaluk() || c.taluk === this.fTaluk())).map((c) => c.gramPanchayat)));

  constructor() {
    this.load();
    this.data.zones().subscribe((z) => {
      this.zones.set(z);
      // If a zone is already chosen (zone/center login, or editing), fix the MOU period to it.
      if (this.form.zoneId) this.applyZoneMou(this.form.zoneId);
    });
    this.location.districts().subscribe((d) => this.districts.set(d));
    // Side-nav "Create Center" opens the form via ?new=1; "View Centers" shows the list.
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((p) => (p.get('new') !== null ? this.newCenter() : this.editing.set(false)));
  }

  /** Picking a zone fixes the center's MOU period to that zone's franchise validity. */
  onZoneSelected(zoneId: string): void {
    this.form.zoneId = zoneId;
    this.applyZoneMou(zoneId);
  }

  /** Set MOU (from) = zone's certificate issue date, (to) = +2 years, contract = 2 Years. */
  private applyZoneMou(zoneId?: string): void {
    const zone = this.zones().find((z) => z.id === zoneId);
    if (!zone?.issueDate) return;
    this.form.dateOfMou = zone.issueDate;
    this.form.mouEndDate = zone.validTill || this.plusYears(zone.issueDate, 2);
    this.form.contractDuration = '2 Years';
  }

  private plusYears(iso: string, years: number): string {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return '';
    d.setFullYear(d.getFullYear() + years);
    return d.toISOString().slice(0, 10);
  }

  onDistrict(): void {
    this.form.taluk = undefined;
    this.form.gramPanchayat = undefined;
    this.taluks.set([]);
    this.gps.set([]);
    if (this.form.district) {
      this.location.taluks(this.form.district).subscribe((t) => this.taluks.set(t));
    }
  }

  onTaluk(): void {
    this.form.gramPanchayat = undefined;
    this.gps.set([]);
    if (this.form.district && this.form.taluk) {
      this.location.gramPanchayats(this.form.district, this.form.taluk).subscribe((g) => this.gps.set(g));
    }
  }

  addCourse(ev: MatChipInputEvent): void {
    const v = (ev.value || '').trim();
    if (v) (this.form.courses ??= []).push(v);
    ev.chipInput!.clear();
  }

  removeCourse(c: string): void {
    this.form.courses = (this.form.courses || []).filter((x) => x !== c);
  }

  onImportLocations(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.location.importExcel(file).subscribe({
      next: (r) => {
        this.snack.open(`Imported ${r.added} location rows`, 'OK', { duration: 3000 });
        if (this.form.taluk) this.onTaluk();
      },
      error: (e) => this.snack.open(e?.error?.message || 'Import failed', 'OK', { duration: 3000 }),
    });
    input.value = '';
  }

  load(): void {
    this.data.centers().subscribe((c) => this.centers.set(c));
  }

  /** Existing colleges to suggest (filtered by the chosen type + what's typed); free text still allowed. */
  collegeSuggestions(): string[] {
    const q = (this.form.name || '').toLowerCase().trim();
    const type = this.form.centerType;
    const seen = new Set<string>();
    return this.centers()
      .filter((c) => !!c.name && (!type || c.centerType === type))
      .map((c) => c.name!)
      .filter((n) => (seen.has(n) ? false : (seen.add(n), true)))
      .filter((n) => !q || n.toLowerCase().includes(q))
      .slice(0, 20);
  }

  blank(): Center {
    return { name: '', courses: [], hasWebsite: false };
  }

  newCenter(): void {
    this.form = this.blank();
    this.files = {};
    this.taluks.set([]);
    this.gps.set([]);
    this.result.set(null);
    this.tabIndex.set(0);
    this.editing.set(true);
  }

  // ---- Wizard helpers ----
  private has(v: unknown): boolean {
    return v !== undefined && v !== null && String(v).trim() !== '';
  }
  private anyDocChosen(): boolean {
    return Object.keys(this.files).length > 0 || (this.form.documents?.length ?? 0) > 0;
  }
  private sectionFields(key: string): boolean[] {
    const f = this.form;
    switch (key) {
      case 'academic':
        return f.id
          ? [this.has(f.academicYear), this.has(f.centerType), this.has(f.name)]
          : [this.has(f.academicYear), this.has(f.centerType), this.has(f.name), this.has(f.userId), this.has(f.password)];
      case 'location':
        return [this.has(f.district), this.has(f.taluk), this.has(f.gramPanchayat), this.has(f.address), this.has(f.pincode), this.has(f.zoneId)];
      case 'details':
        return [this.has(f.email), this.has(f.officeNumber), this.has(f.principalName), this.has(f.principalNumber)];
      case 'courses':
        return [(f.courses?.length ?? 0) > 0, this.has(f.totalStrength), this.has(f.strengthTotal)];
      case 'mou':
        return [this.has(f.dateOfMou), this.has(f.mouEndDate), this.has(f.contractDuration)];
      case 'documents':
        return [this.anyDocChosen()];
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
  private distinctVals(vals: (string | undefined)[]): string[] {
    return [...new Set(vals.filter((v): v is string => !!v && v.trim() !== ''))].sort();
  }
  clearFilters(): void {
    this.fDistrict.set(''); this.fTaluk.set(''); this.fGp.set(''); this.fSearch.set('');
  }

  edit(c: Center): void {
    this.form = { ...c, courses: [...(c.courses || [])] };
    this.files = {};
    this.taluks.set([]);
    this.gps.set([]);
    this.tabIndex.set(0);
    this.editing.set(true);
    if (c.district) this.location.taluks(c.district).subscribe((t) => this.taluks.set(t));
    if (c.district && c.taluk) {
      this.location.gramPanchayats(c.district, c.taluk).subscribe((g) => this.gps.set(g));
    }
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

  /** True if a document is provided — uploaded now or already saved on the center. */
  hasDoc(type: string): boolean {
    return !!this.files[type] || !!this.existingDocName(type);
  }

  save(): void {
    if (!this.form.name) {
      this.snack.open('College / Center name is required', 'OK', { duration: 2500 });
      return;
    }
    if (!this.form.id && (!this.form.userId || !this.form.password)) {
      this.snack.open('User ID and Password are required to create the center login', 'OK', { duration: 3500 });
      return;
    }
    if (!this.hasDoc('centerLogo')) {
      this.snack.open('Please upload the Center Logo (Center Details tab)', 'OK', { duration: 3500 });
      this.tabIndex.set(this.sections.findIndex((s) => s.key === 'details'));
      return;
    }
    if (!this.hasDoc('centerBuilding')) {
      this.snack.open('Please upload the Center Building photo (Center Details tab)', 'OK', { duration: 3500 });
      this.tabIndex.set(this.sections.findIndex((s) => s.key === 'details'));
      return;
    }
    this.saving.set(true);
    if (this.form.id) {
      this.data.updateCenter(this.form.id, this.form).subscribe({
        next: (c) => this.afterSave(c.id!, null),
        error: (e) => this.fail(e),
      });
    } else {
      this.data.createCenter(this.form).subscribe({
        next: (res) => this.afterSave(res.center.id!, res),
        error: (e) => this.fail(e),
      });
    }
  }

  private afterSave(id: string, res: CenterRegistrationResult | null): void {
    const slots = Object.entries(this.files);
    const uploads: Observable<unknown> = slots.length
      ? forkJoin(slots.map(([type, file]) =>
          this.data.uploadCenterDocument(id, type, this.labelFor(type), file)))
      : of(null);
    uploads.subscribe({
      next: () => {
        this.saving.set(false);
        this.editing.set(false);
        if (res) this.result.set(res);
        this.snack.open(res ? res.emailNote : 'Center saved', 'OK', { duration: 4000 });
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

  remove(c: Center): void {
    if (!c.id || !confirm(`Delete center "${c.name}"?`)) return;
    this.data.deleteCenter(c.id).subscribe(() => {
      this.snack.open('Center deleted', 'OK', { duration: 2000 });
      this.load();
    });
  }

  /** Download the per-center "Batch Approval" PDF, filled with this center's own data. */
  downloadBatchApproval(c: Center): void {
    if (!c.id) return;
    this.data.centerBatchApprovalPdf(c.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = `Batch-Approval-${c.name}.pdf`; a.click();
        URL.revokeObjectURL(url);
      },
      error: (e) => this.snack.open(e?.error?.message || 'Could not build the batch approval PDF', 'OK', { duration: 3500 }),
    });
  }
}
