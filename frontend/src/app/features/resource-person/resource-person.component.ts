import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { DataService } from '../../core/data.service';
import { AuthService } from '../../core/auth.service';
import { SearchSelectComponent } from '../../shared/search-select.component';
import { Center, RESOURCE_ORG_GROUPS, ResourcePerson, Zone } from '../../core/models';

@Component({
  selector: 'app-resource-person',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatSnackBarModule, SearchSelectComponent,
  ],
  templateUrl: './resource-person.component.html',
  styleUrl: './resource-person.component.scss',
})
export class ResourcePersonComponent {
  private data = inject(DataService);
  private auth = inject(AuthService);
  private snack = inject(MatSnackBar);

  readonly MAX = 10;
  readonly orgGroups = RESOURCE_ORG_GROUPS;

  /** CENTER acts on its own center; ADMIN picks Zone→Center; ZONE picks one of its own centers. */
  readonly role = this.auth.role();
  readonly isCenter = this.role === 'CENTER';
  readonly isAdmin = this.role === 'SUPER_ADMIN' || this.role === 'ADMIN';
  zones = signal<Zone[]>([]);
  selectedZoneId = signal<string | undefined>(undefined);
  centers = signal<Center[]>([]);
  selectedCenterId = signal<string | undefined>(undefined);
  centersForPicker(): Center[] {
    if (!this.isAdmin) return this.centers();
    const zid = this.selectedZoneId();
    if (!zid) return [];
    // Show centers linked to the chosen zone, plus any not yet linked to a zone,
    // so legacy/unassigned centers stay reachable instead of a dead-end empty list.
    return this.centers().filter((c) => c.zoneId === zid || !c.zoneId);
  }
  get ready(): boolean {
    return this.isCenter || !!this.selectedCenterId();
  }
  private centerId(): string | undefined {
    return this.isCenter ? undefined : this.selectedCenterId();
  }

  count = 1;
  persons: ResourcePerson[] = [{}];
  saving = signal(false);

  constructor() {
    if (this.isCenter) {
      this.loadForCenter();
    } else {
      this.data.centers().subscribe((c) => this.centers.set(c));
      if (this.isAdmin) this.data.zones().subscribe((z) => this.zones.set(z));
    }
  }

  onZone(id: string): void {
    this.selectedZoneId.set(id || undefined);
    this.selectedCenterId.set(undefined);
    this.count = 1;
    this.persons = [{}];
  }

  onCenter(id: string): void {
    this.selectedCenterId.set(id || undefined);
    this.count = 1;
    this.persons = [{}];
    if (id) this.loadForCenter();
  }

  private loadForCenter(): void {
    this.data.resourcePersonsGet(this.centerId()).subscribe((r) => {
      if (r) {
        this.count = r.countRequired || 1;
        this.persons = (r.persons?.length ? r.persons : [{}]).map((p: ResourcePerson) => ({ ...p }));
        this.syncRows();
      }
    });
  }

  /** Keep the number of person rows in sync with the required count (1..10). */
  syncRows(): void {
    this.count = Math.max(1, Math.min(this.MAX, Number(this.count) || 1));
    while (this.persons.length < this.count) this.persons.push({});
    if (this.persons.length > this.count) this.persons = this.persons.slice(0, this.count);
  }

  private payload() {
    return { countRequired: this.count, persons: this.persons };
  }

  save(): void {
    if (this.persons.every((p) => !p.organization && !p.name)) {
      this.snack.open('Add at least one resource person', 'OK', { duration: 2500 });
      return;
    }
    this.saving.set(true);
    this.data.resourcePersonsSave(this.payload(), this.centerId()).subscribe({
      next: () => { this.saving.set(false); this.snack.open('Saved', 'OK', { duration: 2000 }); },
      error: (e) => this.fail(e),
    });
  }

  /** Save then download the request letter. */
  submitLetter(): void {
    this.saving.set(true);
    this.data.resourcePersonsSave(this.payload(), this.centerId()).subscribe({
      next: () => {
        this.data.resourcePersonsLetter(this.centerId()).subscribe({
          next: (blob) => {
            this.saving.set(false);
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url; a.download = 'Resource-Person-Letter.pdf'; a.click();
            URL.revokeObjectURL(url);
            this.snack.open('Submitted — request letter downloaded.', 'OK', { duration: 3500 });
          },
          error: (e) => this.fail(e),
        });
      },
      error: (e) => this.fail(e),
    });
  }

  private fail(e: any): void {
    this.saving.set(false);
    this.snack.open(e?.error?.message || 'Could not save', 'OK', { duration: 3500 });
  }
}
