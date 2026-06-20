import { Injectable, inject } from '@angular/core';

import { HttpClient } from '@angular/common/http';

import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';



export interface StripeCheckoutRequest {

  courseId: number;

  courseIds?: number[];

  amount: number;

  paymentToken: string;

  couponCode?: string;

}



export interface CheckoutQuoteRequest {

  courseIds: number[];

  couponCode?: string;

}



export interface CheckoutQuoteLine {

  courseId: number;

  title: string;

  baseAmount: number;

  totalAmount: number;

}



export interface CheckoutQuoteResponse {

  subtotal: number;

  discount: number;

  total: number;

  couponCode?: string | null;

  couponPercentOff?: number | null;

  couponAmountOff?: number | null;

  couponLabel?: string | null;

  couponValid: boolean;

  message?: string | null;

  lines: CheckoutQuoteLine[];

}



export interface StripeCheckoutResponse {

  message: string;

  status: string;

  paymentId?: number;

}



@Injectable({ providedIn: 'root' })

export class CheckoutService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${environment.apiUrl}/checkout`;



  quote(payload: CheckoutQuoteRequest): Observable<CheckoutQuoteResponse> {

    return this.http.post<CheckoutQuoteResponse>(`${this.apiUrl}/quote`, payload);

  }



  processPayment(payload: StripeCheckoutRequest): Observable<StripeCheckoutResponse> {

    return this.http.post<StripeCheckoutResponse>(`${this.apiUrl}/stripe`, payload);

  }

}

