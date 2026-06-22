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
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
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
    MatTabsModule, MatChipsModule, MatCheckboxModule, SearchSelectComponent,
  ],
  templateUrl: './center-list.component.html',
  styleUrl: './center-list.component.scss',
})
export class CenterListComponent {
  private data = inject(DataService);
  private location = inject(LocationService);
  private snack = inject(MatSnackBar);

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

  constructor() {
    this.load();
    this.data.zones().subscribe((z) => this.zones.set(z));
    this.location.districts().subscribe((d) => this.districts.set(d));
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

  blank(): Center {
    return { name: '', courses: [], hasWebsite: false };
  }

  newCenter(): void {
    this.form = this.blank();
    this.files = {};
    this.taluks.set([]);
    this.gps.set([]);
    this.result.set(null);
    this.editing.set(true);
  }

  edit(c: Center): void {
    this.form = { ...c, courses: [...(c.courses || [])] };
    this.files = {};
    this.taluks.set([]);
    this.gps.set([]);
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

  save(): void {
    if (!this.form.name) {
      this.snack.open('College / Center name is required', 'OK', { duration: 2500 });
      return;
    }
    if (!this.form.id && (!this.form.userId || !this.form.password)) {
      this.snack.open('User ID and Password are required to create the center login', 'OK', { duration: 3500 });
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

  downloadCenterPdf(c: Center): void {
    if (!c.id) return;
    this.data.centerRegistrationPdf(c.id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = `Center-${c.name}.pdf`; a.click();
      URL.revokeObjectURL(url);
    });
  }
}
