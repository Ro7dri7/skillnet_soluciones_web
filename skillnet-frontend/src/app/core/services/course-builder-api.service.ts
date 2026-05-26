import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CourseBuilderRequest,
  CourseBuilderResponse,
} from '../../shared/models/course-builder.model';

@Injectable({ providedIn: 'root' })
export class CourseBuilderApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/courses/builder`;

  saveDraft(payload: CourseBuilderRequest): Observable<CourseBuilderResponse> {
    return this.http.post<CourseBuilderResponse>(this.baseUrl, payload);
  }

  getByCourseId(courseId: number): Observable<CourseBuilderResponse> {
    return this.http.get<CourseBuilderResponse>(`${this.baseUrl}/${courseId}`);
  }
}
