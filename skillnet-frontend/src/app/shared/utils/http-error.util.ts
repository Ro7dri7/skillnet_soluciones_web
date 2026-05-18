import { HttpErrorResponse } from '@angular/common/http';

export function messageFromHttpError(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as { message?: string } | null;
    if (body?.message) {
      return body.message;
    }
    if (error.status === 401) {
      return 'Credenciales inválidas. Verifica tu usuario y contraseña.';
    }
    if (error.status === 0) {
      return 'Error de conexión con el servidor.';
    }
  }
  return fallback;
}
