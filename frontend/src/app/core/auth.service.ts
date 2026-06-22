import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ApiService } from './api.service';
import { AuthResponse, Role } from './models';

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
