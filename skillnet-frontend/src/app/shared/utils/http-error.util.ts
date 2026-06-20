import { HttpErrorResponse } from '@angular/common/http';

export function messageFromHttpError(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as { message?: string } | null;
    if (body?.message) {
      return translateApiMessage(body.message);
    }
    if (error.status === 401) {
      return 'Tu sesión expiró o no es válida. Inicia sesión de nuevo.';
    }
    if (error.status === 403) {
      return 'No tienes permiso para esta acción. Cambia a vista Infoproductor o usa un curso tuyo.';
    }
    if (error.status === 0) {
      return 'Error de conexión con el servidor.';
    }
  }
  return fallback;
}

export function isCourseOwnershipError(error: unknown): boolean {
  if (!(error instanceof HttpErrorResponse) || error.status !== 403) {
    return false;
  }
  const body = error.error as { message?: string } | null;
  const msg = (body?.message ?? '').toLowerCase();
  return msg.includes('own') || msg.includes('permiso') || msg.includes('permission');
}

function translateApiMessage(message: string): string {
  const lower = message.toLowerCase();
  if (lower.includes('you do not own this course')) {
    return 'Este curso no pertenece a tu cuenta. Se creará un borrador nuevo al guardar.';
  }
  if (lower.includes('an unexpected error occurred')) {
    return 'Se ha producido un error inesperado. Inténtalo de nuevo más tarde.';
  }
  return message;
}
