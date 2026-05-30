import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { sanitizeReturnUrl, hasValidSessionToken } from '../../shared/utils/return-url.util';

export const authGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.getToken();
  if (hasValidSessionToken(token)) {
    return true;
  }

  if (token) {
    authService.logout();
  }

  const returnUrl = sanitizeReturnUrl(state.url);
  return router.createUrlTree(
    ['/login'],
    returnUrl ? { queryParams: { returnUrl } } : undefined,
  );
};
