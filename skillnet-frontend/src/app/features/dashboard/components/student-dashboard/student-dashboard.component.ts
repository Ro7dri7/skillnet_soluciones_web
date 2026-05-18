import { Component, computed, ElementRef, input, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CourseCardComponent } from '../../../../shared/components/course-card/course-card.component';
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

interface LearningCourse {
  id: number;
  title: string;
  professor: string;
  category: CategoryFilter;
  tab: Exclude<LearningTab, 'todos'>;
  progress: number;
  lessonsDone: number;
  lessonsTotal: number;
  gradient: string;
}

interface LearningPathStep {
  title: string;
  status: 'completed' | 'in_progress' | 'pending';
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

interface KpiSlice {
  enrolled: number;
  completed: number;
  certificates: number;
  hours: number;
  enrolledBadge?: string;
}

@Component({
  selector: 'app-student-dashboard',
  standalone: true,
  imports: [FormsModule, CourseCardComponent],
  templateUrl: './student-dashboard.component.html',
})
export class StudentDashboardComponent {
  readonly user = input.required<User>();

  readonly carouselRef = viewChild<ElementRef<HTMLElement>>('carouselTrack');

  readonly years = ['2024', '2025', '2026'];
  readonly months = [
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
  ];
  readonly categories: CategoryFilter[] = [
    'Todas',
    'Tecnología',
    'Finanzas',
    'Marketing',
    'Diseño',
  ];
  readonly learningTabs: { id: LearningTab; label: string }[] = [
    { id: 'todos', label: 'Todos' },
    { id: 'videocursos', label: 'Videocursos' },
    { id: 'ebooks', label: 'Ebooks' },
    { id: 'audiolibros', label: 'Audiolibros' },
  ];

  selectedYear = '2026';
  selectedMonth = 'Mayo';
  selectedCategory: CategoryFilter = 'Todas';
  activeLearningTab = signal<LearningTab>('todos');

  private readonly kpiByPeriod: Record<string, KpiSlice> = {
    '2026-Mayo-Todas': { enrolled: 12, completed: 24, certificates: 8, hours: 342.5, enrolledBadge: '+2 hoy' },
    '2026-Mayo-Tecnología': { enrolled: 5, completed: 11, certificates: 4, hours: 156.2, enrolledBadge: '+1 hoy' },
    '2026-Mayo-Finanzas': { enrolled: 4, completed: 8, certificates: 2, hours: 98.0 },
    '2026-Mayo-Marketing': { enrolled: 2, completed: 3, certificates: 1, hours: 45.5 },
    '2026-Mayo-Diseño': { enrolled: 1, completed: 2, certificates: 1, hours: 42.8 },
    '2026-Abril-Todas': { enrolled: 10, completed: 20, certificates: 6, hours: 298.0 },
    '2025-Diciembre-Todas': { enrolled: 8, completed: 18, certificates: 5, hours: 210.5 },
  };

  private readonly allProgress: ProgressItem[] = [
    { subject: 'Tecnología', percent: 92, categories: ['Todas', 'Tecnología'] },
    { subject: 'Finanzas', percent: 85, categories: ['Todas', 'Finanzas'] },
    { subject: 'Marketing', percent: 45, categories: ['Todas', 'Marketing'] },
    { subject: 'Diseño', percent: 62, categories: ['Todas', 'Diseño'] },
  ];

  private readonly allLearningCourses: LearningCourse[] = [
    {
      id: 1,
      title: 'Desarrollo Web Fullstack',
      professor: 'Prof. Maria Elena Castro',
      category: 'Tecnología',
      tab: 'videocursos',
      progress: 85,
      lessonsDone: 24,
      lessonsTotal: 28,
      gradient: 'from-skillnet-dark via-skillnet-accent/80 to-skillnet-cyan/60',
    },
    {
      id: 2,
      title: 'Inversiones en Mercados Globales',
      professor: 'Prof. David Chen',
      category: 'Finanzas',
      tab: 'videocursos',
      progress: 42,
      lessonsDone: 12,
      lessonsTotal: 30,
      gradient: 'from-emerald-900 via-emerald-700 to-teal-600',
    },
    {
      id: 3,
      title: 'UX/UI Design Pro',
      professor: 'Prof. Sarah Johnson',
      category: 'Diseño',
      tab: 'ebooks',
      progress: 15,
      lessonsDone: 4,
      lessonsTotal: 32,
      gradient: 'from-violet-900 via-purple-700 to-fuchsia-600',
    },
    {
      id: 4,
      title: 'Growth Marketing 2026',
      professor: 'Prof. Laura Méndez',
      category: 'Marketing',
      tab: 'audiolibros',
      progress: 58,
      lessonsDone: 14,
      lessonsTotal: 24,
      gradient: 'from-orange-900 via-amber-700 to-yellow-600',
    },
  ];

  readonly learningPath: LearningPathStep[] = [
    { title: 'Fundamentos de Python', status: 'completed' },
    { title: 'Análisis de Datos con Pandas', status: 'in_progress' },
    { title: 'Machine Learning Avanzado', status: 'pending' },
  ];

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
    {
      id: 3,
      title: 'Branding Digital Avanzado',
      price: 59.99,
      category: 'Marketing',
      imageUrl: null,
      rating: 4.7,
      students: '980 alumnos',
    },
    {
      id: 4,
      title: 'Figma para Product Designers',
      price: 49.99,
      category: 'Diseño',
      imageUrl: null,
      rating: 4.9,
      students: '3.1k alumnos',
    },
  ];

  readonly filteredKpis = computed<KpiMetric[]>(() => {
    const slice = this.resolveKpiSlice();
    return [
      {
        label: 'Cursos Activos',
        value: slice.enrolled,
        icon: 'ri-book-open-line',
        badge: slice.enrolledBadge,
      },
      { label: 'Completados', value: slice.completed, icon: 'ri-checkbox-circle-line' },
      { label: 'Certificados', value: slice.certificates, icon: 'ri-award-line' },
      { label: 'Horas Invertidas', value: slice.hours, icon: 'ri-time-line' },
    ];
  });

  readonly filteredProgress = computed(() =>
    this.allProgress.filter(
      (p) => this.selectedCategory === 'Todas' || p.categories.includes(this.selectedCategory),
    ),
  );

  readonly filteredLearningCourses = computed(() =>
    this.allLearningCourses.filter((c) => {
      const categoryOk =
        this.selectedCategory === 'Todas' || c.category === this.selectedCategory;
      const tab = this.activeLearningTab();
      const tabOk = tab === 'todos' || c.tab === tab;
      return categoryOk && tabOk;
    }),
  );

  readonly streakDays = computed(() => {
    const base = 3;
    const monthOffset = this.months.indexOf(this.selectedMonth);
    return base + (monthOffset % 4);
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
    /* ngModel ya actualiza; computed reacciona automáticamente */
  }

  scrollCarousel(direction: 'left' | 'right'): void {
    const el = this.carouselRef()?.nativeElement;
    if (!el) {
      return;
    }
    el.scrollBy({ left: direction === 'left' ? -320 : 320, behavior: 'smooth' });
  }

  pathDotClass(status: LearningPathStep['status']): string {
    if (status === 'completed') {
      return 'bg-emerald-500';
    }
    if (status === 'in_progress') {
      return 'bg-skillnet-cyan animate-pulse';
    }
    return 'bg-slate-300';
  }

  private resolveKpiSlice(): KpiSlice {
    const exact = this.kpiByPeriod[`${this.selectedYear}-${this.selectedMonth}-${this.selectedCategory}`];
    if (exact) {
      return exact;
    }
    const period =
      this.kpiByPeriod[`${this.selectedYear}-${this.selectedMonth}-Todas`] ??
      this.kpiByPeriod['2026-Mayo-Todas'];
    if (this.selectedCategory === 'Todas') {
      return period;
    }
    const factor = this.categoryFactor(this.selectedCategory);
    return {
      enrolled: Math.max(1, Math.round(period.enrolled * factor)),
      completed: Math.max(0, Math.round(period.completed * factor)),
      certificates: Math.max(0, Math.round(period.certificates * factor)),
      hours: Math.round(period.hours * factor * 10) / 10,
    };
  }

  private categoryFactor(category: CategoryFilter): number {
    const map: Record<CategoryFilter, number> = {
      Todas: 1,
      Tecnología: 0.45,
      Finanzas: 0.35,
      Marketing: 0.25,
      Diseño: 0.2,
    };
    return map[category];
  }
}
