import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CourseBasicsResponse,
  CourseCouponResponse,
  CourseMessagesResponse,
  CoursePricingResponse,
  CreateCourseCouponRequest,
  CreateCourseRequest,
  ProducerCourseSummary,
  UpdateCourseBasicsRequest,
  UpdateCourseMessagesRequest,
  UpdateCoursePricingRequest,
} from '../../shared/models/producer-course.model';

@Injectable({ providedIn: 'root' })
export class ProducerCoursesService {
  private readonly http = inject(HttpClient);
  private readonly producerBaseUrl = `${environment.apiUrl}/producer/courses`;
  private readonly coursesBaseUrl = `${environment.apiUrl}/courses`;

  createDraft(payload: CreateCourseRequest): Observable<ProducerCourseSummary> {
    return this.http.post<ProducerCourseSummary>(this.producerBaseUrl, payload);
  }

  getMyCourses(): Observable<ProducerCourseSummary[]> {
    return this.http.get<ProducerCourseSummary[]>(this.producerBaseUrl);
  }

  publishCourse(courseId: number): Observable<ProducerCourseSummary> {
    return this.http.put<ProducerCourseSummary>(`${this.coursesBaseUrl}/${courseId}/publish`, {});
  }

  unpublishCourse(courseId: number): Observable<ProducerCourseSummary> {
    return this.http.put<ProducerCourseSummary>(`${this.coursesBaseUrl}/${courseId}/draft`, {});
  }

  updateBasics(courseId: number, payload: UpdateCourseBasicsRequest): Observable<CourseBasicsResponse> {
    return this.http.put<CourseBasicsResponse>(
      `${this.producerBaseUrl}/${courseId}/basics`,
      payload,
    );
  }

  updatePricing(courseId: number, payload: UpdateCoursePricingRequest): Observable<CoursePricingResponse> {
    return this.http.put<CoursePricingResponse>(
      `${this.producerBaseUrl}/${courseId}/pricing`,
      payload,
    );
  }

  updateMessages(courseId: number, payload: UpdateCourseMessagesRequest): Observable<CourseMessagesResponse> {
    return this.http.put<CourseMessagesResponse>(
      `${this.producerBaseUrl}/${courseId}/messages`,
      payload,
    );
  }

  listCoupons(courseId: number): Observable<CourseCouponResponse[]> {
    return this.http.get<CourseCouponResponse[]>(`${this.producerBaseUrl}/${courseId}/coupons`);
  }

  createCoupon(courseId: number, payload: CreateCourseCouponRequest): Observable<CourseCouponResponse> {
    return this.http.post<CourseCouponResponse>(
      `${this.producerBaseUrl}/${courseId}/coupons`,
      payload,
    );
  }

  deleteCoupon(courseId: number, couponId: number): Observable<void> {
    return this.http.delete<void>(`${this.producerBaseUrl}/${courseId}/coupons/${couponId}`);
  }
}
