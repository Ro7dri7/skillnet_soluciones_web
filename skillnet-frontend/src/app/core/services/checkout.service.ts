import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface StripeCheckoutRequest {
  courseId: number;
  courseIds?: number[];
  amount: number;
  paymentToken: string;
}

export interface StripeCheckoutResponse {
  message: string;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class CheckoutService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/checkout`;

  processPayment(payload: StripeCheckoutRequest): Observable<StripeCheckoutResponse> {
    return this.http.post<StripeCheckoutResponse>(`${this.apiUrl}/stripe`, payload);
  }
}
