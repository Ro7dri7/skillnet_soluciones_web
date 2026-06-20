import { CurrencyPipe } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  loadStripe,
  Stripe,
  StripeCardCvcElement,
  StripeCardExpiryElement,
  StripeCardNumberElement,
  StripeElements,
} from '@stripe/stripe-js';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  ProducerCapabilityStatus,
  ProducerEntitlement,
  ProducerPlanOffering,
  ProducerPlansService,
} from '../../../../core/services/producer-plans.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

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
  selector: 'app-producer-plans',
  standalone: true,
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './producer-plans.component.html',
  styleUrl: './producer-plans.component.scss',
})
export class ProducerPlansComponent implements AfterViewInit, OnDestroy {
  private readonly plansService = inject(ProducerPlansService);

  readonly environment = environment;

  readonly cardNumberMountRef = viewChild<ElementRef<HTMLDivElement>>('stripeCardNumberMount');
  readonly cardExpiryMountRef = viewChild<ElementRef<HTMLDivElement>>('stripeCardExpiryMount');
  readonly cardCvcMountRef = viewChild<ElementRef<HTMLDivElement>>('stripeCardCvcMount');

  readonly loading = signal(true);
  readonly paying = signal(false);
  readonly error = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly offerings = signal<ProducerPlanOffering[]>([]);
  readonly entitlements = signal<ProducerEntitlement[]>([]);
  readonly capabilities = signal<Record<string, ProducerCapabilityStatus>>({});
  readonly selectedOffering = signal<ProducerPlanOffering | null>(null);
  readonly stripeReady = signal(false);
  readonly stripeCardComplete = signal(false);

  private stripe: Stripe | null = null;
  private elements: StripeElements | null = null;
  private cardNumberElement: StripeCardNumberElement | null = null;
  private cardExpiryElement: StripeCardExpiryElement | null = null;
  private cardCvcElement: StripeCardCvcElement | null = null;
  private stripeInitStarted = false;
  private cardNumberComplete = false;
  private cardExpiryComplete = false;
  private cardCvcComplete = false;

  ngAfterViewInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.cardNumberElement?.destroy();
    this.cardExpiryElement?.destroy();
    this.cardCvcElement?.destroy();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    Promise.all([
      firstValueFrom(this.plansService.listOfferings()),
      firstValueFrom(this.plansService.listEntitlements()),
      firstValueFrom(this.plansService.capabilities()),
    ])
      .then(([offerings, entitlements, capabilities]) => {
        this.offerings.set(offerings ?? []);
        this.entitlements.set(entitlements ?? []);
        this.capabilities.set(capabilities ?? {});
        this.loading.set(false);
      })
      .catch((err) => {
        this.error.set(messageFromHttpError(err, 'No se pudieron cargar los planes.'));
        this.loading.set(false);
      });
  }

  selectPlan(offering: ProducerPlanOffering): void {
    this.selectedOffering.set(offering);
    this.successMessage.set(null);
    this.error.set(null);
    this.stripeInitStarted = false;
    this.stripeReady.set(false);
    setTimeout(() => void this.initStripeElementsWithRetry(), 50);
  }

  cancelCheckout(): void {
    this.selectedOffering.set(null);
    this.stripeReady.set(false);
    this.stripeCardComplete.set(false);
  }

  capabilityLabel(key?: string): string {
    if (key === 'gamma_ebook') return 'Ebooks con Gamma';
    if (key === 'podcast_ai') return 'Podcasts con ElevenLabs';
    return key ?? 'Plan';
  }

  usesFor(key: string): number {
    return this.capabilities()[key]?.usesRemaining ?? 0;
  }

  hasActive(key: string): boolean {
    return this.capabilities()[key]?.active === true;
  }

  isPaidPlan(offering: ProducerPlanOffering): boolean {
    return Number(offering.priceUsd) > 0;
  }

  requiresStripeCard(): boolean {
    const selected = this.selectedOffering();
    return !!selected && this.isPaidPlan(selected) && !!this.environment.stripePublicKey;
  }

  async purchaseSelected(): Promise<void> {
    const offering = this.selectedOffering();
    if (!offering) {
      return;
    }
    this.paying.set(true);
    this.error.set(null);

    try {
      let paymentToken: string | undefined;
      if (Number(offering.priceUsd) > 0) {
        if (!this.stripe || !this.cardNumberElement) {
          throw new Error('Stripe no está listo. Espera un momento e intenta de nuevo.');
        }
        const result = await this.stripe.createToken(this.cardNumberElement);
        if (result.error || !result.token) {
          throw new Error(result.error?.message ?? 'No se pudo tokenizar la tarjeta.');
        }
        paymentToken = result.token.id;
      }

      const response = await firstValueFrom(
        this.plansService.checkoutStripe({
          serviceOfferingId: offering.id,
          amount: Number(offering.priceUsd),
          paymentToken,
        }),
      );
      this.successMessage.set(response.message ?? 'Plan activado.');
      this.selectedOffering.set(null);
      this.load();
    } catch (err) {
      this.error.set(messageFromHttpError(err, 'No se pudo completar la compra del plan.'));
    } finally {
      this.paying.set(false);
    }
  }

  private async initStripeElementsWithRetry(attempt = 0): Promise<void> {
    if (this.stripeInitStarted || !this.selectedOffering()) {
      return;
    }
    const numberMount = this.cardNumberMountRef()?.nativeElement;
    const expiryMount = this.cardExpiryMountRef()?.nativeElement;
    const cvcMount = this.cardCvcMountRef()?.nativeElement;
    if (!numberMount || !expiryMount || !cvcMount) {
      if (attempt < 12) {
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
      this.stripeReady.set(true);
      return;
    }
    try {
      this.stripe = await loadStripe(environment.stripePublicKey);
      if (!this.stripe) {
        this.error.set('No se pudo cargar Stripe.');
        return;
      }
      this.elements = this.stripe.elements({ locale: 'es' });
      this.cardNumberElement = this.elements.create('cardNumber', {
        style: STRIPE_ELEMENT_STYLE,
        showIcon: true,
      });
      this.cardExpiryElement = this.elements.create('cardExpiry', { style: STRIPE_ELEMENT_STYLE });
      this.cardCvcElement = this.elements.create('cardCvc', { style: STRIPE_ELEMENT_STYLE });
      this.cardNumberElement.on('change', (event) => {
        this.cardNumberComplete = event.complete;
        this.syncStripeCardComplete();
      });
      this.cardExpiryElement.on('change', (event) => {
        this.cardExpiryComplete = event.complete;
        this.syncStripeCardComplete();
      });
      this.cardCvcElement.on('change', (event) => {
        this.cardCvcComplete = event.complete;
        this.syncStripeCardComplete();
      });
      this.cardNumberElement.mount(numberMount);
      this.cardExpiryElement.mount(expiryMount);
      this.cardCvcElement.mount(cvcMount);
      this.stripeReady.set(true);
    } catch {
      this.error.set('Error inicializando Stripe.');
    }
  }

  private syncStripeCardComplete(): void {
    this.stripeCardComplete.set(
      this.cardNumberComplete && this.cardExpiryComplete && this.cardCvcComplete,
    );
  }
}
