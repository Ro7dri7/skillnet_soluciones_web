import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { TrafficService } from '../../../../core/services/traffic.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

@Component({
  selector: 'app-producer-traffic',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './producer-traffic.component.html',
  styleUrl: './producer-traffic.component.scss',
})
export class ProducerTrafficComponent implements OnInit {
  private readonly trafficService = inject(TrafficService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly configured = signal(false);
  readonly pageViews = signal<{ date: string; value: number }[]>([]);
  readonly uniqueVisitors = signal<{ date: string; value: number }[]>([]);

  readonly totalPageViews = computed(() =>
    this.pageViews().reduce((sum, p) => sum + p.value, 0),
  );

  readonly totalVisitors = computed(() =>
    this.uniqueVisitors().reduce((sum, p) => sum + p.value, 0),
  );

  ngOnInit(): void {
    this.trafficService.getAnalytics().subscribe({
      next: (data) => {
        this.configured.set(data.configured);
        this.pageViews.set(data.pageViews ?? []);
        this.uniqueVisitors.set(data.uniqueVisitors ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudieron cargar las analíticas.'));
        this.loading.set(false);
      },
    });
  }
}
