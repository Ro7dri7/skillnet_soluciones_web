import { HttpClient } from '@angular/common/http';

import { Injectable, inject } from '@angular/core';

import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';



export interface QuizAnswerPayload {

  questionId: string;

  optionIndex?: number;

  textAnswer?: string;

  matchingAnswers?: string[];

}



export interface QuizSubmitPayload {

  score: number;

  timeTakenSeconds?: number;

  needsManualReview?: boolean;

  answers?: QuizAnswerPayload[];

}



export interface QuizSubmissionResult {

  id: number;

  quizId?: number;

  lessonId?: number;

  score: number;

  reviewStatus?: string;

  timeTakenSeconds?: number;

  createdAt?: string;

}



export interface QuizAttempts {

  quizId?: number;

  attemptsUsed: number;

  maxAttempts: number;

  attemptsRemaining: number;

  canSubmit: boolean;

}



@Injectable({ providedIn: 'root' })

export class QuizSubmissionService {

  private readonly http = inject(HttpClient);

  private readonly baseUrl = `${environment.apiUrl}/student/lessons`;



  submit(lessonId: number, payload: QuizSubmitPayload): Observable<QuizSubmissionResult> {

    return this.http.post<QuizSubmissionResult>(

      `${this.baseUrl}/${lessonId}/quiz-submissions`,

      payload,

    );

  }



  checkAttempts(lessonId: number): Observable<QuizAttempts> {

    return this.http.get<QuizAttempts>(`${this.baseUrl}/${lessonId}/quiz-submissions`);

  }

}


