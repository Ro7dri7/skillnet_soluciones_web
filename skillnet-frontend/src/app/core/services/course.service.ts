import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CourseApiPayload,
  CourseRequest,
  CourseResponse,
} from '../../shared/models/course.model';

interface CourseApiResponse {
  id: number;
  title: string;
  slug: string;
  description: string;
  level: string;
  language: string;
  status: string;
  price: number | string;
  professorId: number | null;
  imageUrl?: string | null;
  videoUrl?: string | null;
  category?: string | null;
  subcategory?: string | null;
  whatYouWillLearn?: string | null;
  targetAudience?: string | null;
  currency?: string | null;
  originalPrice?: number | string | null;
  onSale?: boolean;
  affiliateCommission?: number | string | null;
  affiliatePolicy?: string | null;
  welcomeMessage?: string | null;
  congratulationsMessage?: string | null;
}

@Injectable({ providedIn: 'root' })
export class CourseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/courses`;

  getCourses(): Observable<CourseResponse[]> {
    return this.http
      .get<CourseApiResponse[]>(this.baseUrl)
      .pipe(map((items) => items.map((item) => this.toCourseResponse(item))));
  }

  getCourse(id: number): Observable<CourseResponse> {
    return this.http
      .get<CourseApiResponse>(`${this.baseUrl}/${id}`)
      .pipe(map((item) => this.toCourseResponse(item)));
  }

  getCourseBySlug(slug: string): Observable<CourseResponse> {
    return this.http
      .get<CourseApiResponse>(`${this.baseUrl}/by-slug/${encodeURIComponent(slug)}`)
      .pipe(map((item) => this.toCourseResponse(item)));
  }

  createCourse(course: CourseRequest, professorId?: number): Observable<CourseResponse> {
    return this.http
      .post<CourseApiResponse>(this.baseUrl, this.toApiPayload(course, professorId))
      .pipe(map((item) => this.toCourseResponse(item)));
  }

  updateCourse(
    id: number,
    course: CourseRequest,
    professorId?: number,
  ): Observable<CourseResponse> {
    return this.http
      .put<CourseApiResponse>(`${this.baseUrl}/${id}`, this.toApiPayload(course, professorId))
      .pipe(map((item) => this.toCourseResponse(item)));
  }

  deleteCourse(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  private toApiPayload(course: CourseRequest, professorId?: number): CourseApiPayload {
    return {
      title: course.title,
      slug: course.slug,
      description: course.description ?? '',
      level: course.level,
      status: course.status,
      price: course.price,
      originalPrice: course.price,
      language: 'es',
      courseFormat: 'video',
      currency: 'USD',
      affiliateCommission: 0,
      affiliatePolicy: 'none',
      ally: 'skillnet',
      securityStatus: 'pending',
      ...(professorId != null ? { professorId } : {}),
    };
  }

  private toCourseResponse(item: CourseApiResponse): CourseResponse {
    return {
      id: item.id,
      title: item.title,
      slug: item.slug,
      description: item.description ?? '',
      level: item.level ?? 'beginner',
      language: item.language ?? 'es',
      status: item.status,
      price: typeof item.price === 'string' ? parseFloat(item.price) : item.price,
      professorId: item.professorId,
      imageUrl: item.imageUrl ?? null,
      videoUrl: item.videoUrl ?? null,
      category: item.category ?? null,
      subcategory: item.subcategory ?? null,
      whatYouWillLearn: item.whatYouWillLearn ?? null,
      targetAudience: item.targetAudience ?? null,
      currency: item.currency ?? 'USD',
      originalPrice: this.toNumber(item.originalPrice ?? item.price),
      onSale: item.onSale ?? false,
      affiliateCommission: this.toNumber(item.affiliateCommission ?? 0),
      affiliatePolicy: item.affiliatePolicy ?? 'all',
      welcomeMessage: item.welcomeMessage ?? null,
      congratulationsMessage: item.congratulationsMessage ?? null,
    };
  }

  private toNumber(value: number | string | null | undefined): number {
    if (value == null) {
      return 0;
    }
    return typeof value === 'string' ? parseFloat(value) : value;
  }

  getCourseWithAudience(id: number): Observable<CourseResponse> {
    return this.getCourse(id);
  }
}
