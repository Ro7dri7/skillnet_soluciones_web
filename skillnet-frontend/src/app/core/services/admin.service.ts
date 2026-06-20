import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CourseResponse } from '../../shared/models/course.model';
import { User } from '../../shared/models/auth.model';

export interface AdminKpi {
  id: string;
  label: string;
  value: string;
  changePercent: string;
  changeDirection: 'up' | 'down' | 'neutral';
  meta: string;
  accentColor: string;
  icon: string;
}

export interface AdminChartPoint {
  label: string;
  current: number;
  previous: number;
}

export interface AdminTransaction {
  id: number;
  buyerName: string;
  buyerInitials: string;
  courseTitle: string;
  amount: number;
  status: string;
}

export interface AdminTopProducer {
  id: number;
  name: string;
  category: string;
  revenue: number;
}

export interface AdminDashboardData {
  periodLabel: string;
  periodStart: string;
  periodEnd: string;
  view: string;
  kpis: AdminKpi[];
  revenueSeries: AdminChartPoint[];
  usersSeries: AdminChartPoint[];
  recentTransactions: AdminTransaction[];
  topProducers: AdminTopProducer[];
  pendingDraftCourses: number;
  inactiveUsersTotal: number;
}

export type AdminPeriod = 'dia' | 'semana' | 'mes' | 'anio' | 'personalizado';
export type AdminView = 'resumen' | 'ventas' | 'cursos' | 'usuarios' | 'titulados';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/admin`;

  getDashboard(
    period: AdminPeriod,
    view: AdminView,
    startDate?: string,
    endDate?: string,
  ): Observable<AdminDashboardData> {
    let params = new HttpParams().set('period', period).set('view', view);
    if (period === 'personalizado' && startDate && endDate) {
      params = params.set('startDate', startDate).set('endDate', endDate);
    }
    return this.http.get<AdminDashboardData>(`${this.baseUrl}/dashboard`, { params });
  }

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${environment.apiUrl}/users`);
  }

  getCourses(): Observable<CourseResponse[]> {
    return this.http.get<CourseResponse[]>(`${this.baseUrl}/courses`);
  }

  publishCourse(courseId: number): Observable<CourseResponse> {
    return this.http.put<CourseResponse>(`${this.baseUrl}/courses/${courseId}/publish`, null);
  }

  setCourseDraft(courseId: number): Observable<CourseResponse> {
    return this.http.put<CourseResponse>(`${this.baseUrl}/courses/${courseId}/draft`, null);
  }

  takedownCourse(courseId: number): Observable<CourseResponse> {
    return this.http.put<CourseResponse>(`${this.baseUrl}/courses/${courseId}/takedown`, null);
  }

  updateUserRole(userId: number, role: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/users/${userId}/role`, { role });
  }

  getEnrollments(): Observable<AdminEnrollment[]> {
    return this.http.get<AdminEnrollment[]>(`${this.baseUrl}/enrollments`);
  }

  createEnrollment(payload: AdminEnrollmentCreatePayload): Observable<AdminEnrollment> {
    return this.http.post<AdminEnrollment>(`${this.baseUrl}/enrollments`, payload);
  }

  getServiceOfferings(): Observable<ServiceOffering[]> {
    return this.http.get<ServiceOffering[]>(`${environment.apiUrl}/admin/service-offerings`);
  }

  getServiceOffering(id: number): Observable<ServiceOffering> {
    return this.http.get<ServiceOffering>(`${environment.apiUrl}/admin/service-offerings/${id}`);
  }

  createServiceOffering(payload: ServiceOfferingPayload): Observable<ServiceOffering> {
    return this.http.post<ServiceOffering>(
      `${environment.apiUrl}/admin/service-offerings`,
      payload,
    );
  }

  updateServiceOffering(id: number, payload: ServiceOfferingPayload): Observable<ServiceOffering> {
    return this.http.put<ServiceOffering>(
      `${environment.apiUrl}/admin/service-offerings/${id}`,
      payload,
    );
  }

  deleteServiceOffering(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/admin/service-offerings/${id}`);
  }
}

export interface AdminEnrollment {
  id: number;
  userId: number;
  courseId: number;
  enrollmentType?: string;
  enrolledAt?: string;
  user?: { id: number; username: string; email?: string };
  course?: { id: number; title: string; slug?: string };
}

export interface AdminEnrollmentCreatePayload {
  userId: number;
  courseId: number;
  enrollmentType?: string;
}

export interface ServiceOffering {
  id: number;
  section: string;
  title: string;
  description?: string;
  priceUsd: number;
  iconClass?: string;
  sortOrder?: number;
  active?: boolean;
  featured?: boolean;
}

export interface ServiceOfferingPayload {
  section: string;
  title: string;
  description?: string;
  priceUsd: number;
  iconClass?: string;
  sortOrder?: number;
  active?: boolean;
  featured?: boolean;
}
