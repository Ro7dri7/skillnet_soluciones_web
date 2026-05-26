export type ProductType =
  | 'course'
  | 'ebook'
  | 'audiobook'
  | 'podcast'
  | 'subscription'
  | 'event'
  | 'workshop'
  | 'app'
  | 'script'
  | 'image';

export type LessonType = 'VIDEO' | 'TEXT' | 'QUIZ';

export interface BuilderLessonDraft {
  clientId: string;
  title: string;
  contentUrl: string;
  lessonType: LessonType;
  durationMinutes: number;
  orderIndex: number;
}

export interface BuilderModuleDraft {
  clientId: string;
  title: string;
  orderIndex: number;
  expanded: boolean;
  lessons: BuilderLessonDraft[];
}

export interface CourseBuilderDraft {
  productType: ProductType | null;
  productLabel: string;
  title: string;
  category: string;
  subcategory: string;
  whatYouWillLearn: string;
  targetAudience: string;
  curriculum: BuilderModuleDraft[];
  courseId: number | null;
}

export interface CourseBuilderRequest {
  format: string;
  title: string;
  category: string;
  subcategory: string;
  audience: string;
  curriculum: {
    title: string;
    orderIndex: number;
    lessons: {
      title: string;
      contentUrl: string;
      lessonType: LessonType;
      durationMinutes: number;
      orderIndex: number;
    }[];
  }[];
}

export interface CourseBuilderResponse {
  id: number;
  slug: string;
  format: string;
  title: string;
  category: string;
  subcategory: string;
  audience: string;
  status: string;
  curriculum: {
    id: number;
    title: string;
    orderIndex: number;
    lessons: {
      id: number;
      title: string;
      contentUrl: string;
      lessonType: LessonType;
      durationMinutes: number;
      orderIndex: number;
    }[];
  }[];
}
