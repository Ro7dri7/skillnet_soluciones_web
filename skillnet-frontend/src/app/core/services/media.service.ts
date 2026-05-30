import { HttpClient, HttpEvent, HttpEventType, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, filter, map } from 'rxjs';
import { environment } from '../../../environments/environment';

export type MediaUploadType = 'thumbnail' | 'pdf' | 'video' | 'resource';

export type LessonResourceType = 'pdf' | 'video' | 'image' | 'audio';

export interface MediaUploadResponse {
  url: string;
  storageKey?: string;
  type?: string;
}

@Injectable({ providedIn: 'root' })
export class MediaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/producer/media`;

  uploadFile(
    courseId: number,
    file: File,
    type: MediaUploadType,
  ): Observable<{ event: HttpEvent<MediaUploadResponse>; progress: number }> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    formData.append('type', type);
    formData.append('courseId', String(courseId));

    return this.http
      .post<MediaUploadResponse>(`${this.baseUrl}/upload`, formData, {
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

  uploadAndWait(courseId: number, file: File, type: MediaUploadType): Observable<MediaUploadResponse> {
    return this.uploadFile(courseId, file, type).pipe(
      filter((item) => item.event.type === HttpEventType.Response),
      map((item) => (item.event as HttpResponse<MediaUploadResponse>).body!),
    );
  }

  /** Sube un recurso de lección (PDF, imagen, audio o vídeo) al bucket del curso. */
  uploadLessonResource(
    courseId: number,
    file: File,
    _contentType: LessonResourceType,
  ): Observable<MediaUploadResponse> {
    return this.uploadAndWait(courseId, file, 'resource');
  }
}
