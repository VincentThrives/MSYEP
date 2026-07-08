import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ApiService } from './api.service';
import {
  AuthResponse, IdName, OtpRequestResult, Role,
  StudentSelfRegisterRequest, StudentSelfRegisterResult,
} from './models';

const STORAGE_KEY = 'msyep_auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = inject(ApiService);

  private readonly _user = signal<AuthResponse | null>(this.load());
  readonly user = this._user.asReadonly();
  readonly isLoggedIn = computed(() => !!this._user());
  readonly role = computed<Role | null>(() => this._user()?.role ?? null);

  login(email: string, password: string): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/login', { email, password }).pipe(
      tap((res) => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(res));
        this._user.set(res);
      })
    );
  }

  /** Public student self-registration → creates an OTP-only account. */
  registerStudent(payload: StudentSelfRegisterRequest): Observable<StudentSelfRegisterResult> {
    return this.api.post<StudentSelfRegisterResult>('/auth/register', payload);
  }

  /** Public zone list (id + name) for the registration form. */
  publicZones(): Observable<IdName[]> {
    return this.api.get<IdName[]>('/public/zones');
  }

  /** Public center list (id + name), optionally filtered by zone. */
  publicCenters(zoneId?: string): Observable<IdName[]> {
    return this.api.get<IdName[]>('/public/centers', { zoneId });
  }

  /** Step 1 of student login: request an OTP by User ID / email or mobile number. */
  requestOtp(identifier: string): Observable<OtpRequestResult> {
    return this.api.post<OtpRequestResult>('/auth/otp/request', { identifier });
  }

  /** Step 2 of student login: verify the OTP and establish the session. */
  verifyOtp(identifier: string, otp: string): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/otp/verify', { identifier, otp }).pipe(
      tap((res) => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(res));
        this._user.set(res);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
    this._user.set(null);
  }

  get token(): string | null {
    return this._user()?.token ?? null;
  }

  /** Landing route for each role after login. */
  homeRoute(): string {
    switch (this.role()) {
      case 'FINANCE':
        return '/app/finance';
      case 'STUDENT':
        return '/app/students';
      default:
        return '/app/dashboard';
    }
  }

  private load(): AuthResponse | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as AuthResponse) : null;
  }
}
