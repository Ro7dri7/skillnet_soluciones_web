export const DEFAULT_POSITIVE_PRICE = '10';

export function normalizePositivePriceString(value: unknown, fallback = DEFAULT_POSITIVE_PRICE): string {
  const trimmed = String(value ?? '').trim();
  const num = Number(trimmed);
  if (trimmed !== '' && !Number.isNaN(num) && num > 0) {
    return trimmed;
  }
  return fallback;
}

export function getPositivePriceError(value: string, fieldLabel = 'precio'): string | null {
  const trimmed = String(value).trim();
  if (trimmed === '') {
    return `El ${fieldLabel} es obligatorio.`;
  }
  if (!/^\d+(\.\d+)?$/.test(trimmed)) {
    return `El ${fieldLabel} solo puede contener números (sin letras ni símbolos).`;
  }
  const num = Number(trimmed);
  if (Number.isNaN(num)) {
    return `El ${fieldLabel} debe ser un número válido.`;
  }
  if (num <= 0) {
    return `El ${fieldLabel} debe ser mayor a cero.`;
  }
  return null;
}

export function parsePositivePrice(value: string): number | null {
  const err = getPositivePriceError(value);
  if (err) {
    return null;
  }
  return Number(String(value).trim());
}
