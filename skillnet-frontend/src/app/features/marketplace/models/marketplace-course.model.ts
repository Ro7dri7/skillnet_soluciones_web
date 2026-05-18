export interface MarketplaceCourse {
  id: number;
  title: string;
  slug: string;
  description: string;
  level: string;
  status: string;
  price: number;
  originalPrice?: number;
  category: string;
  format: string;
  rating: number;
  enrollmentCount: number;
  lessonsCount: number;
  professorName: string;
  imageUrl: string | null;
}
