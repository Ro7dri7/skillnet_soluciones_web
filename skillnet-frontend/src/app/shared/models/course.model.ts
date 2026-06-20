export type CourseLevel = 'beginner' | 'intermediate' | 'advanced';
export type CourseStatus = 'draft' | 'published';

export interface ProfessorSummary {
  id: number;
  username: string;
  email?: string;
  firstName?: string;
  lastName?: string;
}

export interface PublicCurriculumLesson {
  id: number;
  title: string;
}

export interface PublicCurriculumModule {
  id: number;
  title: string;
  lessons: PublicCurriculumLesson[];
}

export interface CourseResponse {
  id: number;
  title: string;
  slug: string;
  description: string;
  level: string;
  language: string;
  status: string;
  price: number;
  professorId: number | null;
  professor?: ProfessorSummary | null;
  imageUrl?: string | null;
  videoUrl?: string | null;
  category?: string | null;
  subcategory?: string | null;
  whatYouWillLearn?: string | null;
  targetAudience?: string | null;
  requirements?: string | null;
  durationHours?: number;
  durationMinutes?: number;
  currency?: string;
  originalPrice?: number;
  onSale?: boolean;
  affiliateCommission?: number;
  affiliatePolicy?: string;
  welcomeMessage?: string | null;
  congratulationsMessage?: string | null;
  courseFormat?: string | null;
  moduleCount?: number;
  lessonsCount?: number;
  enrollmentCount?: number;
  sections?: PublicCurriculumModule[];
}

export interface CourseRequest {
  title: string;
  slug: string;
  description: string;
  level: CourseLevel;
  status: CourseStatus;
  price: number;
}

/** Payload completo requerido por el backend Spring (CourseRequestDTO). */
export interface CourseApiPayload {
  title: string;
  slug: string;
  description: string;
  level: string;
  status: string;
  price: number;
  originalPrice: number;
  language: string;
  courseFormat: string;
  currency: string;
  affiliateCommission: number;
  affiliatePolicy: string;
  ally: string;
  securityStatus: string;
  professorId?: number;
}
