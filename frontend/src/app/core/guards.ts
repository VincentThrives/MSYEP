import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from './models';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) return true;
  router.navigate(['/login']);
  return false;
};

/** Restrict a route to specific roles (SUPER_ADMIN always allowed). */
export function roleGuard(...roles: Role[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const role = auth.role();
    if (role && (role === 'SUPER_ADMIN' || roles.includes(role))) return true;
    router.navigate([auth.isLoggedIn() ? auth.homeRoute() : '/login']);
    return false;
  };
}
