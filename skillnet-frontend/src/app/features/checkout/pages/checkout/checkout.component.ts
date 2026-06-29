import { CurrencyPipe } from '@angular/common';
import {
  AfterViewInit,
  Component,
  DestroyRef,
  ElementRef,
  OnDestroy,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  loadStripe,
  Stripe,
  StripeCardCvcElement,
  StripeCardExpiryElement,
  StripeCardNumberElement,
  StripeElements,
} from '@stripe/stripe-js';
import { firstValueFrom, startWith } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { AuthService } from '../../../../core/services/auth.service';
import { CartItem } from '../../../../core/models/cart-item.model';
import { CartService } from '../../../../core/services/cart.service';
import { CheckoutService } from '../../../../core/services/checkout.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

type PaymentMethod = 'card' | 'yape';

interface AppliedCoupon {
  code: string;
  percent: number | null;
  amountOff: number | null;
  label: string;
}

const STRIPE_ELEMENT_STYLE = {
  base: {
    color: '#032b60',
    fontFamily: 'Inter, system-ui, sans-serif',
    fontSize: '16px',
    '::placeholder': { color: '#94a3b8' },
  },
  invalid: { color: '#dc2626' },
} as const;

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss',
})
export class CheckoutComponent implements AfterViewInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly checkoutService = inject(CheckoutService);
  private readonly destroyRef = inject(DestroyRef);

  readonly cartService = inject(CartService);

  readonly cardNumberMountRef = viewChild<ElementRef<HTMLDivElement>>('stripeCardNumberMount');
  readonly cardExpiryMountRef = viewChild<ElementRef<HTMLDivElement>>('stripeCardExpiryMount');
  readonly cardCvcMountRef = viewChild<ElementRef<HTMLDivElement>>('stripeCardCvcMount');

  readonly paymentMethod = signal<PaymentMethod>('card');
  readonly couponCode = signal('');
  readonly couponMessage = signal<string | null>(null);
  readonly couponError = signal(false);
  readonly appliedCoupon = signal<AppliedCoupon | null>(null);
  readonly couponApplying = signal(false);
  readonly expandedItemId = signal<number | null>(null);
  readonly isLoading = signal(false);
  readonly paymentSuccess = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly stripeReady = signal(false);
  readonly stripeCardComplete = signal(false);
  readonly personalFormValid = signal(false);

  private stripe: Stripe | null = null;
  private elements: StripeElements | null = null;
  private cardNumberElement: StripeCardNumberElement | null = null;
  private cardExpiryElement: StripeCardExpiryElement | null = null;
  private cardCvcElement: StripeCardCvcElement | null = null;
  private stripeInitStarted = false;

  private cardNumberComplete = false;
  private cardExpiryComplete = false;
  private cardCvcComplete = false;

  readonly personalForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    documentId: ['', [Validators.required, Validators.minLength(6)]],
    yapePhone: [''],
  });

  readonly subtotal = computed(() => this.cartService.cartTotal());

  readonly discountAmount = computed(() => {
    const coupon = this.appliedCoupon();
    if (!coupon) {
      return 0;
    }
    if (coupon.percent != null && coupon.percent > 0) {
      return Math.round(this.subtotal() * (coupon.percent / 100) * 100) / 100;
    }
    if (coupon.amountOff != null && coupon.amountOff > 0) {
      return Math.min(coupon.amountOff, this.subtotal());
    }
    return 0;
  });

  readonly totalToPay = computed(() =>
    Math.max(0, Math.round((this.subtotal() - this.discountAmount()) * 100) / 100),
  );

  readonly missingBillingHint = computed(() => {
    const missing: string[] = [];
    const controls = this.personalForm.controls;
    if (controls.fullName.invalid) {
      missing.push('nombre completo');
    }
    if (controls.email.invalid) {
      missing.push('correo electrónico');
    }
    if (controls.documentId.invalid) {
      missing.push('documento de identidad');
    }
    return missing;
  });

  readonly canSubmitPayment = computed(() => {
    if (this.cartService.cartCount() === 0 || this.isLoading()) {
      return false;
    }
    if (!this.personalFormValid()) {
      return false;
    }
    if (this.totalToPay() <= 0) {
      return true;
    }
    if (this.paymentMethod() === 'card') {
      return this.stripeReady() && this.stripeCardComplete();
    }
    if (this.paymentMethod() === 'yape') {
      const phone = this.personalForm.controls.yapePhone.value.trim();
      return /^9\d{8}$/.test(phone);
    }
    return true;
  });

  readonly stripeDevMockEnabled = environment.stripeDevMock;

  constructor() {
    this.prefillFromUser();
    this.personalForm.statusChanges
      .pipe(startWith(this.personalForm.status), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.personalFormValid.set(this.personalForm.valid);
      });
    this.personalFormValid.set(this.personalForm.valid);
  }

  ngAfterViewInit(): void {
    void this.initStripeElementsWithRetry();
  }

  ngOnDestroy(): void {
    this.cardNumberElement?.destroy();
    this.cardExpiryElement?.destroy();
    this.cardCvcElement?.destroy();
  }

  setPaymentMethod(method: PaymentMethod): void {
    this.paymentMethod.set(method);
    this.errorMessage.set(null);
    if (method === 'card' && this.totalToPay() > 0) {
      this.stripeInitStarted = false;
      setTimeout(() => void this.initStripeElementsWithRetry(), 0);
    }
  }

  toggleItemExpand(itemId: number): void {
    this.expandedItemId.update((current) => (current === itemId ? null : itemId));
  }

  isItemExpanded(itemId: number): boolean {
    return this.expandedItemId() === itemId;
  }

  applyCoupon(): void {
    const code = this.couponCode().trim().toUpperCase();
    if (!code) {
      this.couponMessage.set('Ingresa un código de cupón');
      this.couponError.set(true);
      return;
    }

    const courseIds = this.cartService.cartItems().map((item) => item.id);
    if (courseIds.length === 0) {
      return;
    }

    this.couponApplying.set(true);
    this.checkoutService.quote({ courseIds, couponCode: code }).subscribe({
      next: (quote) => {
        this.couponApplying.set(false);
        if (!quote.couponValid) {
          this.appliedCoupon.set(null);
          this.couponMessage.set(quote.message ?? 'Cupón inválido o expirado');
          this.couponError.set(true);
          return;
        }

        this.appliedCoupon.set({
          code: quote.couponCode ?? code,
          percent: quote.couponPercentOff ?? null,
          amountOff: quote.couponAmountOff != null ? Number(quote.couponAmountOff) : null,
          label: quote.couponLabel ?? 'Descuento aplicado',
        });
        this.couponMessage.set(quote.message ?? `Cupón "${code}" aplicado correctamente`);
        this.couponError.set(false);
      },
      error: (err) => {
        this.couponApplying.set(false);
        this.appliedCoupon.set(null);
        this.couponMessage.set(messageFromHttpError(err, 'No se pudo validar el cupón'));
        this.couponError.set(true);
      },
    });
  }

  removeCoupon(): void {
    this.appliedCoupon.set(null);
    this.couponCode.set('');
    this.couponMessage.set(null);
    this.couponError.set(false);
  }

  async completePayment(): Promise<void> {
    this.errorMessage.set(null);

    if (!this.canSubmitPayment()) {
      this.personalForm.markAllAsTouched();
      this.personalFormValid.set(this.personalForm.valid);
      return;
    }

    if (this.paymentMethod() === 'yape') {
      await this.processYapeCheckout();
      return;
    }

    await this.processStripePayment();
  }

  itemLineTotal(item: CartItem): number {
    const subtotal = this.subtotal();
    const coupon = this.appliedCoupon();
    if (!coupon || subtotal <= 0) {
      return item.price;
    }
    const share = item.price / subtotal;
    return Math.round((item.price - this.discountAmount() * share) * 100) / 100;
  }

  private async initStripeElementsWithRetry(attempt = 0): Promise<void> {
    if (this.stripeInitStarted || this.cartService.cartCount() === 0) {
      return;
    }

    const numberMount = this.cardNumberMountRef()?.nativeElement;
    const expiryMount = this.cardExpiryMountRef()?.nativeElement;
    const cvcMount = this.cardCvcMountRef()?.nativeElement;

    if (!numberMount || !expiryMount || !cvcMount) {
      if (attempt < 10) {
        setTimeout(() => void this.initStripeElementsWithRetry(attempt + 1), 120);
      }
      return;
    }

    this.stripeInitStarted = true;
    await this.initStripeElements(numberMount, expiryMount, cvcMount);
  }

  private async initStripeElements(
    numberMount: HTMLElement,
    expiryMount: HTMLElement,
    cvcMount: HTMLElement,
  ): Promise<void> {
    if (!environment.stripePublicKey) {
      this.errorMessage.set('Stripe no está configurado en este entorno.');
      return;
    }

    try {
      this.stripe = await loadStripe(environment.stripePublicKey);
      if (!this.stripe) {
        this.errorMessage.set('No se pudo cargar el SDK de Stripe.');
        return;
      }

      this.elements = this.stripe.elements({ locale: 'es' });

      this.cardNumberElement = this.elements.create('cardNumber', {
        style: STRIPE_ELEMENT_STYLE,
        showIcon: true,
      });
      this.cardExpiryElement = this.elements.create('cardExpiry', {
        style: STRIPE_ELEMENT_STYLE,
      });
      this.cardCvcElement = this.elements.create('cardCvc', {
        style: STRIPE_ELEMENT_STYLE,
      });

      this.cardNumberElement.on('change', (event) => {
        this.cardNumberComplete = event.complete;
        this.syncStripeCardComplete(event.error?.message);
      });
      this.cardExpiryElement.on('change', (event) => {
        this.cardExpiryComplete = event.complete;
        this.syncStripeCardComplete(event.error?.message);
      });
      this.cardCvcElement.on('change', (event) => {
        this.cardCvcComplete = event.complete;
        this.syncStripeCardComplete(event.error?.message);
      });

      this.cardNumberElement.mount(numberMount);
      this.cardExpiryElement.mount(expiryMount);
      this.cardCvcElement.mount(cvcMount);
      this.stripeReady.set(true);
    } catch {
      this.stripeInitStarted = false;
      this.errorMessage.set('Error al inicializar la pasarela de pagos.');
    }
  }

  private syncStripeCardComplete(errorMessage?: string): void {
    this.stripeCardComplete.set(
      this.cardNumberComplete && this.cardExpiryComplete && this.cardCvcComplete,
    );
    if (errorMessage) {
      this.errorMessage.set(errorMessage);
    } else if (this.stripeCardComplete()) {
      this.errorMessage.set(null);
    }
  }

  private async processStripePayment(): Promise<void> {
    if (!this.authService.getCurrentUser()) {
      this.errorMessage.set('Debes iniciar sesión para completar la compra.');
      return;
    }

    const items = this.cartService.cartItems();
    if (items.length === 0) {
      return;
    }

    if (this.totalToPay() > 0 && !environment.stripeDevMock && (!this.stripe || !this.cardNumberElement)) {
      this.errorMessage.set('La pasarela aún no está lista. Espera un momento e intenta de nuevo.');
      return;
    }

    this.isLoading.set(true);

    try {
      let paymentToken = 'free_checkout';
      if (this.totalToPay() > 0) {
        if (environment.stripeDevMock) {
          paymentToken = 'dev_mock_checkout';
        } else {
          if (!this.stripe || !this.cardNumberElement) {
            this.errorMessage.set('La pasarela aún no está lista. Espera un momento e intenta de nuevo.');
            return;
          }
          const { token, error } = await this.stripe.createToken(this.cardNumberElement);
          if (error || !token) {
            this.errorMessage.set(error?.message ?? 'No se pudo tokenizar la tarjeta.');
            return;
          }
          paymentToken = token.id;
        }
      }

      const response = await firstValueFrom(
        this.checkoutService.processPayment({
          courseId: items[0].id,
          courseIds: items.map((item) => item.id),
          amount: this.totalToPay(),
          paymentToken,
          couponCode: this.appliedCoupon()?.code,
        }),
      );

      this.paymentSuccess.set(true);
      this.cartService.clearCart();
      const paymentId = response.paymentId;
      const query = paymentId != null ? { paymentId: String(paymentId) } : undefined;
      void this.router.navigate(['/payment/success'], { queryParams: query });
    } catch (err) {
      this.errorMessage.set(messageFromHttpError(err, 'No se pudo procesar el pago con Stripe.'));
    } finally {
      this.isLoading.set(false);
    }
  }

  private async processYapeCheckout(): Promise<void> {
    if (!this.authService.getCurrentUser()) {
      this.errorMessage.set('Debes iniciar sesión para completar la compra.');
      return;
    }
    const items = this.cartService.cartItems();
    if (items.length === 0) {
      return;
    }

    this.isLoading.set(true);
    try {
      const paymentToken =
        this.totalToPay() <= 0 ? 'free_checkout' : environment.stripeDevMock ? 'dev_mock_yape' : null;
      if (!paymentToken && this.totalToPay() > 0) {
        this.errorMessage.set('Yape solo está disponible en modo simulación local. Usa tarjeta o contacta soporte.');
        return;
      }

      const response = await firstValueFrom(
        this.checkoutService.processPayment({
          courseId: items[0].id,
          courseIds: items.map((item) => item.id),
          amount: this.totalToPay(),
          paymentToken: paymentToken ?? 'dev_mock_yape',
          couponCode: this.appliedCoupon()?.code,
        }),
      );

      this.paymentSuccess.set(true);
      this.cartService.clearCart();
      const paymentId = response.paymentId;
      void this.router.navigate(['/payment/success'], {
        queryParams: paymentId != null ? { paymentId: String(paymentId) } : undefined,
      });
    } catch (err) {
      this.errorMessage.set(messageFromHttpError(err, 'No se pudo procesar el pago.'));
    } finally {
      this.isLoading.set(false);
    }
  }

  private prefillFromUser(): void {
    const user = this.authService.getCurrentUser();
    if (!user) {
      return;
    }
    const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
    this.personalForm.patchValue({
      fullName: fullName || user.username,
      email: user.email ?? '',
    });
    this.personalFormValid.set(this.personalForm.valid);
  }
}
