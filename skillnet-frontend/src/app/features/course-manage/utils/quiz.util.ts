import type { QuizData, QuizQuestionDraft, QuizQuestionType } from '../../../shared/models/curriculum.model';

export const MAX_QUIZ_ANSWER_OPTIONS = 6;

export function createQuizQuestionId(): string {
  return `q-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
}

export function createDefaultQuizQuestion(): QuizQuestionDraft {
  return {
    id: createQuizQuestionId(),
    type: 'multiple',
    text: '',
    options: [''],
    correctIndex: 0,
    matches: [],
    imageUrl: null,
  };
}

export function normalizeQuizQuestion(q: QuizQuestionDraft): QuizQuestionDraft {
  const type: QuizQuestionType = q.type ?? 'multiple';
  let options = Array.isArray(q.options) ? [...q.options] : [''];
  let matches = Array.isArray(q.matches) ? [...q.matches] : [];

  if (type === 'true_false') {
    options = ['Verdadero', 'Falso'];
  } else if (type === 'free_response') {
    options = options.length > 0 ? [options[0]] : ['Escribe aquí tu respuesta...'];
  } else if (options.length === 0) {
    options = [''];
  }

  if (type === 'matching' && matches.length < options.length) {
    matches = [...matches, ...Array(options.length - matches.length).fill('')];
  }

  const correctIndex = Math.min(Math.max(q.correctIndex ?? 0, 0), Math.max(options.length - 1, 0));

  return {
    ...q,
    type,
    options,
    matches: type === 'matching' ? matches : q.matches,
    correctIndex,
    imageUrl: q.imageUrl ?? null,
  };
}

export function normalizeQuizData(data: QuizData): QuizData {
  const questions =
    data.questions.length > 0
      ? data.questions.map(normalizeQuizQuestion)
      : [createDefaultQuizQuestion()];

  return {
    passingScore: data.passingScore ?? 80,
    timeLimitMinutes: data.timeLimitMinutes ?? 30,
    maxAttempts: data.maxAttempts ?? 3,
    questions,
  };
}

export function questionTypeLabel(type: QuizQuestionType | undefined): string {
  switch (type) {
    case 'true_false':
      return 'Verdadero/Falso';
    case 'matching':
      return 'Relacionar';
    case 'free_response':
      return 'Respuesta libre';
    default:
      return 'Opción múltiple';
  }
}
