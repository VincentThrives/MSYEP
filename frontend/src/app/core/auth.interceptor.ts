import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const snack = inject(MatSnackBar);
  const token = auth.token;

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((err) => {
      // 401 = not authenticated (missing / expired token) → bounce to login.
      if (err.status === 401 && auth.isLoggedIn()) {
        auth.logout();
        snack.open('Your session has expired — please sign in again.', 'OK', { duration: 4000 });
        router.navigate(['/login']);
      }
      return throwError(() => err);
    })
  );
};
