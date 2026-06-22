import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
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
    MatFormFieldModule, MatInputModule, MatSnackBarModule,
    LocationPickerComponent, SearchSelectComponent,
  ],
  templateUrl: './staff-list.component.html',
  styleUrl: './staff-list.component.scss',
})
export class StaffListComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);

  cols = ['name', 'designation', 'phone', 'zone', 'district', 'actions'];
  staff = signal<Staff[]>([]);
  zones = signal<Zone[]>([]);
  centers = signal<Center[]>([]);
  editing = signal(false);
  form: Staff = this.blank();

  constructor() {
    this.load();
    this.data.zones().subscribe((z) => this.zones.set(z));
    this.data.centers().subscribe((c) => this.centers.set(c));
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
    this.editing.set(true);
  }

  edit(s: Staff): void {
    this.form = { ...s };
    this.editing.set(true);
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
