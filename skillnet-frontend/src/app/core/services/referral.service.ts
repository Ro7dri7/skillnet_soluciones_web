import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ReferralLink {
  courseId: number;
  token: string;
  url: string;
}

@Injectable({ providedIn: 'root' })
export class ReferralService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/producer/courses`;

  getReferralLink(courseId: number): Observable<ReferralLink> {
    return this.http.get<ReferralLink>(`${this.baseUrl}/${courseId}/referral-link`);
  }
}
