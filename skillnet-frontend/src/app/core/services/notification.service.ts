import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AppNotification {
  id: number;
  notificationType?: string;
  title: string;
  message: string;
  read: boolean;
  createdAt?: string;
  link?: string;
}

export interface NotificationCount {
  unreadCount: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/notifications`;

  list(): Observable<AppNotification[]> {
    return this.http.get<AppNotification[]>(this.baseUrl);
  }

  count(): Observable<NotificationCount> {
    return this.http.get<NotificationCount>(`${this.baseUrl}/count`);
  }

  markRead(id: number): Observable<AppNotification> {
    return this.http.post<AppNotification>(`${this.baseUrl}/${id}/read`, null);
  }

  markAllRead(): Observable<{ updated: number }> {
    return this.http.post<{ updated: number }>(`${this.baseUrl}/read-all`, null);
  }
}
