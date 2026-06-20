import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { parseCourseSlug } from '../../shared/utils/course-slug.util';

export interface CourseReview {
  id: number;
  courseId: number;
  userId: number;
  user?: { id: number; username: string; firstName?: string; lastName?: string };
  rating: number;
  comment?: string;
  helpfulCount?: number;
  createdAt?: string;
}

export interface CreateReviewPayload {
  rating: number;
  comment?: string;
}

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly http = inject(HttpClient);

  getReviews(slug: string, courseFormat?: string | null): Observable<CourseReview[]> {
    return this.http.get<CourseReview[]>(this.reviewsUrl(slug, courseFormat));
  }

  addReview(
    slug: string,
    payload: CreateReviewPayload,
    courseFormat?: string | null,
  ): Observable<CourseReview> {
    return this.http.post<CourseReview>(this.reviewsUrl(slug, courseFormat), payload);
  }

  private reviewsUrl(slug: string, courseFormat?: string | null): string {
    const base = `${environment.apiUrl}/courses/by-slug`;
    const parsed = parseCourseSlug(slug, courseFormat);
    if (parsed.format) {
      return `${base}/${encodeURIComponent(parsed.format)}/${encodeURIComponent(parsed.slug)}/reviews`;
    }
    if (slug.includes('/')) {
      return `${base}/reviews?slug=${encodeURIComponent(slug)}`;
    }
    return `${base}/${encodeURIComponent(parsed.slug)}/reviews`;
  }
}
