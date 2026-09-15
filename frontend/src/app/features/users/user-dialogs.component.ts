import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

/** Styled replacements for the browser's native prompt()/confirm() popups on the Logins page. */

export interface ConfirmData {
  title: string;
  message: string;
  confirmLabel: string;
  danger?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title class="title">
      <mat-icon [class.danger]="data.danger">{{ data.danger ? 'warning' : 'help' }}</mat-icon>
      {{ data.title }}
    </h2>
    <mat-dialog-content><p class="msg">{{ data.message }}</p></mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="ref.close(false)">Cancel</button>
      <button mat-flat-button [color]="data.danger ? 'warn' : 'primary'" (click)="ref.close(true)">
        {{ data.confirmLabel }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .title { display: flex; align-items: center; gap: 8px; color: #0E5132; }
    .title mat-icon.danger { color: #b3261e; }
    .msg { color: #4b5b52; white-space: pre-line; margin: 4px 0 0; }
  `],
})
export class ConfirmDialogComponent {
  ref = inject(MatDialogRef<ConfirmDialogComponent>);
  data = inject<ConfirmData>(MAT_DIALOG_DATA);
}

@Component({
  selector: 'app-reset-password-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatDialogModule, MatButtonModule,
    MatIconModule, MatFormFieldModule, MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title class="title"><mat-icon>key</mat-icon> Reset password</h2>
    <mat-dialog-content>
      <p class="who">for <b>{{ data.email }}</b></p>

      <mat-form-field appearance="outline" class="full">
        <mat-label>New Password</mat-label>
        <mat-icon matPrefix>lock</mat-icon>
        <input matInput [type]="hide1 ? 'password' : 'text'" [(ngModel)]="password"
               autocomplete="new-password" (keyup.enter)="submit()" />
        <button mat-icon-button matSuffix type="button" (click)="hide1 = !hide1"
                [attr.aria-label]="hide1 ? 'Show password' : 'Hide password'">
          <mat-icon>{{ hide1 ? 'visibility_off' : 'visibility' }}</mat-icon>
        </button>
        <mat-hint>At least 6 characters</mat-hint>
      </mat-form-field>

      <mat-form-field appearance="outline" class="full">
        <mat-label>Confirm Password</mat-label>
        <mat-icon matPrefix>lock_reset</mat-icon>
        <input matInput [type]="hide2 ? 'password' : 'text'" [(ngModel)]="confirm"
               autocomplete="new-password" (keyup.enter)="submit()" />
        <button mat-icon-button matSuffix type="button" (click)="hide2 = !hide2"
                [attr.aria-label]="hide2 ? 'Show password' : 'Hide password'">
          <mat-icon>{{ hide2 ? 'visibility_off' : 'visibility' }}</mat-icon>
        </button>
      </mat-form-field>

      <p class="err" *ngIf="error">{{ error }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="ref.close()">Cancel</button>
      <button mat-flat-button color="primary" (click)="submit()">Update Password</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .title { display: flex; align-items: center; gap: 8px; color: #0E5132; }
    .who { color: #6b7d72; margin: 0 0 14px; font-size: 13px; }
    .full { width: 100%; }
    .err { color: #d32f2f; font-size: 13px; margin: 2px 0 0; }
  `],
})
export class ResetPasswordDialogComponent {
  ref = inject(MatDialogRef<ResetPasswordDialogComponent>);
  data = inject<{ email: string }>(MAT_DIALOG_DATA);

  password = '';
  confirm = '';
  hide1 = true;
  hide2 = true;
  error = '';

  submit(): void {
    if (this.password.trim().length < 6) {
      this.error = 'Password must be at least 6 characters.';
      return;
    }
    if (this.password !== this.confirm) {
      this.error = 'Passwords do not match.';
      return;
    }
    this.ref.close(this.password);
  }
}
