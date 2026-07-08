import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { LocationService } from '../../core/location.service';
import { SearchSelectComponent } from '../../shared/search-select.component';

@Component({
  selector: 'app-location-management',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTabsModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatSnackBarModule, SearchSelectComponent,
  ],
  templateUrl: './location-management.component.html',
  styleUrl: './location-management.component.scss',
})
export class LocationManagementComponent {
  private loc = inject(LocationService);
  private snack = inject(MatSnackBar);

  readonly state = 'Karnataka';
  districts = signal<string[]>([]);
  saving = signal(false);

  // District tab
  newDistrict = '';

  // Taluk tab
  tDistrict = signal<string | undefined>(undefined);
  tTaluks = signal<string[]>([]);
  newTaluk = '';

  // Village / GP tab
  gDistrict = signal<string | undefined>(undefined);
  gTaluk = signal<string | undefined>(undefined);
  gTaluks = signal<string[]>([]);
  gGps = signal<string[]>([]);
  newGp = '';

  // Inline edit state (which item is being renamed + the working value)
  editingDistrict = signal<string | null>(null);
  editingTaluk = signal<string | null>(null);
  editingGp = signal<string | null>(null);
  editName = '';

  constructor() {
    this.loadDistricts();
  }

  private loadDistricts(): void {
    this.loc.resetDistricts();
    this.loc.districts().subscribe((d) => this.districts.set(d));
  }

  // --- District ---
  submitDistrict(): void {
    const name = this.newDistrict.trim();
    if (!name) { this.snack.open('Enter a district name', 'OK', { duration: 2500 }); return; }
    this.saving.set(true);
    this.loc.addDistrict(name).subscribe({
      next: () => {
        this.saving.set(false);
        this.newDistrict = '';
        this.snack.open(`District "${name}" added`, 'OK', { duration: 2500 });
        this.loadDistricts();
      },
      error: (e) => { this.saving.set(false); this.fail(e); },
    });
  }

  // --- Taluk ---
  onTDistrict(d: string): void {
    this.tDistrict.set(d);
    this.tTaluks.set([]);
    if (d) this.loc.taluks(d).subscribe((t) => this.tTaluks.set(t));
  }
  submitTaluk(): void {
    const d = this.tDistrict(), name = this.newTaluk.trim();
    if (!d) { this.snack.open('Select a district', 'OK', { duration: 2500 }); return; }
    if (!name) { this.snack.open('Enter a taluk name', 'OK', { duration: 2500 }); return; }
    this.saving.set(true);
    this.loc.addTaluk(d, name).subscribe({
      next: () => {
        this.saving.set(false);
        this.newTaluk = '';
        this.snack.open(`Taluk "${name}" added to ${d}`, 'OK', { duration: 2500 });
        this.onTDistrict(d);
      },
      error: (e) => { this.saving.set(false); this.fail(e); },
    });
  }

  // --- Village / GP ---
  onGDistrict(d: string): void {
    this.gDistrict.set(d);
    this.gTaluk.set(undefined);
    this.gTaluks.set([]);
    this.gGps.set([]);
    if (d) this.loc.taluks(d).subscribe((t) => this.gTaluks.set(t));
  }
  onGTaluk(t: string): void {
    this.gTaluk.set(t);
    this.gGps.set([]);
    const d = this.gDistrict();
    if (d && t) this.loc.gramPanchayats(d, t).subscribe((g) => this.gGps.set(g));
  }
  submitGp(): void {
    const d = this.gDistrict(), t = this.gTaluk(), name = this.newGp.trim();
    if (!d) { this.snack.open('Select a district', 'OK', { duration: 2500 }); return; }
    if (!t) { this.snack.open('Select a taluk', 'OK', { duration: 2500 }); return; }
    if (!name) { this.snack.open('Enter a village / GP name', 'OK', { duration: 2500 }); return; }
    this.saving.set(true);
    this.loc.addGramPanchayat(d, t, name).subscribe({
      next: () => {
        this.saving.set(false);
        this.newGp = '';
        this.snack.open(`Village/GP "${name}" added to ${t}`, 'OK', { duration: 2500 });
        this.onGTaluk(t);
      },
      error: (e) => { this.saving.set(false); this.fail(e); },
    });
  }

  // --- District edit / delete ---
  startEditDistrict(d: string): void { this.editingDistrict.set(d); this.editName = d; }
  saveDistrict(old: string): void {
    const n = this.editName.trim();
    if (!n || n === old) { this.editingDistrict.set(null); return; }
    this.loc.renameDistrict(old, n).subscribe({
      next: () => { this.editingDistrict.set(null); this.snack.open('District renamed', 'OK', { duration: 2000 }); this.loadDistricts(); },
      error: (e) => this.fail(e),
    });
  }
  removeDistrict(d: string): void {
    if (!confirm(`Delete district "${d}" and ALL its taluks & villages?`)) return;
    this.loc.deleteDistrict(d).subscribe({
      next: () => { this.snack.open('District deleted', 'OK', { duration: 2000 }); this.loadDistricts(); },
      error: (e) => this.fail(e),
    });
  }

  // --- Taluk edit / delete ---
  startEditTaluk(t: string): void { this.editingTaluk.set(t); this.editName = t; }
  saveTaluk(old: string): void {
    const d = this.tDistrict(), n = this.editName.trim();
    if (!d || !n || n === old) { this.editingTaluk.set(null); return; }
    this.loc.renameTaluk(d, old, n).subscribe({
      next: () => { this.editingTaluk.set(null); this.snack.open('Taluk renamed', 'OK', { duration: 2000 }); this.onTDistrict(d); },
      error: (e) => this.fail(e),
    });
  }
  removeTaluk(t: string): void {
    const d = this.tDistrict();
    if (!d || !confirm(`Delete taluk "${t}" and its villages?`)) return;
    this.loc.deleteTaluk(d, t).subscribe({
      next: () => { this.snack.open('Taluk deleted', 'OK', { duration: 2000 }); this.onTDistrict(d); },
      error: (e) => this.fail(e),
    });
  }

  // --- Village / GP edit / delete ---
  startEditGp(g: string): void { this.editingGp.set(g); this.editName = g; }
  saveGp(old: string): void {
    const d = this.gDistrict(), t = this.gTaluk(), n = this.editName.trim();
    if (!d || !t || !n || n === old) { this.editingGp.set(null); return; }
    this.loc.renameGramPanchayat(d, t, old, n).subscribe({
      next: () => { this.editingGp.set(null); this.snack.open('Village/GP renamed', 'OK', { duration: 2000 }); this.onGTaluk(t); },
      error: (e) => this.fail(e),
    });
  }
  removeGp(g: string): void {
    const d = this.gDistrict(), t = this.gTaluk();
    if (!d || !t || !confirm(`Delete village / GP "${g}"?`)) return;
    this.loc.deleteGramPanchayat(d, t, g).subscribe({
      next: () => { this.snack.open('Village/GP deleted', 'OK', { duration: 2000 }); this.onGTaluk(t); },
      error: (e) => this.fail(e),
    });
  }

  private fail(e: any): void {
    this.snack.open(e?.error?.message || 'Could not save', 'OK', { duration: 3500 });
  }
}
