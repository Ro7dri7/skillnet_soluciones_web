import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

function isBuilderOrManageRoute(url: string): boolean {
  return (
    url.includes('/infoproductor/courses/new') ||
    url.includes('/instructor/courses/')
  );
}

function isAuthEndpoint(url: string): boolean {
  return (
    url.includes('/auth/login') ||
    url.includes('/auth/register') ||
    url.includes('/auth/google')
  );
}

export const authErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }

      const url = router.url;
      const stayOnPage = isBuilderOrManageRoute(url);

      if (error.status === 401 && !isAuthEndpoint(req.url)) {
        const hadToken = Boolean(authService.getToken());
        authService.logout();
        if (hadToken && !url.startsWith('/login') && !stayOnPage) {
          void router.navigate(['/login'], {
            queryParams: { returnUrl: url },
          });
        }
      }

      if (error.status === 403 && !authService.isLoggedIn() && !url.startsWith('/login')) {
        void router.navigate(['/login'], {
          queryParams: { returnUrl: url },
        });
      }

      return throwError(() => error);
    }),
  );
};
