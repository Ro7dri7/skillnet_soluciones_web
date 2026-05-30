import {
  Component,
  ElementRef,
  effect,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MediaService } from '../../../../core/services/media.service';
import { ToastService } from '../../../../core/services/toast.service';
import type { QuizData, QuizQuestionDraft, QuizQuestionType } from '../../../../shared/models/curriculum.model';
import {
  MAX_QUIZ_ANSWER_OPTIONS,
  createDefaultQuizQuestion,
  questionTypeLabel,
} from '../../utils/quiz.util';

const QUESTION_TYPES: { label: string; value: QuizQuestionType }[] = [
  { label: 'Opción Múltiple', value: 'multiple' },
  { label: 'Verdadero/Falso', value: 'true_false' },
  { label: 'Relacionar', value: 'matching' },
  { label: 'Respuesta Libre', value: 'free_response' },
];

@Component({
  selector: 'app-quiz-builder',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './quiz-builder.component.html',
  styleUrl: './quiz-builder.component.scss',
})
export class QuizBuilderComponent {
  readonly questions = input.required<QuizQuestionDraft[]>();
  readonly passingScore = input.required<number>();
  readonly timeLimitMinutes = input.required<number>();
  readonly maxAttempts = input.required<number>();
  readonly courseId = input<number | null>(null);

  readonly dataChange = output<QuizData>();

  private readonly media = inject(MediaService);
  private readonly toast = inject(ToastService);

  readonly activeQuestionId = signal<string | null>(null);
  readonly imageUploadingId = signal<string | null>(null);

  readonly questionTypes = QUESTION_TYPES;
  readonly maxOptions = MAX_QUIZ_ANSWER_OPTIONS;
  readonly typeLabel = questionTypeLabel;

  private readonly stripRef = viewChild<ElementRef<HTMLElement>>('quizStrip');

  constructor() {
    effect(() => {
      const items = this.questions();
      const active = this.activeQuestionId();
      if (items.length === 0) {
        this.activeQuestionId.set(null);
        return;
      }
      if (!active || !items.some((q) => q.id === active)) {
        this.activeQuestionId.set(items[0].id);
      }
    });

    effect(() => {
      const activeId = this.activeQuestionId();
      const strip = this.stripRef()?.nativeElement;
      if (!activeId || !strip) {
        return;
      }
      const card = strip.querySelector(`[data-quiz-strip-id="${activeId}"]`);
      if (card instanceof HTMLElement) {
        requestAnimationFrame(() => {
          card.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
        });
      }
    });
  }

  activeQuestion(): QuizQuestionDraft | undefined {
    const id = this.activeQuestionId();
    return this.questions().find((q) => q.id === id);
  }

  activeQuestionIndex(): number {
    const id = this.activeQuestionId();
    return this.questions().findIndex((q) => q.id === id);
  }

  emitChange(partial: Partial<QuizData>): void {
    this.dataChange.emit({
      passingScore: partial.passingScore ?? this.passingScore(),
      timeLimitMinutes: partial.timeLimitMinutes ?? this.timeLimitMinutes(),
      maxAttempts: partial.maxAttempts ?? this.maxAttempts(),
      questions: partial.questions ?? this.questions(),
    });
  }

  updatePassingScore(value: number): void {
    this.emitChange({ passingScore: value });
  }

  updateTimeLimit(value: number): void {
    this.emitChange({ timeLimitMinutes: value });
  }

  updateMaxAttempts(value: number): void {
    this.emitChange({ maxAttempts: value });
  }

  selectQuestion(id: string): void {
    this.activeQuestionId.set(id);
  }

  onStripKeydown(event: KeyboardEvent, id: string): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.selectQuestion(id);
    }
  }

  addQuestion(): void {
    const next = [...this.questions(), createDefaultQuizQuestion()];
    this.activeQuestionId.set(next[next.length - 1].id);
    this.emitChange({ questions: next });
  }

  removeQuestion(id: string, event?: Event): void {
    event?.stopPropagation();
    const list = this.questions();
    if (list.length <= 1) {
      return;
    }
    const idx = list.findIndex((q) => q.id === id);
    const next = list.filter((q) => q.id !== id);
    if (this.activeQuestionId() === id) {
      const neighbor = next[idx] ?? next[idx - 1] ?? next[0];
      this.activeQuestionId.set(neighbor?.id ?? null);
    }
    this.emitChange({ questions: next });
  }

  patchQuestion(id: string, patch: Partial<QuizQuestionDraft>): void {
    const next = this.questions().map((q) => (q.id === id ? { ...q, ...patch } : q));
    this.emitChange({ questions: next });
  }

  setQuestionType(id: string, type: QuizQuestionType): void {
    const q = this.questions().find((item) => item.id === id);
    if (!q) {
      return;
    }

    let options = [...q.options];
    let matches = q.matches ? [...q.matches] : [];

    if (type === 'true_false') {
      options = ['Verdadero', 'Falso'];
    } else if (type === 'multiple' && q.type === 'true_false') {
      options = ['', '', '', ''];
    } else if (type === 'matching') {
      if (!q.matches?.length) {
        matches = Array(options.length).fill('');
      }
    } else if (type === 'free_response') {
      options = ['Escribe aquí tu respuesta...'];
    }

    this.patchQuestion(id, { type, options, matches, correctIndex: 0 });
  }

  setCorrectOption(id: string, index: number): void {
    this.patchQuestion(id, { correctIndex: index });
  }

  updateOption(id: string, index: number, value: string): void {
    const q = this.questions().find((item) => item.id === id);
    if (!q || q.type === 'true_false') {
      return;
    }
    const options = [...q.options];
    options[index] = value;
    this.patchQuestion(id, { options });
  }

  updateMatch(id: string, index: number, value: string): void {
    const q = this.questions().find((item) => item.id === id);
    if (!q) {
      return;
    }
    const matches = q.matches ? [...q.matches] : Array(q.options.length).fill('');
    matches[index] = value;
    this.patchQuestion(id, { matches });
  }

  addOption(id: string): void {
    const q = this.questions().find((item) => item.id === id);
    if (!q || q.type === 'true_false' || q.type === 'free_response') {
      return;
    }
    if (
      (q.type === 'multiple' || q.type === 'matching') &&
      q.options.length >= MAX_QUIZ_ANSWER_OPTIONS
    ) {
      return;
    }
    const options = [...q.options, ''];
    const matches = q.matches ? [...q.matches, ''] : q.type === 'matching' ? [''] : q.matches;
    this.patchQuestion(id, { options, matches });
  }

  removeOption(id: string, index: number): void {
    const q = this.questions().find((item) => item.id === id);
    if (!q || q.type === 'true_false') {
      return;
    }
    const options = q.options.filter((_, i) => i !== index);
    const matches = q.matches?.filter((_, i) => i !== index);
    let correctIndex = q.correctIndex;
    if (correctIndex >= options.length) {
      correctIndex = Math.max(0, options.length - 1);
    }
    this.patchQuestion(id, { options, matches, correctIndex });
  }

  optionCountLabel(q: QuizQuestionDraft): string {
    if (q.type === 'free_response') {
      return 'Respuesta abierta';
    }
    return `${q.options.length} opciones`;
  }

  canAddOption(q: QuizQuestionDraft): boolean {
    if (q.type === 'true_false' || q.type === 'free_response') {
      return false;
    }
    if (q.type === 'multiple' || q.type === 'matching') {
      return q.options.length < MAX_QUIZ_ANSWER_OPTIONS;
    }
    return true;
  }

  optionLetter(index: number): string {
    return String.fromCharCode(65 + index);
  }

  triggerImageUpload(id: string): void {
    document.getElementById(`question-image-upload-${id}`)?.click();
  }

  clearQuestionImage(id: string): void {
    this.patchQuestion(id, { imageUrl: null });
  }

  async onQuestionImageSelected(id: string, event: Event): Promise<void> {
    const inputEl = event.target as HTMLInputElement;
    const file = inputEl.files?.[0];
    inputEl.value = '';
    if (!file) {
      return;
    }

    const courseId = this.courseId();
    if (!courseId) {
      this.toast.info('Guarda el curso antes de subir imágenes.');
      return;
    }

    this.imageUploadingId.set(id);
    try {
      const res = await firstValueFrom(this.media.uploadLessonResource(courseId, file, 'image'));
      this.patchQuestion(id, { imageUrl: res.url });
    } catch {
      this.toast.error('No se pudo subir la imagen.');
    } finally {
      this.imageUploadingId.set(null);
    }
  }
}
