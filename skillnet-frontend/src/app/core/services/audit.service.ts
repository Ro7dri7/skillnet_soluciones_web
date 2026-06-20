import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AuditLogEntry {
  id: number;
  action: string;
  entityName: string;
  entityId?: number | null;
  userEmail?: string | null;
  userDisplayName?: string | null;
  ipAddress?: string | null;
  userAgent?: string | null;
  details?: string | null;
  timestamp: string;
}

export interface AuditLogPage {
  content: AuditLogEntry[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface AuditLogFilters {
  email?: string;
  action?: string;
  startDate?: string;
  endDate?: string;
}

@Injectable({ providedIn: 'root' })
export class AuditService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/admin/audit-logs`;

  getAuditLogs(page = 0, size = 50, filters: AuditLogFilters = {}): Observable<AuditLogPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    params = this.appendFilters(params, filters);
    return this.http.get<AuditLogPage>(this.baseUrl, { params });
  }

  exportAuditLogsToCsv(filters: AuditLogFilters = {}): Observable<Blob> {
    let params = this.appendFilters(new HttpParams(), filters);
    return this.http.get(`${this.baseUrl}/export`, {
      params,
      responseType: 'blob',
    });
  }

  private appendFilters(params: HttpParams, filters: AuditLogFilters): HttpParams {
    if (filters.email?.trim()) {
      params = params.set('email', filters.email.trim());
    }
    if (filters.action?.trim()) {
      params = params.set('action', filters.action.trim());
    }
    if (filters.startDate?.trim()) {
      params = params.set('startDate', filters.startDate.trim());
    }
    if (filters.endDate?.trim()) {
      params = params.set('endDate', filters.endDate.trim());
    }
    return params;
  }
}
