import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CourseService } from '../../../../core/services/course.service';
import { CourseResponse } from '../../../../shared/models/course.model';

@Component({
  selector: 'app-course-landing',
  standalone: true,
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './course-landing.component.html',
})
export class CourseLandingComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly courseService = inject(CourseService);

  readonly course = signal<CourseResponse | null>(null);
  readonly isLoading = signal(true);
  readonly notFound = signal(false);

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (!slug) {
      this.notFound.set(true);
      this.isLoading.set(false);
      return;
    }
    this.courseService.getCourses().subscribe({
      next: (items) => {
        const found = items.find((c) => c.slug === slug) ?? null;
        this.course.set(found);
        this.notFound.set(!found);
        this.isLoading.set(false);
      },
      error: () => {
        this.notFound.set(true);
        this.isLoading.set(false);
      },
    });
  }

  levelLabel(level: string): string {
    const map: Record<string, string> = {
      beginner: 'Principiante',
      intermediate: 'Intermedio',
      advanced: 'Avanzado',
    };
    return map[level] ?? level;
  }
}
