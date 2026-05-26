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
    const payload = JSON.parse(atob(padded)) as { role?: string };
    return typeof payload.role === 'string' ? payload.role.toLowerCase() : undefined;
  } catch {
    return undefined;
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

export function isStudentRole(user: User | null, token: string | null = null): boolean {
  return resolveUserRole(user, token) === 'student';
}

export function isInfoproductorRole(user: User | null, token: string | null = null): boolean {
  return resolveUserRole(user, token) === 'infoproductor';
}

export type DashboardSlug = 'estudiante' | 'infoproductor';

/** Segmento de URL del panel según el rol activo. */
export function dashboardSlugForRole(role: string | undefined): DashboardSlug {
  return role === 'infoproductor' ? 'infoproductor' : 'estudiante';
}

export function dashboardPathForRole(role: string | undefined): string {
  return `/dashboard/${dashboardSlugForRole(role)}`;
}

/** Puede usar la vista infoproductor (modelo dual Hotmart / Lernymart). */
export function canAssumeInfoproductorRole(user: User | null): boolean {
  if (!user) {
    return false;
  }
  if (user.role === 'admin') {
    return false;
  }
  return user.infoproductor !== false;
}

/** Muestra el conmutador Estudiante / Infoproductor (oculto solo para admin). */
export function canSwitchDashboardRole(user: User | null): boolean {
  if (!user) {
    return false;
  }
  const role = resolveUserRole(user);
  return role !== 'admin';
}
