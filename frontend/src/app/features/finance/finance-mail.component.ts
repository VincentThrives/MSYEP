import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { DataService } from '../../core/data.service';
import { GramPanchayat } from '../../core/models';

/** Manage the Gram Panchayat mail IDs used by the finance wing (add / edit / delete). */
@Component({
  selector: 'app-finance-mail',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatTooltipModule, MatSnackBarModule,
  ],
  templateUrl: './finance-mail.component.html',
  styleUrl: './finance-mail.component.scss',
})
export class FinanceMailComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);

  cols = ['name', 'email', 'taluk', 'district', 'contactPerson', 'actions'];
  rows = signal<GramPanchayat[]>([]);
  form: GramPanchayat = this.blank();
  editingId = signal<string | null>(null);

  constructor() {
    this.fetch();
  }

  private blank(): GramPanchayat {
    return { name: '', email: '', taluk: '', district: '', contactPerson: '' };
  }

  fetch(): void {
    this.data.gramPanchayats().subscribe({
      next: (r) => this.rows.set(r),
      error: (e) => this.snack.open(
        `Couldn't load GP mail IDs — ${e?.error?.message || e?.message || 'request failed'}`, 'OK', { duration: 5000 }),
    });
  }

  edit(gp: GramPanchayat): void {
    this.form = { ...gp };
    this.editingId.set(gp.id ?? null);
  }

  cancel(): void {
    this.form = this.blank();
    this.editingId.set(null);
  }

  save(): void {
    const name = (this.form.name || '').trim();
    const email = (this.form.email || '').trim();
    if (!name || !email) {
      this.snack.open('Gram Panchayat name and mail ID are required', 'OK', { duration: 2500 });
      return;
    }
    this.data.saveGramPanchayat({ ...this.form, name, email }).subscribe({
      next: () => {
        this.snack.open(this.editingId() ? 'Mail ID updated' : 'Mail ID added', 'OK', { duration: 2000 });
        this.cancel();
        this.fetch();
      },
      error: (e) => this.snack.open(e?.error?.message || 'Save failed', 'OK', { duration: 3000 }),
    });
  }

  remove(gp: GramPanchayat): void {
    if (!gp.id) return;
    this.data.deleteGramPanchayat(gp.id).subscribe({
      next: () => {
        this.snack.open('Mail ID deleted', 'OK', { duration: 2000 });
        if (this.editingId() === gp.id) this.cancel();
        this.fetch();
      },
      error: (e) => this.snack.open(e?.error?.message || 'Delete failed', 'OK', { duration: 3000 }),
    });
  }
}
