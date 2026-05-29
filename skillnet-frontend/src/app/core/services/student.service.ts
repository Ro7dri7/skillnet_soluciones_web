import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MyCourse {
  courseId: number;
  title: string;
  slug: string;
  thumbnailUrl: string | null;
  authorName: string;
  progressPercentage: number;
  enrolledAt: string;
}

@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly http = inject(HttpClient);

  getMyCourses(): Observable<MyCourse[]> {
    return this.http.get<MyCourse[]>(`${environment.apiUrl}/student/my-learning`);
  }
}
