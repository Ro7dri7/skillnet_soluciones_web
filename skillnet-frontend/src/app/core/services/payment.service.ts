import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PaymentItem {
  id: number;
  amount: number;
  status: string;
  paymentMethod?: string;
  courseId?: number;
  courseTitle?: string;
  createdAt?: string;
}

export interface PaymentStatus {
  id: number;
  status: string;
  courseId?: number;
  courseTitle?: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/student/payments`;

  myPayments(): Observable<PaymentItem[]> {
    return this.http.get<PaymentItem[]>(this.baseUrl);
  }

  paymentStatus(paymentId: number): Observable<PaymentStatus> {
    return this.http.get<PaymentStatus>(`${this.baseUrl}/${paymentId}/status`);
  }
}
