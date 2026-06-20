import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CertificateItem {
  id: number;
  courseId: number;
  courseTitle: string;
  courseSlug?: string;
  certificateFile?: string;
  uploadedAt?: string;
}

export interface CertificateOverview {
  totalCertificates: number;
  certificates: CertificateItem[];
}

export interface CertificateCheck {
  eligible: boolean;
  hasCertificate?: boolean;
  courseId: number;
  message?: string;
  certificate?: CertificateItem;
}

@Injectable({ providedIn: 'root' })
export class CertificateService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/student/certificates`;

  overview(): Observable<CertificateOverview> {
    return this.http.get<CertificateOverview>(this.baseUrl);
  }

  check(courseId: number): Observable<CertificateCheck> {
    return this.http.get<CertificateCheck>(`${this.baseUrl}/courses/${courseId}/check`);
  }
}
