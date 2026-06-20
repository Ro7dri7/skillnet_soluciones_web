import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

const PRODUCER_RESERVED_SEGMENTS = new Set(['students', 'quiz-review', 'traffic', 'courses']);

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

function isPublicAppRoute(url: string): boolean {
  const path = url.split('?')[0];
  if (
    path === '/login' ||
    path === '/register' ||
    path === '/password-reset' ||
    path === '/reset-password'
  ) {
    return true;
  }
  if (path === '/marketplace' || path.startsWith('/marketplace/')) {
    return !path.endsWith('/learn');
  }
  if (path === '/catalog' || path.startsWith('/catalog/')) {
    return true;
  }
  const infoproductorMatch = /^\/infoproductor\/([^/]+)$/.exec(path);
  if (infoproductorMatch) {
    return !PRODUCER_RESERVED_SEGMENTS.has(infoproductorMatch[1]);
  }
  return false;
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
      const stayOnPage = isBuilderOrManageRoute(url) || isPublicAppRoute(url);

      if (error.status === 401 && !isAuthEndpoint(req.url)) {
        const hadToken = Boolean(authService.getToken());
        authService.logout();
        if (hadToken && !url.startsWith('/login') && !stayOnPage) {
          void router.navigate(['/login'], {
            queryParams: { returnUrl: url },
            replaceUrl: true,
          });
        }
      }

      if (error.status === 403 && !authService.isLoggedIn() && !url.startsWith('/login') && !isPublicAppRoute(url)) {
        void router.navigate(['/login'], {
          queryParams: { returnUrl: url },
          replaceUrl: true,
        });
      }

      return throwError(() => error);
    }),
  );
};
