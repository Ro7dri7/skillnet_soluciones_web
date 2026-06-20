import { CourseResponse } from '../models/course.model';

export interface CatalogDisplayPrices {
  displayPrice: number;
  displayOldPrice: number;
  showStrikethrough: boolean;
}

export function getCatalogDisplayPrices(course: CourseResponse): CatalogDisplayPrices {
  const price = course.price ?? 0;
  const original = course.originalPrice ?? price;
  const onSale = course.onSale === true && original > price;
  return {
    displayPrice: price,
    displayOldPrice: original,
    showStrikethrough: onSale,
  };
}

export function getCatalogDisplayPricesWithOverride(
  course: CourseResponse,
  discountedPrice: number | null,
): CatalogDisplayPrices {
  if (discountedPrice != null && discountedPrice >= 0) {
    const base = getCatalogDisplayPrices(course);
    return {
      displayPrice: discountedPrice,
      displayOldPrice: base.displayOldPrice > discountedPrice ? base.displayOldPrice : base.displayPrice,
      showStrikethrough: base.displayOldPrice > discountedPrice || base.showStrikethrough,
    };
  }
  return getCatalogDisplayPrices(course);
}

export function formatCatalogPrice(amount: number, currency = 'USD'): string {
  return new Intl.NumberFormat('es-PE', {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(amount);
}
