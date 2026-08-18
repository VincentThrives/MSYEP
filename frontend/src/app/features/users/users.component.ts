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

// Students are created via Create Student / self-registration and sign in with OTP only —
// so no password-based STUDENT login can be created from the Logins page.
const ROLES: Role[] = ['ADMIN', 'ZONE', 'CENTER', 'STAFF', 'FINANCE'];

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatSnackBarModule, SearchSelectComponent,
  ],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss',
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

  remove(u: any): void {
    if (!confirm(`Delete login "${u.email}"?`)) return;
    this.data.deleteUser(u.id).subscribe(() => {
      this.snack.open('Login deleted', 'OK', { duration: 2000 });
      this.load();
    });
  }
}
