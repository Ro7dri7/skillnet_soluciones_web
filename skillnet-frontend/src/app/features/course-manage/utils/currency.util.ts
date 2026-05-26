const SYMBOLS: Record<string, string> = {
  USD: '$',
  EUR: '€',
  PEN: 'S/',
  COP: '$',
  MXN: '$',
  CLP: '$',
  BRL: 'R$',
  ARS: '$',
};

export function currencySymbol(code: string): string {
  return SYMBOLS[code?.toUpperCase()] ?? '$';
}
