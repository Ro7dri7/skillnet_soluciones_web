import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TrafficSeriesPoint {
  date: string;
  value: number;
}

export interface TrafficAnalytics {
  configured: boolean;
  pageViews: TrafficSeriesPoint[];
  uniqueVisitors: TrafficSeriesPoint[];
}

@Injectable({ providedIn: 'root' })
export class TrafficService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/producer/traffic-analytics`;

  getAnalytics(): Observable<TrafficAnalytics> {
    return this.http.get<TrafficAnalytics>(this.baseUrl);
  }
}
