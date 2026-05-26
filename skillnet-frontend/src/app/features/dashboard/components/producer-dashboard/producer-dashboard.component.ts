import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import type { EChartsCoreOption } from 'echarts/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, combineLatest, of, switchMap, tap } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { ProducerAnalyticsService } from '../../../../core/services/producer-analytics.service';
import { ProducerAnalyticsResponse } from '../../../../shared/models/analytics.model';
import { User } from '../../../../shared/models/auth.model';

const CHART_COLORS = ['#145bff', '#39b8fd', '#89ceff', '#032b60', '#6366f1', '#0ea5e9'];

const TOP_COURSE_COVER_STYLES = [
  'linear-gradient(135deg, #032b60 0%, #145bff 100%)',
  'linear-gradient(135deg, #145bff 0%, #39b8fd 100%)',
  'linear-gradient(135deg, #39b8fd 0%, #89ceff 100%)',
];

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

const EMPTY_ANALYTICS: ProducerAnalyticsResponse = {
  kpis: { totalRevenue: 0, activeStudents: 0, publishedCourses: 0, avgRating: 0 },
  revenueTrend: [],
  salesByCategory: [],
  topCourses: [],
  recentTransactions: [],
};

@Component({
  selector: 'app-producer-dashboard',
  standalone: true,
  imports: [RouterLink, FormsModule, CurrencyPipe, DatePipe, DecimalPipe, NgxEchartsDirective],
  templateUrl: './producer-dashboard.component.html',
})
export class ProducerDashboardComponent {
  private readonly analyticsService = inject(ProducerAnalyticsService);
  private readonly authService = inject(AuthService);

  readonly user = input.required<User>();

  readonly months = [...MONTH_NAMES];

  readonly years = this.buildYearOptions();

  readonly selectedYear = signal(String(new Date().getFullYear()));
  readonly selectedMonth = signal(MONTH_NAMES[new Date().getMonth()]);

  readonly isLoading = signal(true);
  readonly loadError = signal<string | null>(null);

  readonly analyticsData = toSignal<ProducerAnalyticsResponse | null>(
    combineLatest([
      toObservable(this.selectedYear),
      toObservable(this.selectedMonth),
      this.authService.currentUser$,
    ]).pipe(
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
            if (err instanceof HttpErrorResponse && (err.status === 403 || err.status === 401)) {
              return of(EMPTY_ANALYTICS);
            }
            this.loadError.set('No se pudieron cargar las analíticas.');
            return of(EMPTY_ANALYTICS);
          }),
        );
      }),
    ),
    { initialValue: null },
  );

  readonly revenueChartOptions = computed<EChartsCoreOption>(() =>
    this.buildRevenueChart(this.analyticsData() ?? null),
  );

  readonly categoryChartOptions = computed<EChartsCoreOption>(() =>
    this.buildCategoryChart(this.analyticsData() ?? null),
  );

  onFiltersChange(): void {
    // selectedYear / selectedMonth disparan la recarga vía combineLatest + switchMap.
  }

  welcomeName(user: User): string {
    const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
    return fullName || user.username;
  }

  coverStyleForIndex(index: number): string {
    return TOP_COURSE_COVER_STYLES[index % TOP_COURSE_COVER_STYLES.length];
  }

  parseTransactionDate(dateStr: string): Date {
    const parsed = new Date(dateStr);
    return Number.isNaN(parsed.getTime()) ? new Date() : parsed;
  }

  private buildYearOptions(): string[] {
    const currentYear = new Date().getFullYear();
    return Array.from({ length: 4 }, (_, i) => String(currentYear - 2 + i));
  }

  private buildRevenueChart(data: ProducerAnalyticsResponse | null): EChartsCoreOption {
    const trend = data?.revenueTrend ?? [];
    const labels = trend.map((point) => this.formatChartDate(point.date));
    const values = trend.map((point) => Number(point.amount) || 0);

    return {
      animationDuration: 900,
      animationEasing: 'cubicOut',
      grid: { left: 48, right: 20, top: 24, bottom: 32 },
      tooltip: {
        trigger: 'axis',
        backgroundColor: '#032b60',
        borderWidth: 0,
        textStyle: { color: '#fff', fontSize: 12 },
        formatter: (params: unknown) => {
          const items = params as { axisValue: string; data: number }[];
          const point = items[0];
          if (!point) {
            return '';
          }
          return `${point.axisValue}<br/><b>$${point.data.toLocaleString()}</b>`;
        },
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: labels,
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisLabel: { color: '#6b7280', fontSize: 10, interval: Math.max(0, Math.floor(labels.length / 6)) },
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        splitLine: { lineStyle: { color: '#f3f4f6' } },
        axisLabel: {
          color: '#6b7280',
          fontSize: 10,
          formatter: (v: number) => `$${v}`,
        },
      },
      series: [
        {
          name: 'Ingresos',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          showSymbol: labels.length <= 12,
          lineStyle: { width: 3, color: '#145bff' },
          itemStyle: { color: '#145bff' },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(20, 91, 255, 0.35)' },
                { offset: 1, color: 'rgba(20, 91, 255, 0.04)' },
              ],
            },
          },
          data: values,
        },
      ],
    };
  }

  private buildCategoryChart(data: ProducerAnalyticsResponse | null): EChartsCoreOption {
    const pieData = (data?.salesByCategory ?? []).map((item) => ({
      name: item.categoryName,
      value: Number(item.totalSales) || 0,
    }));

    return {
      animationDuration: 1000,
      animationEasing: 'elasticOut',
      color: CHART_COLORS,
      tooltip: {
        trigger: 'item',
        backgroundColor: '#032b60',
        borderWidth: 0,
        textStyle: { color: '#fff', fontSize: 12 },
        formatter: '{b}<br/>${c} ({d}%)',
      },
      legend: {
        orient: 'vertical',
        right: 0,
        top: 'middle',
        itemWidth: 8,
        itemHeight: 8,
        textStyle: { color: '#43474f', fontSize: 10 },
      },
      series: [
        {
          name: 'Ventas por categoría',
          type: 'pie',
          radius: ['48%', '72%'],
          center: ['36%', '50%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          emphasis: {
            label: { show: true, fontSize: 11, fontWeight: 'bold' },
            scaleSize: 8,
          },
          labelLine: { show: false },
          data: pieData,
        },
      ],
    };
  }

  private formatChartDate(isoDate: string): string {
    const parsed = new Date(isoDate);
    if (Number.isNaN(parsed.getTime())) {
      return isoDate;
    }
    return parsed.toLocaleDateString('es-ES', { day: 'numeric', month: 'short' });
  }
}
