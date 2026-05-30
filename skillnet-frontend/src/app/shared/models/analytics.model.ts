export interface KpiData {
  totalRevenue: number;
  activeStudents: number;
  publishedCourses: number;
  coursesSold: number;
  avgRating: number;
}

export interface DailyCount {
  date: string;
  count: number;
}

export interface DailyRevenue {
  date: string;
  amount: number;
}

export interface CategorySales {
  categoryName: string;
  totalSales: number;
}

export interface TopCourse {
  id: number;
  title: string;
  studentsCount: number;
  revenue: number;
}

export interface RecentTransaction {
  date: string;
  courseName: string;
  studentName: string;
  amount: number;
}

export interface ProducerAnalyticsResponse {
  kpis: KpiData;
  revenueTrend: DailyRevenue[];
  coursesSoldTrend: DailyCount[];
  salesByCategory: CategorySales[];
  topCourses: TopCourse[];
  recentTransactions: RecentTransaction[];
}

export interface StudentKpiData {
  purchasedCourses: number;
  purchasedInPeriod?: number;
  completedCourses: number;
  certificates: number;
  activeCourses: number;
}

export interface CategoryProgress {
  categoryName: string;
  percent: number;
}

export interface StudentLearningCourse {
  id: number;
  title: string;
  professor: string;
  category: string;
  slug: string | null;
  thumbnailUrl?: string | null;
  progress: number;
  lessonsDone: number;
  lessonsTotal: number;
  enrolledAt: string;
}

export interface StudentAnalyticsResponse {
  kpis: StudentKpiData;
  purchaseTrend: DailyCount[];
  progressByCategory: CategoryProgress[];
  learningCourses: StudentLearningCourse[];
}
