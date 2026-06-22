import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { forkJoin, of, Observable } from 'rxjs';

import { DataService } from '../../core/data.service';
import { SearchSelectComponent } from '../../shared/search-select.component';
import { LocationPickerComponent } from '../../shared/location-picker.component';
import { TermsComponent } from '../../shared/terms.component';
import {
  GENDERS, INVESTMENT_CAPACITY, MEMBERSHIP_TIERS, OWN_RENT, START_TIMELINE,
  Zone, ZoneRegistrationResult, ZONE_DOC_SLOTS,
} from '../../core/models';

@Component({
  selector: 'app-zone-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatCheckboxModule, MatTabsModule, MatSnackBarModule,
    SearchSelectComponent, LocationPickerComponent, TermsComponent,
  ],
  templateUrl: './zone-list.component.html',
  styleUrl: './zone-list.component.scss',
})
export class ZoneListComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);

  cols = ['code', 'org', 'owner', 'tier', 'status', 'actions'];
  genders = GENDERS;
  ownRent = OWN_RENT;
  investments = INVESTMENT_CAPACITY;
  timelines = START_TIMELINE;
  tiers = MEMBERSHIP_TIERS;
  docSlots = ZONE_DOC_SLOTS;

  zones = signal<Zone[]>([]);
  editing = signal(false);
  saving = signal(false);
  showTerms = signal(false);
  result = signal<ZoneRegistrationResult | null>(null);
  form: Zone = this.blank();
  private files: Record<string, File> = {};

  constructor() {
    this.load();
  }

  load(): void {
    this.data.zones().subscribe((z) => this.zones.set(z));
  }

  blank(): Zone {
    return { name: '', hasWebsite: false, tcAccepted: false };
  }

  newZone(): void {
    this.form = this.blank();
    this.files = {};
    this.result.set(null);
    this.editing.set(true);
  }

  edit(z: Zone): void {
    this.form = { ...z };
    this.files = {};
    this.editing.set(true);
  }

  onFile(type: string, ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      this.snack.open('File exceeds 2 MB limit', 'OK', { duration: 3000 });
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
    if (!this.form.name && !this.form.organizationName) {
      this.snack.open('Organization / Zone name is required', 'OK', { duration: 2500 });
      return;
    }
    if (!this.form.id && (!this.form.userId || !this.form.password)) {
      this.snack.open('User ID and Password are required for the franchise login', 'OK', { duration: 3500 });
      return;
    }
    if (!this.form.id && !this.form.tcAccepted) {
      this.snack.open('Please accept the Terms & Conditions', 'OK', { duration: 3000 });
      return;
    }
    this.saving.set(true);
    if (this.form.id) {
      this.data.updateZone(this.form.id, this.form).subscribe({
        next: (z) => this.afterSave(z.id!, null),
        error: (e) => this.fail(e),
      });
    } else {
      this.data.createZone(this.form).subscribe({
        next: (res) => this.afterSave(res.zone.id!, res),
        error: (e) => this.fail(e),
      });
    }
  }

  private afterSave(id: string, res: ZoneRegistrationResult | null): void {
    const slots = Object.entries(this.files);
    const uploads: Observable<unknown> = slots.length
      ? forkJoin(slots.map(([type, file]) =>
          this.data.uploadZoneDocument(id, type, this.labelFor(type), file)))
      : of(null);
    uploads.subscribe({
      next: () => {
        this.saving.set(false);
        this.editing.set(false);
        if (res) this.result.set(res);
        this.snack.open(res ? res.note : 'Zone saved', 'OK', { duration: 4000 });
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
    this.snack.open(e?.error?.message || 'Sign-up failed', 'OK', { duration: 3000 });
  }

  private labelFor(type: string): string {
    return this.docSlots.find((s) => s.type === type)?.label ?? type;
  }

  remove(z: Zone): void {
    if (!z.id || !confirm(`Delete franchise "${z.organizationName || z.name}"?`)) return;
    this.data.deleteZone(z.id).subscribe(() => {
      this.snack.open('Deleted', 'OK', { duration: 2000 });
      this.load();
    });
  }
}
