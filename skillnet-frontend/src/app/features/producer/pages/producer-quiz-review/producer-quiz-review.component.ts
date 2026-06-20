import { DatePipe } from '@angular/common';

import { Component, inject, OnInit, signal } from '@angular/core';

import { FormsModule } from '@angular/forms';

import { ActivatedRoute, Router } from '@angular/router';

import {

  ProducerQuizReviewService,

  QuizAnswerReview,

  QuizSubmissionReview,

} from '../../../../core/services/producer-quiz-review.service';

import { messageFromHttpError } from '../../../../shared/utils/http-error.util';



@Component({

  selector: 'app-producer-quiz-review',

  standalone: true,

  imports: [DatePipe, FormsModule],

  templateUrl: './producer-quiz-review.component.html',

  styleUrl: './producer-quiz-review.component.scss',

})

export class ProducerQuizReviewComponent implements OnInit {

  private readonly quizReviewService = inject(ProducerQuizReviewService);

  private readonly route = inject(ActivatedRoute);

  private readonly router = inject(Router);



  readonly loading = signal(true);

  readonly error = signal<string | null>(null);

  readonly rows = signal<QuizSubmissionReview[]>([]);

  readonly detail = signal<QuizSubmissionReview | null>(null);

  readonly feedback = signal('');

  readonly reviewing = signal(false);

  readonly gradingAnswerId = signal<number | null>(null);



  ngOnInit(): void {

    const submissionId = this.route.snapshot.queryParamMap.get('submissionId');

    if (submissionId) {

      this.loadDetail(Number(submissionId));

      return;

    }

    this.loadList();

  }



  loadList(): void {

    this.loading.set(true);

    this.quizReviewService.list().subscribe({

      next: (data) => {

        this.rows.set(data);

        this.loading.set(false);

      },

      error: (err) => {

        this.error.set(messageFromHttpError(err, 'No se pudieron cargar las entregas.'));

        this.loading.set(false);

      },

    });

  }



  loadDetail(id: number): void {

    this.loading.set(true);

    this.quizReviewService.get(id).subscribe({

      next: (data) => {

        this.detail.set(data);

        this.feedback.set(data.tutorFeedback ?? '');

        this.loading.set(false);

      },

      error: (err) => {

        this.error.set(messageFromHttpError(err, 'No se pudo cargar la entrega.'));

        this.loading.set(false);

      },

    });

  }



  openDetail(row: QuizSubmissionReview): void {

    void this.router.navigate(['/infoproductor/quiz-review'], {

      queryParams: { submissionId: row.id },

    });

    this.loadDetail(row.id);

  }



  backToList(): void {

    this.detail.set(null);

    void this.router.navigate(['/infoproductor/quiz-review']);

    this.loadList();

  }



  submitReview(approved: boolean): void {

    const current = this.detail();

    if (!current) {

      return;

    }

    this.reviewing.set(true);

    this.quizReviewService.review(current.id, { approved, feedback: this.feedback() }).subscribe({

      next: (updated) => {

        this.detail.set(updated);

        this.reviewing.set(false);

      },

      error: (err) => {

        this.error.set(messageFromHttpError(err, 'No se pudo registrar la revisión.'));

        this.reviewing.set(false);

      },

    });

  }



  gradeAnswer(answer: QuizAnswerReview, correct: boolean): void {

    const current = this.detail();

    if (!current) {

      return;

    }

    this.gradingAnswerId.set(answer.id);

    this.quizReviewService

      .gradeAnswer(current.id, { answerId: answer.id, correct, feedback: answer.tutorFeedback })

      .subscribe({

        next: (result) => {

          this.detail.update((prev) => {

            if (!prev) {

              return prev;

            }

            const answers = (prev.answers ?? []).map((item) =>

              item.id === answer.id ? { ...item, correct } : item,

            );

            return {

              ...prev,

              score: result.newScore,

              reviewStatus: result.reviewStatus ?? prev.reviewStatus,

              answers,

            };

          });

          this.gradingAnswerId.set(null);

        },

        error: (err) => {

          this.error.set(messageFromHttpError(err, 'No se pudo calificar la respuesta.'));

          this.gradingAnswerId.set(null);

        },

      });

  }



  isGrantable(answer: QuizAnswerReview): boolean {

    return answer.requiresManualGrading || answer.questionType === 'free_text';

  }

}


