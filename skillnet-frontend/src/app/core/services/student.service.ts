import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ContentBlockDTO } from '../../shared/models/curriculum.model';

export interface MyCourse {
  courseId: number;
  title: string;
  slug: string;
  thumbnailUrl: string | null;
  authorName: string;
  progressPercentage: number;
  enrolledAt: string;
}

export interface LearnLesson {
  id: number;
  title: string;
  contentType: string;
  resourceUrl: string;
  textContent: string;
  orderIndex: number;
  blocks?: ContentBlockDTO[];
}

export interface LearnModule {
  id: number;
  title: string;
  orderIndex: number;
  lessons: LearnLesson[];
}

export interface CourseLearnPage {
  courseId: number;
  title: string;
  slug: string;
  welcomeMessage: string | null;
  congratulationsMessage: string | null;
  modules: LearnModule[];
}

@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly http = inject(HttpClient);

  getMyCourses(): Observable<MyCourse[]> {
    return this.http.get<MyCourse[]>(`${environment.apiUrl}/student/my-learning`);
  }

  getLearnPage(slug: string): Observable<CourseLearnPage> {
    return this.http.get<CourseLearnPage>(
      `${environment.apiUrl}/student/courses/${encodeURIComponent(slug)}/learn`,
    );
  }
}
