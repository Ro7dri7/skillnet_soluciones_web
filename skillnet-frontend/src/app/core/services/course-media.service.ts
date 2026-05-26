import { HttpClient, HttpEvent, HttpEventType, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, filter, map } from 'rxjs';
import { environment } from '../../../environments/environment';

export type CourseMediaKind = 'cover' | 'promo_video';

export interface CourseMediaUploadResponse {
  kind: string;
  storageKey: string;
  publicUrl: string;
  imageUrl: string | null;
  videoUrl: string | null;
}

@Injectable({ providedIn: 'root' })
export class CourseMediaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/producer/courses`;

  upload(
    courseId: number,
    kind: CourseMediaKind,
    file: File,
  ): Observable<{ event: HttpEvent<CourseMediaUploadResponse>; progress: number }> {
    const formData = new FormData();
    formData.append('kind', kind);
    formData.append('file', file, file.name);

    return this.http
      .post<CourseMediaUploadResponse>(`${this.baseUrl}/${courseId}/media`, formData, {
        reportProgress: true,
        observe: 'events',
      })
      .pipe(
        map((event) => {
          let progress = 0;
          if (event.type === HttpEventType.UploadProgress && event.total) {
            progress = Math.round((100 * event.loaded) / event.total);
          }
          return { event, progress };
        }),
      );
  }

  uploadAndWait(courseId: number, kind: CourseMediaKind, file: File): Observable<CourseMediaUploadResponse> {
    return this.upload(courseId, kind, file).pipe(
      filter((item) => item.event.type === HttpEventType.Response),
      map((item) => (item.event as HttpResponse<CourseMediaUploadResponse>).body!),
    );
  }
}
