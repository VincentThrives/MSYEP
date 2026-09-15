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

import { DataService } from '../../core/data.service';
import { Center, Role, Zone } from '../../core/models';
import { SearchSelectComponent } from '../../shared/search-select.component';

// Students are created via Create Student / self-registration and sign in with OTP only —
// so no password-based STUDENT login can be created from the Logins page.
const ROLES: Role[] = ['ADMIN', 'ZONE', 'CENTER', 'STAFF', 'FINANCE'];

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatSnackBarModule, MatTooltipModule,
    SearchSelectComponent,
  ],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss',
})
export class UsersComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);

  cols = ['name', 'email', 'role', 'status', 'actions'];
  roles = ROLES;
  users = signal<any[]>([]);
  zones = signal<Zone[]>([]);
  centers = signal<Center[]>([]);
  editing = signal(false);
  form: any = this.blank();
  hidePassword = true;   // password show/hide (eye) toggle

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

  /**
   * Enable/disable a login. Deactivating keeps the account and its history but refuses sign-in,
   * so it is the reversible alternative to deleting.
   */
  toggleActive(u: any): void {
    const next = !(u.active !== false);
    const verb = next ? 'Activate' : 'Deactivate';
    if (!confirm(`${verb} login "${u.email}"?`)) return;
    this.data.setUserActive(u.id, next).subscribe({
      next: () => {
        this.snack.open(next ? 'Login activated' : 'Login deactivated', 'OK', { duration: 2500 });
        this.load();
      },
      error: (e) => this.snack.open(e?.error?.message || `${verb} failed`, 'OK', { duration: 4000 }),
    });
  }

  /** Set a new password for any login (the old one is not needed). */
  resetPassword(u: any): void {
    const pwd = prompt(`New password for "${u.email}" (at least 6 characters):`);
    if (pwd === null) return;                       // cancelled
    if (pwd.trim().length < 6) {
      this.snack.open('Password must be at least 6 characters', 'OK', { duration: 3000 });
      return;
    }
    this.data.resetUserPassword(u.id, pwd).subscribe({
      next: () => this.snack.open(`Password updated for ${u.email}`, 'OK', { duration: 3500 }),
      error: (e) => this.snack.open(e?.error?.message || 'Password reset failed', 'OK', { duration: 4000 }),
    });
  }

  remove(u: any): void {
    if (!confirm(`Delete login "${u.email}"?\n\nThis cannot be undone — use Deactivate instead if you may need it later.`)) return;
    this.data.deleteUser(u.id).subscribe(() => {
      this.snack.open('Login deleted', 'OK', { duration: 2000 });
      this.load();
    });
  }
}
