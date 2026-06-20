import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PasswordResetService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auth/password-reset`;

  request(email: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/request`, { email });
  }

  confirm(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/confirm`, { token, newPassword });
  }
}
