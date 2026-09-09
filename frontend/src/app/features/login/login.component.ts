import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
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

/**
 * The hosted backend sleeps when idle, so the first request after a pause has to
 * wake it — that can take up to a minute and otherwise just looks like a hang.
 * If a request is still running after this long, we say so instead.
 */
const WAKE_NOTICE_MS = 4000;

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
export class LoginComponent implements OnInit, OnDestroy {
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  ngOnDestroy(): void {
    clearTimeout(this.wakeTimer);
  }

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
  hidePassword = true;   // password show/hide (eye) toggle

  // Student (OTP) login
  identifier = '';
  otp = '';
  otpSent = signal(false);
  otpTarget = signal('');
  devOtp = signal('');

  loading = signal(false);
  error = signal('');
  info = signal('');
  /** True once a request has run long enough that the server is probably still waking up. */
  waking = signal(false);
  private wakeTimer: ReturnType<typeof setTimeout> | undefined;

  /** Begin a request: clear errors, show the spinner, and arm the "waking up" notice. */
  private startRequest(): void {
    this.loading.set(true);
    this.error.set('');
    this.waking.set(false);
    clearTimeout(this.wakeTimer);
    this.wakeTimer = setTimeout(() => this.waking.set(true), WAKE_NOTICE_MS);
  }

  /** End a request, whatever the outcome. */
  private endRequest(): void {
    this.loading.set(false);
    this.waking.set(false);
    clearTimeout(this.wakeTimer);
  }

  /**
   * A network-level failure (status 0) or a gateway timeout usually means the sleeping
   * backend didn't wake in time — say that rather than the misleading "Login failed".
   */
  private message(err: any, fallback: string): string {
    if (err?.status === 0 || err?.status === 502 || err?.status === 503 || err?.status === 504) {
      return 'Could not reach the server — it may still be starting up. Please try again in a moment.';
    }
    return err?.error?.message || fallback;
  }

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
    this.startRequest();
    this.auth.login(this.email.trim(), this.password).subscribe({
      next: () => {
        this.endRequest();
        this.router.navigateByUrl(this.auth.homeRoute());
      },
      error: (err) => {
        this.endRequest();
        this.error.set(this.message(err, 'Login failed'));
      },
    });
  }

  // --- Student OTP login ---
  sendOtp(): void {
    if (!this.identifier.trim()) return;
    this.startRequest();
    this.info.set('');
    this.devOtp.set('');
    this.auth.requestOtp(this.identifier.trim()).subscribe({
      next: (res) => {
        this.endRequest();
        this.otpSent.set(true);
        this.otpTarget.set(res.target || '');
        this.info.set(res.message || 'OTP sent.');
        this.devOtp.set(res.devOtp || '');
      },
      error: (err) => {
        this.endRequest();
        this.error.set(this.message(err, 'Could not send OTP'));
      },
    });
  }

  verifyOtp(): void {
    if (!this.otp.trim()) return;
    this.startRequest();
    this.auth.verifyOtp(this.identifier.trim(), this.otp.trim()).subscribe({
      next: () => {
        this.endRequest();
        this.router.navigateByUrl(this.auth.homeRoute());
      },
      error: (err) => {
        this.endRequest();
        this.error.set(this.message(err, 'Invalid OTP'));
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
