import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { AuthResponse } from '../../shared/models/auth.model';

export interface TwoFactorStatus {
  enabled: boolean;
  method: string;
  passwordRequiredForDisable?: boolean;
}

export interface TwoFactorEnableResponse {
  method: string;
  secret: string;
  qrCode: string;
}

export interface TwoFactorVerifyLoginRequest {
  twoFactorToken: string;
  code: string;
}

export interface TwoFactorDisableRequest {
  password: string;
  code: string;
}

@Injectable({ providedIn: 'root' })
export class TwoFactorService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/auth/2fa`;

  status(): Observable<TwoFactorStatus> {
    return this.http.get<TwoFactorStatus>(`${this.base}/status`);
  }

  enable(): Observable<TwoFactorEnableResponse> {
    return this.http.post<TwoFactorEnableResponse>(`${this.base}/enable`, {});
  }

  verifySetup(code: string): Observable<void> {
    return this.http.post<void>(`${this.base}/verify`, { code });
  }

  disable(payload: TwoFactorDisableRequest): Observable<void> {
    return this.http.post<void>(`${this.base}/disable`, payload);
  }

  verifyLogin(payload: TwoFactorVerifyLoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/verify-login`, payload);
  }
}
