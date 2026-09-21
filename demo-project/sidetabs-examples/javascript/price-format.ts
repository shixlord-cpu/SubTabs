import { Currency } from './money';

export const DEFAULT_CURRENCY: Currency = 'EUR';

export function formatPrice(value: number, currency = DEFAULT_CURRENCY): string {
  return `${value.toFixed(2)} ${currency}`;
}

export function parsePrice(raw: string): number {
  return Number.parseFloat(raw.replace(',', '.'));
}
