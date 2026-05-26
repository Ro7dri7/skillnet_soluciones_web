import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService, AppRole } from '../services/auth.service';
import {
  canAssumeInfoproductorRole,
  dashboardPathForRole,
  resolveUserRole,
} from '../../shared/utils/user-role.util';

/**
 * Si la URL del dashboard no coincide con el rol del JWT, intenta cambiar de rol en el API
 * antes de activar la ruta (p. ej. /dashboard/infoproductor con sesión de estudiante).
 */
export const dashboardRoleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requiredRole = route.data['requiredRole'] as AppRole | undefined;
  const user = authService.getCurrentUser();
  const currentRole = resolveUserRole(user, authService.getToken());

  if (!requiredRole || currentRole === requiredRole) {
    return true;
  }

  if (requiredRole === 'infoproductor' && !canAssumeInfoproductorRole(user)) {
    // Intentar igualmente: el backend puede autorizar por cursos como profesor.
  }

  return authService.switchRole(requiredRole).pipe(
    map(() => true),
    catchError(() => of(router.createUrlTree([dashboardPathForRole(currentRole)]))),
  );
};
