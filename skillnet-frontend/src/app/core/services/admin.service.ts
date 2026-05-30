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
    return this.http.get<CourseResponse[]>(`${environment.apiUrl}/courses`);
  }
}
