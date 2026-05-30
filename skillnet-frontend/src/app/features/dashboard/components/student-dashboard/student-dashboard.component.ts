import { Component, computed, ElementRef, inject, input, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import type { EChartsCoreOption } from 'echarts/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import { catchError, combineLatest, of, switchMap, tap } from 'rxjs';
import { CourseCardComponent } from '../../../../shared/components/course-card/course-card.component';
import { StudentAnalyticsService } from '../../../../core/services/student-analytics.service';
import {
  StudentAnalyticsResponse,
  StudentLearningCourse,
} from '../../../../shared/models/analytics.model';
import { User } from '../../../../shared/models/auth.model';

export type CategoryFilter = 'Todas' | 'Tecnología' | 'Finanzas' | 'Marketing' | 'Diseño';
export type LearningTab = 'todos' | 'videocursos' | 'ebooks' | 'audiolibros';

interface KpiMetric {
  label: string;
  value: number;
  icon: string;
  badge?: string;
}

interface ProgressItem {
  subject: string;
  percent: number;
  categories: CategoryFilter[];
}

interface RecommendedCourse {
  id: number;
  title: string;
  price: number;
  category: string;
  imageUrl: string | null;
  rating: number;
  students: string;
}

const MONTH_NAMES = [
  'Enero',
  'Febrero',
  'Marzo',
  'Abril',
  'Mayo',
  'Junio',
  'Julio',
  'Agosto',
  'Septiembre',
  'Octubre',
  'Noviembre',
  'Diciembre',
] as const;

const EMPTY_ANALYTICS: StudentAnalyticsResponse = {
  kpis: { purchasedCourses: 0, purchasedInPeriod: 0, completedCourses: 0, certificates: 0, activeCourses: 0 },
  purchaseTrend: [],
  progressByCategory: [],
  learningCourses: [],
};

@Component({
  selector: 'app-student-dashboard',
  standalone: true,
  imports: [FormsModule, RouterLink, CourseCardComponent, NgxEchartsDirective],
  templateUrl: './student-dashboard.component.html',
})
export class StudentDashboardComponent {
  private readonly analyticsService = inject(StudentAnalyticsService);

  readonly user = input.required<User>();
  readonly carouselRef = viewChild<ElementRef<HTMLElement>>('carouselTrack');

  readonly months = [...MONTH_NAMES];
  readonly years = this.buildYearOptions();
  readonly categories: CategoryFilter[] = ['Todas', 'Tecnología', 'Finanzas', 'Marketing', 'Diseño'];
  readonly learningTabs: { id: LearningTab; label: string }[] = [
    { id: 'todos', label: 'Todos' },
    { id: 'videocursos', label: 'Videocursos' },
    { id: 'ebooks', label: 'Ebooks' },
    { id: 'audiolibros', label: 'Audiolibros' },
  ];

  readonly selectedYear = signal(String(new Date().getFullYear()));
  readonly selectedMonth = signal(MONTH_NAMES[new Date().getMonth()]);
  selectedCategory: CategoryFilter = 'Todas';
  readonly activeLearningTab = signal<LearningTab>('todos');

  readonly isLoading = signal(true);
  readonly loadError = signal<string | null>(null);

  readonly analyticsData = toSignal<StudentAnalyticsResponse | null>(
    combineLatest([toObservable(this.selectedYear), toObservable(this.selectedMonth)]).pipe(
      tap(() => {
        this.isLoading.set(true);
        this.loadError.set(null);
      }),
      switchMap(([yearStr, monthName]) => {
        const year = Number.parseInt(yearStr, 10);
        const month = this.months.indexOf(monthName) + 1;
        return this.analyticsService.getAnalytics(year, month).pipe(
          tap(() => this.isLoading.set(false)),
          catchError((err: unknown) => {
            this.isLoading.set(false);
            if (!(err instanceof HttpErrorResponse && (err.status === 401 || err.status === 403))) {
              this.loadError.set('No se pudieron cargar tus métricas de aprendizaje.');
            }
            return of(EMPTY_ANALYTICS);
          }),
        );
      }),
    ),
    { initialValue: null },
  );

  readonly recommendedCourses: RecommendedCourse[] = [
    {
      id: 1,
      title: 'Trading de Alta Frecuencia',
      price: 129.99,
      category: 'Finanzas',
      imageUrl: null,
      rating: 4.9,
      students: '2.4k alumnos',
    },
    {
      id: 2,
      title: 'Python para Análisis Financiero',
      price: 89.99,
      category: 'Tecnología',
      imageUrl: null,
      rating: 4.8,
      students: '1.8k alumnos',
    },
  ];

  readonly filteredKpis = computed<KpiMetric[]>(() => {
    const kpis = this.analyticsData()?.kpis;
    if (!kpis) {
      return [];
    }
    return [
      {
        label: 'Cursos Comprados',
        value: kpis.purchasedCourses,
        icon: 'ri-shopping-bag-line',
        badge:
          (kpis.purchasedInPeriod ?? 0) > 0
            ? `+${kpis.purchasedInPeriod} ${this.selectedMonth()}`
            : undefined,
      },
      { label: 'Completados', value: kpis.completedCourses, icon: 'ri-checkbox-circle-line' },
      { label: 'Certificados', value: kpis.certificates, icon: 'ri-award-line' },
      { label: 'En progreso', value: kpis.activeCourses, icon: 'ri-book-open-line' },
    ];
  });

  readonly filteredProgress = computed<ProgressItem[]>(() => {
    const rows = this.analyticsData()?.progressByCategory ?? [];
    return rows
      .filter((row) => this.matchesCategory(row.categoryName, this.selectedCategory))
      .map((row) => ({
        subject: row.categoryName,
        percent: row.percent,
        categories: ['Todas', row.categoryName as CategoryFilter],
      }));
  });

  readonly filteredLearningCourses = computed(() => {
    const courses = this.analyticsData()?.learningCourses ?? [];
    return courses.filter((c) => {
      const categoryOk = this.matchesCategory(c.category, this.selectedCategory);
      const tab = this.activeLearningTab();
      const tabOk = tab === 'todos' || tab === 'videocursos';
      return categoryOk && tabOk;
    });
  });

  readonly continueCourse = computed(() => {
    const courses = this.analyticsData()?.learningCourses ?? [];
    return courses.find((c) => c.progress < 100) ?? courses[0] ?? null;
  });

  readonly purchaseChartOptions = computed<EChartsCoreOption>(() => {
    const trend = this.analyticsData()?.purchaseTrend ?? [];
    return {
      grid: { left: 36, right: 12, top: 16, bottom: 28 },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: trend.map((p) => this.formatChartDate(p.date)),
        axisLabel: { fontSize: 10, color: '#6b7280' },
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { fontSize: 10, color: '#6b7280' },
      },
      series: [
        {
          name: 'Compras',
          type: 'bar',
          data: trend.map((p) => p.count),
          itemStyle: { color: '#145bff', borderRadius: [4, 4, 0, 0] },
          barMaxWidth: 24,
        },
      ],
    };
  });

  readonly streakDays = computed(() => {
    const active = this.analyticsData()?.kpis.activeCourses ?? 0;
    return Math.max(1, Math.min(30, active + 2));
  });

  welcomeName(user: User): string {
    const name = [user.firstName, user.lastName].filter(Boolean).join(' ');
    return name || user.username;
  }

  setLearningTab(tab: LearningTab): void {
    this.activeLearningTab.set(tab);
  }

  isLearningTabActive(tab: LearningTab): boolean {
    return this.activeLearningTab() === tab;
  }

  onFiltersChange(): void {
    /* selectedYear / selectedMonth recargan vía toSignal */
  }

  scrollCarousel(direction: 'left' | 'right'): void {
    const el = this.carouselRef()?.nativeElement;
    if (!el) {
      return;
    }
    el.scrollBy({ left: direction === 'left' ? -320 : 320, behavior: 'smooth' });
  }

  courseLearnLink(course: StudentLearningCourse): string[] {
    const slug = course.slug?.trim();
    return slug ? ['/marketplace/course', slug, 'learn'] : ['/mis-cursos'];
  }

  learningActionLabel(progress: number): string {
    if (progress >= 100) {
      return 'Repasar Aprendizaje';
    }
    if (progress > 0) {
      return 'Continuar Aprendizaje';
    }
    return 'Iniciar Aprendizaje';
  }

  private buildYearOptions(): string[] {
    const currentYear = new Date().getFullYear();
    return Array.from({ length: 4 }, (_, i) => String(currentYear - 2 + i));
  }

  private matchesCategory(categoryName: string, filter: CategoryFilter): boolean {
    if (filter === 'Todas') {
      return true;
    }
    const normalized = categoryName.toLowerCase();
    return normalized.includes(filter.toLowerCase()) || filter.toLowerCase().includes(normalized);
  }

  private formatChartDate(isoDate: string): string {
    const parsed = new Date(isoDate);
    if (Number.isNaN(parsed.getTime())) {
      return isoDate;
    }
    return parsed.toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
  }
}
