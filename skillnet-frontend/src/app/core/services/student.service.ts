import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ContentBlockDTO, QuizData } from '../../shared/models/curriculum.model';
import { studentCourseApiPath } from '../../shared/utils/course-slug.util';

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
  quizData?: QuizData;
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
  progressPercentage?: number;
  completedLessonIds?: number[];
}

export interface LessonProgressResult {
  lessonId: number;
  completed: boolean;
  progressPercentage: number;
  courseCompleted: boolean;
}

@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly http = inject(HttpClient);

  getMyCourses(): Observable<MyCourse[]> {
    return this.http.get<MyCourse[]>(`${environment.apiUrl}/student/my-learning`);
  }

  getLearnPage(slug: string, courseFormat?: string | null): Observable<CourseLearnPage> {
    const path = studentCourseApiPath(slug, courseFormat);
    return this.http.get<CourseLearnPage>(`${environment.apiUrl}/student/courses/${path}/learn`);
  }

  markLessonComplete(
    slug: string,
    lessonId: number,
    courseFormat?: string | null,
  ): Observable<LessonProgressResult> {
    const path = studentCourseApiPath(slug, courseFormat);
    return this.http.post<LessonProgressResult>(
      `${environment.apiUrl}/student/courses/${path}/lessons/${lessonId}/progress`,
      { completed: true },
    );
  }
}
