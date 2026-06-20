import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ProducerPlanOffering {
  id: number;
  section: string;
  title: string;
  description?: string;
  priceUsd: number;
  iconClass?: string;
  capabilityKey?: string;
  includedUses?: number;
  featured?: boolean;
  features?: unknown;
}

export interface ProducerEntitlement {
  id: number;
  status: string;
  usesRemaining: number;
  capabilityKey?: string;
  offeringTitle?: string;
  createdAt?: string;
}

export interface ProducerCapabilityStatus {
  capabilityKey: string;
  active: boolean;
  usesRemaining: number;
}

export interface PlanCheckoutRequest {
  serviceOfferingId: number;
  amount: number;
  paymentToken?: string;
}

export interface PlanCheckoutResponse {
  message: string;
  status: string;
  paymentId?: number;
}

@Injectable({ providedIn: 'root' })
export class ProducerPlansService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/producer/plans`;

  listOfferings(): Observable<ProducerPlanOffering[]> {
    return this.http.get<ProducerPlanOffering[]>(`${this.baseUrl}/offerings`);
  }

  listEntitlements(): Observable<ProducerEntitlement[]> {
    return this.http.get<ProducerEntitlement[]>(`${this.baseUrl}/entitlements`);
  }

  capabilities(): Observable<Record<string, ProducerCapabilityStatus>> {
    return this.http.get<Record<string, ProducerCapabilityStatus>>(`${this.baseUrl}/capabilities`);
  }

  checkoutStripe(payload: PlanCheckoutRequest): Observable<PlanCheckoutResponse> {
    return this.http.post<PlanCheckoutResponse>(`${this.baseUrl}/checkout/stripe`, payload);
  }
}
