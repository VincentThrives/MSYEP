import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { DataService } from '../../core/data.service';
import { COURSES, Zone } from '../../core/models';
import { LocationPickerComponent } from '../../shared/location-picker.component';

@Component({
  selector: 'app-zone-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatChipsModule, MatSnackBarModule, LocationPickerComponent,
  ],
  template: `
    <div class="head">
      <h1>Zone Management</h1>
      <div class="actions">
        <button mat-stroked-button (click)="fileInput.click()">
          <mat-icon>upload_file</mat-icon> Import Excel
        </button>
        <input #fileInput type="file" hidden accept=".xlsx" (change)="onImport($event)" />
        <button mat-flat-button color="primary" (click)="newZone()">
          <mat-icon>add</mat-icon> Create Zone
        </button>
      </div>
    </div>

    <div class="form-panel" *ngIf="editing()">
      <h3>{{ form.id ? 'Edit' : 'Create' }} Zone (University)</h3>
      <div class="row">
        <mat-form-field appearance="outline"><mat-label>University Name</mat-label>
          <input matInput [(ngModel)]="form.name" /></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Code</mat-label>
          <input matInput [(ngModel)]="form.code" /></mat-form-field>
      </div>
      <div class="row">
        <app-location-picker
          [(district)]="form.district" [(taluk)]="form.taluk"
          [(gramPanchayat)]="form.gramPanchayat"></app-location-picker>
      </div>
      <div class="row">
        <mat-form-field appearance="outline"><mat-label>Contact Person</mat-label>
          <input matInput [(ngModel)]="form.contactPerson" /></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Contact Email</mat-label>
          <input matInput [(ngModel)]="form.contactEmail" /></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Contact Phone</mat-label>
          <input matInput [(ngModel)]="form.contactPhone" /></mat-form-field>
      </div>
      <div class="courses">
        <span class="lbl">Courses / Degrees:</span>
        <mat-chip-listbox multiple>
          <mat-chip-option *ngFor="let c of allCourses" [selected]="form.courses?.includes(c)"
            (selectionChange)="toggleCourse(c, $event.selected)">{{ c }}</mat-chip-option>
        </mat-chip-listbox>
      </div>
      <div class="form-actions">
        <button mat-button (click)="editing.set(false)">Cancel</button>
        <button mat-flat-button color="primary" (click)="save()">Save</button>
      </div>
    </div>

    <table mat-table [dataSource]="zones()" class="mat-elevation-z1 full">
      <ng-container matColumnDef="name">
        <th mat-header-cell *matHeaderCellDef>University</th>
        <td mat-cell *matCellDef="let z">{{ z.name }}</td>
      </ng-container>
      <ng-container matColumnDef="code">
        <th mat-header-cell *matHeaderCellDef>Code</th>
        <td mat-cell *matCellDef="let z">{{ z.code }}</td>
      </ng-container>
      <ng-container matColumnDef="district">
        <th mat-header-cell *matHeaderCellDef>District</th>
        <td mat-cell *matCellDef="let z">{{ z.district }}</td>
      </ng-container>
      <ng-container matColumnDef="courses">
        <th mat-header-cell *matHeaderCellDef>Courses</th>
        <td mat-cell *matCellDef="let z">{{ (z.courses || []).join(', ') }}</td>
      </ng-container>
      <ng-container matColumnDef="actions">
        <th mat-header-cell *matHeaderCellDef>Actions</th>
        <td mat-cell *matCellDef="let z">
          <button mat-icon-button (click)="edit(z)"><mat-icon>edit</mat-icon></button>
          <button mat-icon-button (click)="downloadPdf(z)"><mat-icon>picture_as_pdf</mat-icon></button>
          <button mat-icon-button color="warn" (click)="remove(z)"><mat-icon>delete</mat-icon></button>
        </td>
      </ng-container>
      <tr mat-header-row *matHeaderRowDef="cols"></tr>
      <tr mat-row *matRowDef="let row; columns: cols"></tr>
    </table>
    <p *ngIf="!zones().length" class="empty">No zones yet. Create one or import from Excel.</p>
  `,
  styles: [`
    .head { display: flex; justify-content: space-between; align-items: center; }
    .actions { display: flex; gap: 10px; }
    .form-panel { background: #fff; border: 1px solid #eee; border-radius: 8px; padding: 16px; margin: 16px 0; }
    .row { display: flex; gap: 12px; flex-wrap: wrap; }
    .row mat-form-field { flex: 1; min-width: 200px; }
    .courses { margin: 8px 0; }
    .lbl { font-size: 13px; color: #666; margin-right: 8px; }
    .form-actions { display: flex; justify-content: flex-end; gap: 8px; }
    .full { width: 100%; margin-top: 12px; }
    .empty { color: #999; margin-top: 16px; }
  `],
})
export class ZoneListComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);

  cols = ['name', 'code', 'district', 'courses', 'actions'];
  allCourses = COURSES;
  zones = signal<Zone[]>([]);
  editing = signal(false);
  form: Zone = this.blank();

  constructor() {
    this.load();
  }

  load(): void {
    this.data.zones().subscribe((z) => this.zones.set(z));
  }

  blank(): Zone {
    return { name: '', code: '', district: '', courses: [] };
  }

  newZone(): void {
    this.form = this.blank();
    this.editing.set(true);
  }

  edit(z: Zone): void {
    this.form = { ...z, courses: [...(z.courses || [])] };
    this.editing.set(true);
  }

  toggleCourse(course: string, selected: boolean): void {
    const set = new Set(this.form.courses || []);
    selected ? set.add(course) : set.delete(course);
    this.form.courses = [...set];
  }

  save(): void {
    if (!this.form.name) {
      this.snack.open('University name is required', 'OK', { duration: 2500 });
      return;
    }
    const obs = this.form.id
      ? this.data.updateZone(this.form.id, this.form)
      : this.data.createZone(this.form);
    obs.subscribe({
      next: () => {
        this.snack.open('Zone saved', 'OK', { duration: 2000 });
        this.editing.set(false);
        this.load();
      },
      error: (e) => this.snack.open(e?.error?.message || 'Save failed', 'OK', { duration: 3000 }),
    });
  }

  remove(z: Zone): void {
    if (!z.id || !confirm(`Delete zone "${z.name}"?`)) return;
    this.data.deleteZone(z.id).subscribe(() => {
      this.snack.open('Zone deleted', 'OK', { duration: 2000 });
      this.load();
    });
  }

  downloadPdf(z: Zone): void {
    if (!z.id) return;
    this.data.zonePdf(z.id).subscribe((blob) => this.saveBlob(blob, `zone-${z.name}.pdf`));
  }

  onImport(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.data.importZones(file).subscribe({
      next: (r) => {
        this.snack.open(`Imported ${r.imported} zones`, 'OK', { duration: 2500 });
        this.load();
      },
      error: (e) => this.snack.open(e?.error?.message || 'Import failed', 'OK', { duration: 3000 }),
    });
    input.value = '';
  }

  private saveBlob(blob: Blob, name: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = name;
    a.click();
    URL.revokeObjectURL(url);
  }
}
