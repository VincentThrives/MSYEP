import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';

import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatProgressBarModule, MatIconModule,
  ],
  template: `
    <div class="wrap">
      <div class="card">
        <div class="badge"><mat-icon>school</mat-icon></div>
        <div class="logo">MSYEP</div>
        <div class="tag">Yukta Kaushalya Tarabethi</div>
        <mat-progress-bar *ngIf="loading()" mode="indeterminate" class="bar"></mat-progress-bar>
        <form (ngSubmit)="submit()">
          <mat-form-field appearance="outline" class="full">
            <mat-label>Email</mat-label>
            <mat-icon matPrefix>mail</mat-icon>
            <input matInput type="email" name="email" [(ngModel)]="email" required autocomplete="username" />
          </mat-form-field>
          <mat-form-field appearance="outline" class="full">
            <mat-label>Password</mat-label>
            <mat-icon matPrefix>lock</mat-icon>
            <input matInput type="password" name="password" [(ngModel)]="password" required autocomplete="current-password" />
          </mat-form-field>
          <p class="error" *ngIf="error()">{{ error() }}</p>
          <button mat-flat-button class="full login-btn" type="submit" [disabled]="loading()">
            <mat-icon>login</mat-icon> Sign in
          </button>
        </form>
        <div class="hint">Super Admin · Admin · Zone · Center · Finance · Student</div>
      </div>
    </div>
  `,
  styles: [`
    .wrap { min-height: 100vh; display: grid; place-items: center; padding: 16px;
      background:
        radial-gradient(900px 500px at 110% -10%, rgba(232,197,71,.55), transparent 60%),
        linear-gradient(135deg, #0E5132 0%, #1E7A46 50%, #C9A227 130%); }
    .card { width: 370px; max-width: 92vw; padding: 34px 30px 26px; text-align: center;
      background: #fff; border-radius: 22px;
      box-shadow: 0 24px 60px rgba(0,0,0,.30); position: relative; }
    .badge { width: 64px; height: 64px; margin: -62px auto 10px; border-radius: 50%;
      display: grid; place-items: center; color: #fff;
      background: linear-gradient(135deg, #1E7A46, #C9A227);
      box-shadow: 0 8px 20px rgba(14,81,50,.40); }
    .badge mat-icon { font-size: 32px; height: 32px; width: 32px; }
    .logo { font-size: 32px; font-weight: 800; letter-spacing: 3px;
      background: linear-gradient(90deg, #0E5132, #C9A227);
      -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent; }
    .tag { color: #6b7d72; margin-bottom: 20px; font-size: 13px; letter-spacing: .5px; }
    .bar { margin-bottom: 12px; border-radius: 4px; }
    .full { width: 100%; }
    .login-btn { background: linear-gradient(120deg, #1E7A46, #0E5132); color: #fff;
      height: 46px; font-weight: 700; letter-spacing: .5px; border-radius: 12px; }
    .error { color: #d32f2f; font-size: 13px; margin: 0 0 8px; }
    .hint { margin-top: 16px; font-size: 11px; color: #9aa79f; letter-spacing: .3px; }
  `],
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  password = '';
  loading = signal(false);
  error = signal('');

  submit(): void {
    if (!this.email || !this.password) return;
    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.email.trim(), this.password).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigateByUrl(this.auth.homeRoute());
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message || 'Login failed');
      },
    });
  }
}
