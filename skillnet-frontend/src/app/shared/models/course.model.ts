export type CourseLevel = 'beginner' | 'intermediate' | 'advanced';
export type CourseStatus = 'draft' | 'published';

export interface CourseResponse {
  id: number;
  title: string;
  slug: string;
  description: string;
  level: string;
  status: string;
  price: number;
  professorId: number | null;
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
