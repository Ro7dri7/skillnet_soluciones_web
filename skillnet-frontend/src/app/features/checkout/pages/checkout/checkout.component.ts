import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { CartItem } from '../../../../core/models/cart-item.model';
import { CartService } from '../../../../core/services/cart.service';

type PaymentMethod = 'card' | 'yape';

interface AppliedCoupon {
  code: string;
  percent: number;
  label: string;
}

const MOCK_COUPONS: Record<string, { percent: number; label: string }> = {
  SKILL10: { percent: 10, label: '10% de descuento' },
  SKILLNET20: { percent: 20, label: '20% de descuento' },
  WELCOME15: { percent: 15, label: '15% de bienvenida' },
};

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss',
})
export class CheckoutComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  readonly cartService = inject(CartService);

  readonly paymentMethod = signal<PaymentMethod>('card');
  readonly couponCode = signal('');
  readonly couponMessage = signal<string | null>(null);
  readonly couponError = signal(false);
  readonly appliedCoupon = signal<AppliedCoupon | null>(null);
  readonly expandedItemId = signal<number | null>(null);
  readonly isProcessing = signal(false);
  readonly paymentSuccess = signal(false);

  readonly personalForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    documentId: ['', [Validators.required, Validators.minLength(6)]],
  });

  readonly cardForm = this.fb.nonNullable.group({
    cardNumber: ['', [Validators.required, Validators.minLength(15)]],
    expiry: ['', [Validators.required, Validators.pattern(/^(0[1-9]|1[0-2])\/\d{2}$/)]],
    cvv: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(4)]],
  });

  readonly subtotal = computed(() => this.cartService.cartTotal());

  readonly discountAmount = computed(() => {
    const coupon = this.appliedCoupon();
    if (!coupon) {
      return 0;
    }
    return Math.round(this.subtotal() * (coupon.percent / 100) * 100) / 100;
  });

  readonly totalToPay = computed(() =>
    Math.max(0, Math.round((this.subtotal() - this.discountAmount()) * 100) / 100),
  );

  readonly isFormValid = computed(() => {
    const personalOk = this.personalForm.valid;
    if (this.paymentMethod() === 'yape') {
      return personalOk;
    }
    return personalOk && this.cardForm.valid;
  });

  constructor() {
    this.prefillFromUser();
  }

  setPaymentMethod(method: PaymentMethod): void {
    this.paymentMethod.set(method);
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

    const coupon = MOCK_COUPONS[code];
    if (!coupon) {
      this.appliedCoupon.set(null);
      this.couponMessage.set('Cupón inválido o expirado');
      this.couponError.set(true);
      return;
    }

    this.appliedCoupon.set({ code, ...coupon });
    this.couponMessage.set(`Cupón "${code}" aplicado correctamente`);
    this.couponError.set(false);
  }

  removeCoupon(): void {
    this.appliedCoupon.set(null);
    this.couponCode.set('');
    this.couponMessage.set(null);
    this.couponError.set(false);
  }

  completePayment(): void {
    if (!this.isFormValid() || this.cartService.cartCount() === 0) {
      this.personalForm.markAllAsTouched();
      if (this.paymentMethod() === 'card') {
        this.cardForm.markAllAsTouched();
      }
      return;
    }

    this.isProcessing.set(true);
    setTimeout(() => {
      this.isProcessing.set(false);
      this.paymentSuccess.set(true);
      this.cartService.clearCart();
      setTimeout(() => {
        void this.router.navigate(['/marketplace']);
      }, 2200);
    }, 1400);
  }

  itemLineTotal(item: CartItem): number {
    const coupon = this.appliedCoupon();
    if (!coupon) {
      return item.price;
    }
    return Math.round(item.price * (1 - coupon.percent / 100) * 100) / 100;
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
  }
}
