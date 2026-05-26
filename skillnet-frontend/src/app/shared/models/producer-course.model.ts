export interface CreateCourseRequest {
  title: string;
  courseFormat: string;
  category: string;
  subcategory: string;
  whatYouWillLearn: string;
  targetAudience: string;
}

export interface ProducerCourseSummary {
  id: number;
  title: string;
  courseFormat: string;
  status: string;
  createdAt: string;
  imageUrl?: string | null;
}

export interface UpdateCourseBasicsRequest {
  title?: string;
  description?: string;
  price?: number;
  imageUrl?: string;
  videoUrl?: string;
  language?: string;
  level?: string;
  category?: string;
  subcategory?: string;
  whatYouWillLearn?: string;
  targetAudience?: string;
}

export interface CourseBasicsResponse {
  id: number;
  title: string;
  description: string;
  price: number;
  imageUrl: string | null;
  videoUrl: string | null;
  language: string | null;
  level: string | null;
  category: string | null;
  subcategory: string | null;
  status: string;
}

export interface UpdateCoursePricingRequest {
  currency?: string;
  price?: number;
  onSale?: boolean;
  discountPrice?: number;
  affiliateCommission?: number;
  affiliationType?: string;
}

export interface CoursePricingResponse {
  id: number;
  currency: string;
  price: number;
  originalPrice: number;
  onSale: boolean;
  discountPrice: number | null;
  affiliateCommission: number;
  affiliationType: string;
}

export interface UpdateCourseMessagesRequest {
  welcomeMessage?: string;
  congratulationsMessage?: string;
}

export interface CourseMessagesResponse {
  id: number;
  welcomeMessage: string | null;
  congratulationsMessage: string | null;
}

export interface CreateCourseCouponRequest {
  code: string;
  percentOff: number;
  amountOff?: number;
}

export interface CourseCouponResponse {
  id: number;
  code: string;
  percentOff: number;
  amountOff: number;
  active: boolean;
  validTo: string | null;
}
