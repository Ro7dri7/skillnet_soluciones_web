import { HttpClient } from '@angular/common/http';

import { Injectable, inject } from '@angular/core';

import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';



export interface QuizAnswerReview {

  id: number;

  questionId?: number;

  questionText: string;

  questionType: string;

  textAnswer?: string;

  correct?: boolean | null;

  tutorFeedback?: string;

  requiresManualGrading: boolean;

}



export interface QuizSubmissionReview {

  id: number;

  quizId?: number;

  quizTitle?: string;

  courseId?: number;

  courseTitle?: string;

  studentId?: number;

  studentName?: string;

  score: number;

  passingScore?: number;

  reviewStatus?: string;

  tutorFeedback?: string;

  timeTakenSeconds?: number;

  createdAt?: string;

  answers?: QuizAnswerReview[];

}



export interface QuizReviewPayload {

  feedback?: string;

  approved: boolean;

}



export interface GradeQuizAnswerPayload {

  answerId: number;

  correct: boolean;

  feedback?: string;

}



export interface GradeQuizAnswerResult {

  answerId: number;

  correct?: boolean;

  newScore: number;

  reviewStatus?: string;

  message?: string;

}



@Injectable({ providedIn: 'root' })

export class ProducerQuizReviewService {

  private readonly http = inject(HttpClient);

  private readonly baseUrl = `${environment.apiUrl}/producer/quiz-submissions`;



  list(): Observable<QuizSubmissionReview[]> {

    return this.http.get<QuizSubmissionReview[]>(this.baseUrl);

  }



  get(id: number): Observable<QuizSubmissionReview> {

    return this.http.get<QuizSubmissionReview>(`${this.baseUrl}/${id}`);

  }



  review(id: number, payload: QuizReviewPayload): Observable<QuizSubmissionReview> {

    return this.http.post<QuizSubmissionReview>(`${this.baseUrl}/${id}/review`, payload);

  }



  gradeAnswer(id: number, payload: GradeQuizAnswerPayload): Observable<GradeQuizAnswerResult> {

    return this.http.post<GradeQuizAnswerResult>(`${this.baseUrl}/${id}/grade-answer`, payload);

  }

}


