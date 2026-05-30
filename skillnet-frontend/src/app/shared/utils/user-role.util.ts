import { User } from '../models/auth.model';

function roleFromJwt(token: string | null): string | undefined {
  if (!token) {
    return undefined;
  }
  try {
    const base64 = token.split('.')[1]?.replace(/-/g, '+').replace(/_/g, '/');
    if (!base64) {
      return undefined;
    }
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const payload = JSON.parse(atob(padded)) as { role?: string; roles?: string[] };
    return typeof payload.role === 'string' ? payload.role.toLowerCase() : undefined;
  } catch {
    return undefined;
  }
}

function adminCapableFromToken(token: string | null): boolean {
  if (!token) {
    return false;
  }
  try {
    const base64 = token.split('.')[1]?.replace(/-/g, '+').replace(/_/g, '/');
    if (!base64) {
      return false;
    }
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const payload = JSON.parse(atob(padded)) as { roles?: string[] };
    return Array.isArray(payload.roles) && payload.roles.some((r) => r.toLowerCase() === 'admin');
  } catch {
    return false;
  }
}

/** Rol efectivo: el claim JWT manda (alineado con Spring Security). */
export function resolveUserRole(user: User | null, token: string | null = null): string | undefined {
  const jwtRole = roleFromJwt(token);
  if (jwtRole) {
    return jwtRole;
  }
  if (!user) {
    return undefined;
  }
  if (user.activeRole) {
    return user.activeRole.toLowerCase();
  }
  if (user.role) {
    return user.role.toLowerCase();
  }
  if (user.infoproductor) {
    return 'infoproductor';
  }
  if (user.student !== false) {
    return 'student';
  }
  return undefined;
}

export function isAdminAccount(user: User | null, token: string | null = null): boolean {
  if (!user) {
    return false;
  }
  if (user.superUser === true || user.staff === true) {
    return true;
  }
  if (user.role === 'admin' || user.activeRole === 'admin') {
    return true;
  }
  return adminCapableFromToken(token);
}

export function isStudentRole(user: User | null, token: string | null = null): boolean {
  return resolveUserRole(user, token) === 'student';
}

export function isInfoproductorRole(user: User | null, token: string | null = null): boolean {
  return resolveUserRole(user, token) === 'infoproductor';
}

export type DashboardSlug = 'estudiante' | 'infoproductor' | 'admin';

export function dashboardSlugForRole(role: string | undefined): DashboardSlug {
  if (role === 'admin') {
    return 'admin';
  }
  return role === 'infoproductor' ? 'infoproductor' : 'estudiante';
}

export function dashboardPathForRole(role: string | undefined): string {
  if (role === 'admin') {
    return '/admin';
  }
  return `/dashboard/${role === 'infoproductor' ? 'infoproductor' : 'estudiante'}`;
}

export function canAssumeInfoproductorRole(user: User | null): boolean {
  if (!user) {
    return false;
  }
  if (isAdminAccount(user)) {
    return true;
  }
  return user.infoproductor !== false;
}

/** Admin puede alternar entre las tres vistas. */
export function canSwitchDashboardRole(user: User | null): boolean {
  if (!user) {
    return false;
  }
  if (isAdminAccount(user)) {
    return true;
  }
  return resolveUserRole(user) !== 'admin';
}
