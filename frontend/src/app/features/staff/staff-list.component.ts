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
  template: `
    <div class="head">
      <h1>Staff Management</h1>
      <button mat-flat-button color="primary" (click)="newStaff()">
        <mat-icon>person_add</mat-icon> Add Staff
      </button>
    </div>

    <div class="form-panel" *ngIf="editing()">
      <h3>{{ form.id ? 'Edit' : 'Add' }} Staff</h3>
      <div class="row">
        <mat-form-field appearance="outline"><mat-label>Name</mat-label>
          <input matInput [(ngModel)]="form.name" /></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Designation</mat-label>
          <input matInput [(ngModel)]="form.designation" /></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Phone</mat-label>
          <input matInput [(ngModel)]="form.phone" /></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Email</mat-label>
          <input matInput [(ngModel)]="form.email" /></mat-form-field>
      </div>
      <div class="row">
        <app-search-select label="Zone (University)" [options]="zones()" valueKey="id"
          labelKey="name" [emptyOption]="true" [(value)]="form.zoneId"></app-search-select>
        <app-search-select label="Center (College)" [options]="centers()" valueKey="id"
          labelKey="name" [emptyOption]="true" [(value)]="form.centerId"></app-search-select>
      </div>
      <div class="row">
        <app-location-picker
          [(district)]="form.district" [(taluk)]="form.taluk"
          [(gramPanchayat)]="form.gramPanchayat"></app-location-picker>
      </div>
      <div class="form-actions">
        <button mat-button (click)="editing.set(false)">Cancel</button>
        <button mat-flat-button color="primary" (click)="save()">Save</button>
      </div>
    </div>

    <table mat-table [dataSource]="staff()" class="mat-elevation-z1 full">
      <ng-container matColumnDef="name">
        <th mat-header-cell *matHeaderCellDef>Name</th>
        <td mat-cell *matCellDef="let s">{{ s.name }}</td>
      </ng-container>
      <ng-container matColumnDef="designation">
        <th mat-header-cell *matHeaderCellDef>Designation</th>
        <td mat-cell *matCellDef="let s">{{ s.designation }}</td>
      </ng-container>
      <ng-container matColumnDef="phone">
        <th mat-header-cell *matHeaderCellDef>Phone</th>
        <td mat-cell *matCellDef="let s">{{ s.phone }}</td>
      </ng-container>
      <ng-container matColumnDef="zone">
        <th mat-header-cell *matHeaderCellDef>Zone</th>
        <td mat-cell *matCellDef="let s">{{ zoneName(s.zoneId) }}</td>
      </ng-container>
      <ng-container matColumnDef="district">
        <th mat-header-cell *matHeaderCellDef>District</th>
        <td mat-cell *matCellDef="let s">{{ s.district }}</td>
      </ng-container>
      <ng-container matColumnDef="actions">
        <th mat-header-cell *matHeaderCellDef>Actions</th>
        <td mat-cell *matCellDef="let s">
          <button mat-icon-button (click)="edit(s)"><mat-icon>edit</mat-icon></button>
          <button mat-icon-button color="warn" (click)="remove(s)"><mat-icon>delete</mat-icon></button>
        </td>
      </ng-container>
      <tr mat-header-row *matHeaderRowDef="cols"></tr>
      <tr mat-row *matRowDef="let row; columns: cols"></tr>
    </table>
    <p *ngIf="!staff().length" class="empty">No staff yet.</p>
  `,
  styles: [`
    .head { display: flex; justify-content: space-between; align-items: center; }
    .form-panel { background: #fff; border: 1px solid #eee; border-radius: 14px; padding: 16px; margin: 16px 0; }
    .row { display: flex; gap: 12px; flex-wrap: wrap; }
    .row mat-form-field { flex: 1; min-width: 180px; }
    .form-actions { display: flex; justify-content: flex-end; gap: 8px; }
    .full { width: 100%; margin-top: 12px; }
    .empty { color: #999; margin-top: 16px; }
  `],
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
