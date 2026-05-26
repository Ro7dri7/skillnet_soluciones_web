import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CourseService } from '../../../../core/services/course.service';
import { ManageLayoutSaveService } from '../../../../core/services/manage-layout-save.service';
import { ProducerCoursesService } from '../../../../core/services/producer-courses.service';
import { ToastService } from '../../../../core/services/toast.service';
import { OFFICIAL_CURRENCIES } from '../../../marketplace/data/categories.data';
import { currencySymbol } from '../../utils/currency.util';
import {
  DEFAULT_POSITIVE_PRICE,
  getPositivePriceError,
  normalizePositivePriceString,
  parsePositivePrice,
} from '../../../../shared/utils/price-validation.util';

const PLATFORM_FEE_RATE = 0.08;

@Component({
  selector: 'app-manage-pricing',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: './manage-pricing.component.html',
  styleUrl: './manage-pricing.component.scss',
})
export class ManagePricingComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly courseService = inject(CourseService);
  private readonly producerCourses = inject(ProducerCoursesService);
  private readonly manageSave = inject(ManageLayoutSaveService);
  private readonly toast = inject(ToastService);

  readonly currencies = OFFICIAL_CURRENCIES;
  readonly currencySymbol = currencySymbol;
  readonly loading = signal(true);

  readonly currency = signal('USD');
  readonly price = signal(DEFAULT_POSITIVE_PRICE);
  readonly hasDiscount = signal(false);
  readonly discountPrice = signal('');
  readonly affiliateCommission = signal('15');
  readonly affiliationType = signal<'all' | 'approved' | 'none'>('all');

  readonly priceError = signal<string | null>(null);
  readonly discountError = signal<string | null>(null);
  readonly showInvalidBanner = signal(false);

  private courseId: number | null = null;
  private lastValidPrice = DEFAULT_POSITIVE_PRICE;
  private lastValidDiscount = '';

  readonly hasInvalidPrices = computed(
    () =>
      !!getPositivePriceError(this.price(), 'precio base') ||
      (this.hasDiscount() && !!getPositivePriceError(this.discountPrice(), 'precio con descuento')),
  );

  readonly parsedPrice = computed(() => parsePositivePrice(this.price()) ?? 0);
  readonly parsedCommission = computed(() => {
    const n = parseFloat(this.affiliationType() === 'none' ? '0' : this.affiliateCommission());
    return Number.isNaN(n) ? 0 : Math.min(100, Math.max(0, n));
  });

  readonly platformFee = computed(() => this.parsedPrice() * PLATFORM_FEE_RATE);
  readonly netSubtotal = computed(() => this.parsedPrice() * (1 - PLATFORM_FEE_RATE));
  readonly affiliateAmount = computed(() => this.netSubtotal() * (this.parsedCommission() / 100));
  readonly instructorEarnings = computed(() => this.netSubtotal() - this.affiliateAmount());

  ngOnInit(): void {
    const idParam = this.route.parent?.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : NaN;
    if (Number.isNaN(id)) {
      this.loading.set(false);
      return;
    }
    this.courseId = id;
    this.manageSave.registerSaveHandler(() => this.persistPricing());
    void this.loadCourse(id);
  }

  ngOnDestroy(): void {
    this.manageSave.unregisterSaveHandler();
  }

  onPriceInput(value: string): void {
    this.price.set(value);
    this.priceError.set(null);
    this.showInvalidBanner.set(false);
  }

  onDiscountInput(value: string): void {
    this.discountPrice.set(value);
    this.discountError.set(null);
    this.showInvalidBanner.set(false);
  }

  onBasePriceBlur(): void {
    const err = getPositivePriceError(this.price(), 'precio base');
    if (!err) {
      this.lastValidPrice = this.price().trim();
      this.priceError.set(null);
      return;
    }
    this.notifyPriceProblem(err, this.lastValidPrice);
    this.price.set(this.lastValidPrice);
    this.priceError.set(err);
  }

  onDiscountPriceBlur(): void {
    if (!this.hasDiscount()) {
      this.discountError.set(null);
      return;
    }
    const err = getPositivePriceError(this.discountPrice(), 'precio con descuento');
    if (!err) {
      this.lastValidDiscount = this.discountPrice().trim();
      this.discountError.set(null);
      return;
    }
    const fallback = this.lastValidDiscount || DEFAULT_POSITIVE_PRICE;
    this.notifyPriceProblem(err, fallback);
    this.discountPrice.set(fallback);
    this.discountError.set(err);
  }

  setAffiliationType(type: 'all' | 'approved' | 'none'): void {
    this.affiliationType.set(type);
  }

  private notifyPriceProblem(message: string, restoreTo: string): void {
    window.alert(`${message} Se restauró el valor a ${restoreTo}.`);
  }

  private async loadCourse(id: number): Promise<void> {
    try {
      const course = await firstValueFrom(this.courseService.getCourse(id));
      const cur = (course.currency ?? 'USD').toUpperCase();
      this.currency.set(cur);

      const onSale = course.onSale ?? false;
      const base = onSale
        ? normalizePositivePriceString(course.originalPrice ?? course.price, DEFAULT_POSITIVE_PRICE)
        : normalizePositivePriceString(course.price, DEFAULT_POSITIVE_PRICE);
      this.price.set(base);
      this.lastValidPrice = base;

      const discount = onSale
        ? normalizePositivePriceString(course.price, '')
        : '';
      this.hasDiscount.set(onSale && discount !== '');
      this.discountPrice.set(discount);
      if (discount) {
        this.lastValidDiscount = discount;
      }

      const commission = course.affiliateCommission ?? 0;
      this.affiliateCommission.set(String(commission));
      const policy = (course.affiliatePolicy ?? 'all').toLowerCase();
      if (policy === 'approved' || policy === 'none') {
        this.affiliationType.set(policy);
      } else {
        this.affiliationType.set('all');
      }
    } catch {
      this.toast.error('No se pudo cargar la configuración de precios.');
    } finally {
      this.loading.set(false);
    }
  }

  private async persistPricing(): Promise<void> {
    if (this.hasInvalidPrices()) {
      this.showInvalidBanner.set(true);
      throw new Error('Formato de precio inválido');
    }

    const baseErr = getPositivePriceError(this.price(), 'precio base');
    if (baseErr) {
      this.onBasePriceBlur();
      throw new Error(baseErr);
    }

    const base = parsePositivePrice(this.price())!;
    let onSale = false;
    let discount: number | undefined;

    if (this.hasDiscount()) {
      const discErr = getPositivePriceError(this.discountPrice(), 'precio con descuento');
      if (discErr) {
        this.onDiscountPriceBlur();
        throw new Error(discErr);
      }
      discount = parsePositivePrice(this.discountPrice())!;
      onSale = true;
    }

    const commission =
      this.affiliationType() === 'none' ? 0 : parseFloat(this.affiliateCommission()) || 0;

    if (!this.courseId) {
      return;
    }

    await firstValueFrom(
      this.producerCourses.updatePricing(this.courseId, {
        currency: this.currency(),
        price: base,
        onSale,
        discountPrice: discount,
        affiliateCommission: commission,
        affiliationType: this.affiliationType(),
      }),
    );
    this.showInvalidBanner.set(false);
  }
}
