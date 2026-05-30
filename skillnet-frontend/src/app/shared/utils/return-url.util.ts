/** Evita open-redirect: solo rutas internas relativas. */
export function sanitizeReturnUrl(url: string | null | undefined): string | null {
  if (!url) {
    return null;
  }
  const trimmed = url.trim();
  if (!trimmed.startsWith('/') || trimmed.startsWith('//')) {
    return null;
  }
  if (trimmed.startsWith('/login') || trimmed.startsWith('/register')) {
    return null;
  }
  return trimmed;
}

export function returnUrlFromQuery(query: Record<string, unknown>): string | null {
  const raw = query['returnUrl'];
  if (typeof raw === 'string') {
    return sanitizeReturnUrl(raw);
  }
  if (Array.isArray(raw) && typeof raw[0] === 'string') {
    return sanitizeReturnUrl(raw[0]);
  }
  return null;
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const base64 = token.split('.')[1]?.replace(/-/g, '+').replace(/_/g, '/');
    if (!base64) {
      return null;
    }
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    return JSON.parse(atob(padded)) as Record<string, unknown>;
  } catch {
    return null;
  }
}

/** Token presente pero expirado o malformado → tratar como sesión inválida. */
export function isJwtExpired(token: string | null | undefined): boolean {
  if (!token) {
    return true;
  }
  const payload = decodeJwtPayload(token);
  if (!payload) {
    return true;
  }
  const exp = payload['exp'];
  if (typeof exp !== 'number') {
    return false;
  }
  return Date.now() >= exp * 1000;
}

export function hasValidSessionToken(token: string | null | undefined): boolean {
  return Boolean(token) && !isJwtExpired(token);
}
