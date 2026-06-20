import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import type { QuizData, QuizQuestionDraft } from '../../../../shared/models/curriculum.model';
import { normalizeQuizData, questionTypeLabel } from '../../../course-manage/utils/quiz.util';
import { QuizSubmissionService } from '../../../../core/services/quiz-submission.service';

type QuizPhase = 'intro' | 'taking' | 'results';

@Component({
  selector: 'app-learn-quiz-player',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './learn-quiz-player.component.html',
  styleUrl: './learn-quiz-player.component.scss',
})
export class LearnQuizPlayerComponent {
  private readonly quizSubmissionService = inject(QuizSubmissionService);

  readonly quizData = input.required<QuizData>();
  readonly lessonId = input<number | null>(null);

  readonly passed = output<void>();

  readonly phase = signal<QuizPhase>('intro');
  readonly currentIndex = signal(0);
  readonly selectedAnswers = signal<Record<string, number | string>>({});
  readonly matchingAnswers = signal<Record<string, string[]>>({});
  readonly submitted = signal(false);

  readonly normalized = computed(() => normalizeQuizData(this.quizData()));

  readonly questions = computed(() => this.normalized().questions);

  readonly currentQuestion = computed(() => {
    const list = this.questions();
    const idx = this.currentIndex();
    return list[idx] ?? null;
  });

  readonly scorePercent = computed(() => {
    const list = this.questions();
    if (list.length === 0) {
      return 0;
    }
    const correct = list.filter((q) => this.isQuestionCorrect(q)).length;
    return Math.round((correct / list.length) * 100);
  });

  readonly hasPassed = computed(
    () => this.scorePercent() >= this.normalized().passingScore,
  );

  readonly typeLabel = questionTypeLabel;

  startQuiz(): void {
    this.phase.set('taking');
    this.currentIndex.set(0);
    this.selectedAnswers.set({});
    this.matchingAnswers.set({});
    this.submitted.set(false);
  }

  selectOption(questionId: string, optionIndex: number): void {
    if (this.submitted()) {
      return;
    }
    this.selectedAnswers.update((prev) => ({ ...prev, [questionId]: optionIndex }));
  }

  setFreeResponse(questionId: string, value: string): void {
    if (this.submitted()) {
      return;
    }
    this.selectedAnswers.update((prev) => ({ ...prev, [questionId]: value }));
  }

  setMatchingAnswer(questionId: string, index: number, value: string): void {
    if (this.submitted()) {
      return;
    }
    const q = this.questions().find((item) => item.id === questionId);
    if (!q) {
      return;
    }
    const next = [...(this.matchingAnswers()[questionId] ?? Array(q.options.length).fill(''))];
    next[index] = value;
    this.matchingAnswers.update((prev) => ({ ...prev, [questionId]: next }));
  }

  matchingValue(questionId: string, index: number): string {
    return this.matchingAnswers()[questionId]?.[index] ?? '';
  }

  selectedOptionIndex(questionId: string): number | null {
    const value = this.selectedAnswers()[questionId];
    return typeof value === 'number' ? value : null;
  }

  freeResponseValue(questionId: string): string {
    const value = this.selectedAnswers()[questionId];
    return typeof value === 'string' ? value : '';
  }

  goToQuestion(index: number): void {
    if (index < 0 || index >= this.questions().length) {
      return;
    }
    this.currentIndex.set(index);
  }

  goNext(): void {
    const idx = this.currentIndex();
    if (idx < this.questions().length - 1) {
      this.currentIndex.set(idx + 1);
      return;
    }
    this.finishQuiz();
  }

  goPrev(): void {
    const idx = this.currentIndex();
    if (idx > 0) {
      this.currentIndex.set(idx - 1);
    }
  }

  finishQuiz(): void {
    this.submitted.set(true);
    this.phase.set('results');

    const lessonId = this.lessonId();
    const needsManualReview = this.questions().some((q) => (q.type ?? 'multiple') === 'free_response');
    const answers = this.questions().map((q) => {
      const type = q.type ?? 'multiple';
      if (type === 'free_response') {
        return {
          questionId: q.id,
          textAnswer: this.freeResponseValue(q.id),
        };
      }
      if (type === 'matching') {
        return {
          questionId: q.id,
          matchingAnswers: [...(this.matchingAnswers()[q.id] ?? [])],
        };
      }
      const selected = this.selectedAnswers()[q.id];
      return {
        questionId: q.id,
        optionIndex: typeof selected === 'number' ? selected : undefined,
      };
    });
    const payload = {
      score: this.scorePercent(),
      needsManualReview,
      answers,
    };

    if (lessonId != null) {
      this.quizSubmissionService.submit(lessonId, payload).subscribe({
        next: () => {
          if (this.hasPassed()) {
            this.passed.emit();
          }
        },
        error: () => {
          if (this.hasPassed()) {
            this.passed.emit();
          }
        },
      });
      return;
    }

    if (this.hasPassed()) {
      this.passed.emit();
    }
  }

  isQuestionCorrect(q: QuizQuestionDraft): boolean {
    const type = q.type ?? 'multiple';

    if (type === 'free_response') {
      const answer = this.selectedAnswers()[q.id];
      return typeof answer === 'string' && answer.trim().length > 0;
    }

    if (type === 'matching') {
      const answers = this.matchingAnswers()[q.id] ?? [];
      const expected = q.matches ?? [];
      return q.options.every((_, i) => {
        const given = (answers[i] ?? '').trim().toLowerCase();
        const expectedVal = (expected[i] ?? '').trim().toLowerCase();
        return given.length > 0 && given === expectedVal;
      });
    }

    const selected = this.selectedAnswers()[q.id];
    return typeof selected === 'number' && selected === q.correctIndex;
  }

  questionStatus(index: number): 'correct' | 'incorrect' | 'pending' {
    if (!this.submitted()) {
      return 'pending';
    }
    const q = this.questions()[index];
    return this.isQuestionCorrect(q) ? 'correct' : 'incorrect';
  }

  canAdvance(): boolean {
    const q = this.currentQuestion();
    if (!q) {
      return false;
    }
    const type = q.type ?? 'multiple';

    if (type === 'free_response') {
      return this.freeResponseValue(q.id).trim().length > 0;
    }

    if (type === 'matching') {
      const answers = this.matchingAnswers()[q.id] ?? [];
      return q.options.every((_, i) => (answers[i] ?? '').trim().length > 0);
    }

    return this.selectedOptionIndex(q.id) !== null;
  }

  isLastQuestion(): boolean {
    return this.currentIndex() >= this.questions().length - 1;
  }

  optionLetter(index: number): string {
    return String.fromCharCode(65 + index);
  }

  retryQuiz(): void {
    this.phase.set('intro');
    this.currentIndex.set(0);
    this.selectedAnswers.set({});
    this.matchingAnswers.set({});
    this.submitted.set(false);
  }
}
