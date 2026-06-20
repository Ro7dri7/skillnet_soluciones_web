import { environment } from '../../../environments/environment';

/**
 * Rutas de medios normalizadas. Para fetch/display usar mediaBackendUrl (API directa),
 * no rutas relativas en iframe (Vite devuelve index.html con X-Frame-Options: deny).
 */
const MEDIA_FILES_PREFIX = '/api/v1/media/files/';

export function resolveMediaUrl(raw: string | null | undefined): string {
  if (!raw?.trim()) {
    return '';
  }
  const trimmed = raw.trim();

  if (trimmed.startsWith(MEDIA_FILES_PREFIX)) {
    return trimmed;
  }

  const markerIdx = trimmed.indexOf(MEDIA_FILES_PREFIX);
  if (markerIdx >= 0) {
    return trimmed.slice(markerIdx);
  }

  if (trimmed.startsWith('courses/')) {
    return `${MEDIA_FILES_PREFIX}${trimmed}`;
  }

  return trimmed;
}

/** URL absoluta al backend (Spring) para descargar/mostrar archivos. */
export function mediaBackendUrl(raw: string | null | undefined): string {
  const relative = resolveMediaUrl(raw);
  if (!relative) {
    return '';
  }
  if (/^https?:\/\//i.test(relative)) {
    const markerIdx = relative.indexOf(MEDIA_FILES_PREFIX);
    if (markerIdx >= 0) {
      const apiOrigin = environment.apiUrl.replace(/\/api\/v1\/?$/, '');
      return `${apiOrigin}${relative.slice(markerIdx)}`;
    }
    return relative;
  }
  const apiOrigin = environment.apiUrl.replace(/\/api\/v1\/?$/, '');
  return `${apiOrigin}${relative}`;
}

/** URL absoluta para abrir en nueva pestaña. */
export function absoluteMediaUrl(raw: string | null | undefined): string {
  return mediaBackendUrl(raw);
}

/**
 * URL para cargar el PDF en vista previa (iframe/blob).
 * En dev usa ruta relativa proxyada por ng serve (misma origen, evita X-Frame-Options cross-origin).
 */
export function mediaPreviewFetchUrl(raw: string | null | undefined): string {
  const relative = resolveMediaUrl(raw);
  if (!relative) {
    return '';
  }
  if (relative.startsWith(MEDIA_FILES_PREFIX)) {
    return relative;
  }
  if (/^https?:\/\//i.test(relative)) {
    const markerIdx = relative.indexOf(MEDIA_FILES_PREFIX);
    if (markerIdx >= 0) {
      return relative.slice(markerIdx);
    }
    return relative;
  }
  return mediaBackendUrl(raw);
}
