export type ContentType = 'text' | 'image' | 'video' | 'pdf' | 'quiz' | 'audio';

export interface QuizQuestionDraft {
  id: string;
  text: string;
  options: string[];
  correctIndex: number;
}

export interface QuizData {
  passingScore: number;
  timeLimitMinutes: number;
  questions: QuizQuestionDraft[];
}

export interface ContentBlockDTO {
  id: string;
  contentType: ContentType;
  resourceUrl: string;
  textContent: string;
  orderIndex: number;
  quizData?: QuizData;
}

export interface LessonDTO {
  id: string;
  title: string;
  contentType: ContentType;
  resourceUrl: string;
  textContent: string;
  orderIndex: number;
  quizData?: QuizData;
  blocks: ContentBlockDTO[];
}

export interface SectionDTO {
  id: string;
  title: string;
  orderIndex: number;
  expanded: boolean;
  lessons: LessonDTO[];
}

export interface AddLessonData {
  title: string;
  contentType?: ContentType;
  resourceUrl?: string;
  textContent?: string;
  quizData?: QuizData;
  blocks?: ContentBlockDTO[];
}

export interface PendingLessonForm {
  moduleId: string;
  contentType: ContentType;
}
