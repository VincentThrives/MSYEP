import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonToggleModule } from '@angular/material/button-toggle';

import { AuthService } from '../../core/auth.service';

type Mode = 'staff' | 'zone' | 'center' | 'student';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatProgressBarModule, MatIconModule,
    MatButtonToggleModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  ngOnInit(): void {
    // Prefill from /login?id=... (e.g. straight after self-registration).
    const id = this.route.snapshot.queryParamMap.get('id');
    if (id) {
      this.identifier = id;
      this.mode.set('student');
    }
  }

  mode = signal<Mode>('staff');

  // Staff (password) login
  email = '';
  password = '';

  // Student (OTP) login
  identifier = '';
  otp = '';
  otpSent = signal(false);
  otpTarget = signal('');
  devOtp = signal('');

  loading = signal(false);
  error = signal('');
  info = signal('');

  /** Staff, Zone and Center all sign in with email + password (the account's role decides the portal). */
  isPasswordMode(): boolean {
    return this.mode() !== 'student';
  }

  modeHint(): string {
    switch (this.mode()) {
      case 'zone': return 'Zone / Franchise login';
      case 'center': return 'Center / College login';
      case 'student': return 'Student login';
      default: return 'Staff login';
    }
  }

  setMode(mode: Mode): void {
    this.mode.set(mode);
    this.error.set('');
    this.info.set('');
    this.otpSent.set(false);
    this.otp = '';
    this.devOtp.set('');
  }

  // --- Staff password login ---
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

  // --- Student OTP login ---
  sendOtp(): void {
    if (!this.identifier.trim()) return;
    this.loading.set(true);
    this.error.set('');
    this.info.set('');
    this.devOtp.set('');
    this.auth.requestOtp(this.identifier.trim()).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.otpSent.set(true);
        this.otpTarget.set(res.target || '');
        this.info.set(res.message || 'OTP sent.');
        this.devOtp.set(res.devOtp || '');
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message || 'Could not send OTP');
      },
    });
  }

  verifyOtp(): void {
    if (!this.otp.trim()) return;
    this.loading.set(true);
    this.error.set('');
    this.auth.verifyOtp(this.identifier.trim(), this.otp.trim()).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigateByUrl(this.auth.homeRoute());
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message || 'Invalid OTP');
      },
    });
  }

  resetOtp(): void {
    this.otpSent.set(false);
    this.otp = '';
    this.devOtp.set('');
    this.error.set('');
    this.info.set('');
  }
}
