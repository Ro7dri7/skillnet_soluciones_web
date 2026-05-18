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

/** Rol efectivo del usuario (API, flags legacy o claim JWT). */
export function resolveUserRole(user: User | null, token: string | null = null): string | undefined {
  if (!user) {
    return roleFromJwt(token);
  }
  if (user.role) {
    return user.role.toLowerCase();
  }
  if (user.student) {
    return 'student';
  }
  if (user.infoproductor) {
    return 'infoproductor';
  }
  return roleFromJwt(token);
}

export function isStudentRole(user: User | null, token: string | null = null): boolean {
  return resolveUserRole(user, token) === 'student';
}
