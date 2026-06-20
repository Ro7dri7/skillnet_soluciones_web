import { HttpClient, HttpParams } from '@angular/common/http';

import { Injectable, inject } from '@angular/core';

import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';



export interface StudentProgressItem {

  enrollmentId: number;

  userId: number;

  userName: string;

  userEmail: string;

  courseId: number;

  courseTitle: string;

  enrolledAt?: string;

  completed: boolean;

  completedAt?: string;

  completedLessons: number;

  totalLessons: number;

  progressPercent: number;

}



export interface QuizProgressItem {

  lessonId?: number;

  quizId?: number;

  submissionId?: number;

  title: string;

  status: string;

  color: string;

  score?: number;

}



export interface StudentProgressDetail {

  enrollmentId: number;

  userId: number;

  userName: string;

  userEmail: string;

  profilePictureUrl?: string | null;

  enrolledAt?: string;

  completed: boolean;

  completedLessons: number;

  totalLessons: number;

  progressPercent: number;

  quizzes: QuizProgressItem[];

}



export interface CourseProgressGroup {

  id: number;

  slug?: string;

  title: string;

  status?: string;

  imageUrl?: string | null;

  totalStudents: number;

  students: StudentProgressDetail[];

}



export interface ProducerStudentProgressOverview {

  totalCourses: number;

  totalStudents: number;

  courses: CourseProgressGroup[];

}



@Injectable({ providedIn: 'root' })

export class ProducerStudentsService {

  private readonly http = inject(HttpClient);

  private readonly baseUrl = `${environment.apiUrl}/producer/student-progress`;



  studentProgress(courseId?: number): Observable<StudentProgressItem[]> {

    let params = new HttpParams();

    if (courseId != null) {

      params = params.set('courseId', String(courseId));

    }

    return this.http.get<StudentProgressItem[]>(this.baseUrl, { params });

  }



  overview(courseSlug?: string): Observable<ProducerStudentProgressOverview> {

    let params = new HttpParams();

    if (courseSlug) {

      params = params.set('course', courseSlug);

    }

    return this.http.get<ProducerStudentProgressOverview>(`${this.baseUrl}/overview`, { params });

  }

}


