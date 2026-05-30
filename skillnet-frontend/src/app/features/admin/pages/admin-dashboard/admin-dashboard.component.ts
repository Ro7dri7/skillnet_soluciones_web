import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import type { EChartsCoreOption } from 'echarts/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import { AuthService } from '../../../../core/services/auth.service';
import {
  AdminDashboardData,
  AdminPeriod,
  AdminService,
  AdminView,
} from '../../../../core/services/admin.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, NgxEchartsDirective],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly data = signal<AdminDashboardData | null>(null);

  readonly period = signal<AdminPeriod>('semana');
  readonly view = signal<AdminView>('resumen');
  readonly customStart = signal('');
  readonly customEnd = signal('');

  readonly viewOptions: { value: AdminView; label: string }[] = [
    { value: 'resumen', label: 'Resumen' },
    { value: 'ventas', label: 'Ventas' },
    { value: 'cursos', label: 'Cursos' },
    { value: 'usuarios', label: 'Usuarios' },
    { value: 'titulados', label: 'Titulados' },
  ];

  readonly periodOptions: { value: AdminPeriod; label: string }[] = [
    { value: 'dia', label: 'Día' },
    { value: 'semana', label: 'Semana' },
    { value: 'mes', label: 'Mes' },
    { value: 'anio', label: 'Año' },
    { value: 'personalizado', label: 'Personalizado' },
  ];

  readonly greeting = computed(() => {
    const user = this.authService.getCurrentUser();
    const name = [user?.firstName, user?.lastName].filter(Boolean).join(' ').trim();
    return name || user?.username || 'Admin';
  });

  readonly revenueChart = computed<EChartsCoreOption>(() => {
    const d = this.data();
    if (!d) return {};
    return {
      tooltip: { trigger: 'axis' },
      legend: { top: 0, textStyle: { fontSize: 11 } },
      grid: { left: 40, right: 16, bottom: 24, top: 36 },
      xAxis: { type: 'category', data: d.revenueSeries.map((p) => p.label) },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2ff' } } },
      series: [
        {
          name: 'Actual',
          type: 'bar',
          data: d.revenueSeries.map((p) => p.current),
          itemStyle: { color: '#032b60', borderRadius: [4, 4, 0, 0] },
          barMaxWidth: 28,
        },
        {
          name: 'Anterior',
          type: 'bar',
          data: d.revenueSeries.map((p) => p.previous),
          itemStyle: { color: '#d0dbff', borderRadius: [4, 4, 0, 0] },
          barMaxWidth: 28,
        },
      ],
    };
  });

  readonly usersChart = computed<EChartsCoreOption>(() => {
    const d = this.data();
    if (!d) return {};
    return {
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 16, bottom: 24, top: 16 },
      xAxis: { type: 'category', data: d.usersSeries.map((p) => p.label) },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2ff' } } },
      series: [
        {
          name: 'Registros',
          type: 'line',
          smooth: true,
          data: d.usersSeries.map((p) => p.current),
          lineStyle: { color: '#145bff', width: 2 },
          areaStyle: { color: 'rgba(20, 91, 255, 0.12)' },
          itemStyle: { color: '#145bff' },
        },
      ],
    };
  });

  ngOnInit(): void {
    this.load();
  }

  setPeriod(p: AdminPeriod): void {
    this.period.set(p);
    this.load();
  }

  onViewChange(): void {
    this.load();
  }

  applyCustomRange(): void {
    if (this.customStart() && this.customEnd()) {
      this.period.set('personalizado');
      this.load();
    }
  }

  clearFilters(): void {
    this.period.set('semana');
    this.view.set('resumen');
    this.customStart.set('');
    this.customEnd.set('');
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.adminService
      .getDashboard(
        this.period(),
        this.view(),
        this.customStart() || undefined,
        this.customEnd() || undefined,
      )
      .subscribe({
        next: (payload) => {
          this.data.set(payload);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(messageFromHttpError(err, 'No se pudo cargar el dashboard admin.'));
          this.loading.set(false);
        },
      });
  }

  pillClass(direction: string): string {
    if (direction === 'up') return 'pill up';
    if (direction === 'down') return 'pill down';
    return 'pill neu';
  }

  statusClass(status: string): string {
    if (status === 'Completado') return 'bdg g';
    if (status === 'Pendiente') return 'bdg a';
    return 'bdg r';
  }
}
