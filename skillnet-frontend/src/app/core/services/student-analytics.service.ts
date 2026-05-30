import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { StudentAnalyticsResponse } from '../../shared/models/analytics.model';

@Injectable({ providedIn: 'root' })
export class StudentAnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/student/analytics`;

  getAnalytics(year?: number, month?: number): Observable<StudentAnalyticsResponse> {
    let params = new HttpParams();
    if (year != null && Number.isFinite(year)) {
      params = params.set('year', String(year));
    }
    if (month != null && Number.isFinite(month) && month >= 1 && month <= 12) {
      params = params.set('month', String(month));
    }
    return this.http.get<StudentAnalyticsResponse>(this.baseUrl, { params });
  }
}
