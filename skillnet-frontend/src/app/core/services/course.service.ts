import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { EMPTY, Observable, catchError, concatMap, first, from, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CourseApiPayload,
  CourseRequest,
  CourseResponse,
} from '../../shared/models/course.model';
import { courseSlugLookupCandidates, parseCourseSlug } from '../../shared/utils/course-slug.util';

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
  professor?: {
    id: number;
    username: string;
    email?: string;
    firstName?: string;
    lastName?: string;
  } | null;
  imageUrl?: string | null;
  videoUrl?: string | null;
  category?: string | null;
  subcategory?: string | null;
  whatYouWillLearn?: string | null;
  targetAudience?: string | null;
  requirements?: string | null;
  durationHours?: number;
  durationMinutes?: number;
  currency?: string | null;
  originalPrice?: number | string | null;
  onSale?: boolean;
  affiliateCommission?: number | string | null;
  affiliatePolicy?: string | null;
  welcomeMessage?: string | null;
  congratulationsMessage?: string | null;
  courseFormat?: string | null;
  moduleCount?: number;
  lessonsCount?: number;
  enrollmentCount?: number;
  sections?: {
    id: number;
    title: string;
    lessons?: { id: number; title: string }[];
  }[];
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

  getCourseBySlug(slug: string, courseFormat?: string | null): Observable<CourseResponse> {
    const decoded = decodeURIComponent(slug);
    const parsed = parseCourseSlug(decoded, courseFormat);
    const candidates = courseSlugLookupCandidates(parsed.full || decoded);

    return from(candidates).pipe(
      concatMap((candidate) =>
        this.fetchCourseBySlugCandidate(candidate).pipe(
          catchError(() => EMPTY),
          map((item) => this.toCourseResponse(item)),
        ),
      ),
      first(),
    );
  }

  private fetchCourseBySlugCandidate(candidate: string): Observable<CourseApiResponse> {
    if (candidate.includes('/')) {
      return this.http.get<CourseApiResponse>(`${this.baseUrl}/by-slug`, {
        params: { slug: candidate },
      });
    }

    return this.http.get<CourseApiResponse>(`${this.baseUrl}/by-slug/${candidate}`);
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

  getCoursesByProfessor(professorId: number): Observable<CourseResponse[]> {
    return this.getCourses().pipe(
      map((items) => items.filter((c) => c.professorId === professorId)),
    );
  }

  getCoursesByCategory(category: string): Observable<CourseResponse[]> {
    const normalized = category.trim().toLowerCase();
    return this.getCourses().pipe(
      map((items) =>
        items.filter((c) => (c.category ?? '').trim().toLowerCase() === normalized),
      ),
    );
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
      professor: item.professor ?? null,
      imageUrl: item.imageUrl ?? null,
      videoUrl: item.videoUrl ?? null,
      category: item.category ?? null,
      subcategory: item.subcategory ?? null,
      whatYouWillLearn: item.whatYouWillLearn ?? null,
      targetAudience: item.targetAudience ?? null,
      requirements: item.requirements ?? null,
      durationHours: item.durationHours ?? 0,
      durationMinutes: item.durationMinutes ?? 0,
      currency: item.currency ?? 'USD',
      originalPrice: this.toNumber(item.originalPrice ?? item.price),
      onSale: item.onSale ?? false,
      affiliateCommission: this.toNumber(item.affiliateCommission ?? 0),
      affiliatePolicy: item.affiliatePolicy ?? 'all',
      welcomeMessage: item.welcomeMessage ?? null,
      congratulationsMessage: item.congratulationsMessage ?? null,
      courseFormat: item.courseFormat ?? null,
      moduleCount: item.moduleCount ?? 0,
      lessonsCount: item.lessonsCount ?? 0,
      enrollmentCount: item.enrollmentCount ?? 0,
      sections: (item.sections ?? []).map((section) => ({
        id: section.id,
        title: section.title,
        lessons: (section.lessons ?? []).map((lesson) => ({
          id: lesson.id,
          title: lesson.title,
        })),
      })),
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
