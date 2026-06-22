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

import { DataService } from '../../core/data.service';
import { Center, Role, Zone } from '../../core/models';
import { SearchSelectComponent } from '../../shared/search-select.component';

const ROLES: Role[] = ['ADMIN', 'ZONE', 'CENTER', 'STAFF', 'FINANCE', 'STUDENT'];

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatSnackBarModule, SearchSelectComponent,
  ],
  template: `
    <div class="head">
      <h1>Logins</h1>
      <button mat-flat-button color="primary" (click)="editing.set(!editing())">
        <mat-icon>person_add</mat-icon> Create Login
      </button>
    </div>

    <div class="form-panel" *ngIf="editing()">
      <div class="row">
        <mat-form-field appearance="outline"><mat-label>Name</mat-label>
          <input matInput [(ngModel)]="form.name" /></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Email</mat-label>
          <input matInput [(ngModel)]="form.email" /></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Password</mat-label>
          <input matInput [(ngModel)]="form.password" /></mat-form-field>
        <app-search-select label="Role" [options]="roles"
          [(value)]="form.role"></app-search-select>
      </div>
      <div class="row">
        <app-search-select *ngIf="form.role === 'ZONE'" label="Zone" [options]="zones()"
          valueKey="id" labelKey="name" [(value)]="form.zoneId"></app-search-select>
        <app-search-select *ngIf="form.role === 'CENTER'" label="Center" [options]="centers()"
          valueKey="id" labelKey="name" [(value)]="form.centerId"></app-search-select>
      </div>
      <div class="form-actions">
        <button mat-button (click)="editing.set(false)">Cancel</button>
        <button mat-flat-button color="primary" (click)="save()">Create</button>
      </div>
    </div>

    <table mat-table [dataSource]="users()" class="mat-elevation-z1 full">
      <ng-container matColumnDef="name">
        <th mat-header-cell *matHeaderCellDef>Name</th>
        <td mat-cell *matCellDef="let u">{{ u.name }}</td>
      </ng-container>
      <ng-container matColumnDef="email">
        <th mat-header-cell *matHeaderCellDef>Email</th>
        <td mat-cell *matCellDef="let u">{{ u.email }}</td>
      </ng-container>
      <ng-container matColumnDef="role">
        <th mat-header-cell *matHeaderCellDef>Role</th>
        <td mat-cell *matCellDef="let u">{{ u.role }}</td>
      </ng-container>
      <ng-container matColumnDef="actions">
        <th mat-header-cell *matHeaderCellDef></th>
        <td mat-cell *matCellDef="let u">
          <button mat-icon-button color="warn" (click)="remove(u)"><mat-icon>delete</mat-icon></button>
        </td>
      </ng-container>
      <tr mat-header-row *matHeaderRowDef="cols"></tr>
      <tr mat-row *matRowDef="let row; columns: cols"></tr>
    </table>
  `,
  styles: [`
    .head { display: flex; justify-content: space-between; align-items: center; }
    .form-panel { background: #fff; border: 1px solid #eee; border-radius: 8px; padding: 16px; margin: 16px 0; }
    .row { display: flex; gap: 12px; flex-wrap: wrap; }
    .row mat-form-field { flex: 1; min-width: 180px; }
    .form-actions { display: flex; justify-content: flex-end; gap: 8px; }
    .full { width: 100%; margin-top: 12px; }
  `],
})
export class UsersComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);

  cols = ['name', 'email', 'role', 'actions'];
  roles = ROLES;
  users = signal<any[]>([]);
  zones = signal<Zone[]>([]);
  centers = signal<Center[]>([]);
  editing = signal(false);
  form: any = this.blank();

  constructor() {
    this.load();
    this.data.zones().subscribe((z) => this.zones.set(z));
    this.data.centers().subscribe((c) => this.centers.set(c));
  }

  blank() {
    return { name: '', email: '', password: '', role: 'ZONE' as Role };
  }

  load(): void {
    this.data.users().subscribe((u) => this.users.set(u));
  }

  save(): void {
    if (!this.form.name || !this.form.email || !this.form.password) {
      this.snack.open('Name, email and password are required', 'OK', { duration: 2500 });
      return;
    }
    this.data.createUser(this.form).subscribe({
      next: () => {
        this.snack.open('Login created', 'OK', { duration: 2000 });
        this.form = this.blank();
        this.editing.set(false);
        this.load();
      },
      error: (e) => this.snack.open(e?.error?.message || 'Create failed', 'OK', { duration: 3000 }),
    });
  }

  remove(u: any): void {
    if (!confirm(`Delete login "${u.email}"?`)) return;
    this.data.deleteUser(u.id).subscribe(() => {
      this.snack.open('Login deleted', 'OK', { duration: 2000 });
      this.load();
    });
  }
}
