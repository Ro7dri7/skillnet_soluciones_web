import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { isAdminAccount, resolveUserRole } from '../../shared/utils/user-role.util';

export const adminRoleGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const user = authService.getCurrentUser();
  const token = authService.getToken();

  if (isAdminAccount(user, token)) {
    return true;
  }

  return router.createUrlTree(['/dashboard/estudiante']);
};
