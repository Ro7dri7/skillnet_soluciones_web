export interface KpiData {
  totalRevenue: number;
  activeStudents: number;
  publishedCourses: number;
  avgRating: number;
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
  salesByCategory: CategorySales[];
  topCourses: TopCourse[];
  recentTransactions: RecentTransaction[];
}
