import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CourseService } from '../../../../core/services/course.service';
import { AuthService } from '../../../../core/services/auth.service';
import { CourseResponse } from '../../../../shared/models/course.model';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

@Component({
  selector: 'app-course-list',
  standalone: true,
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './course-list.component.html',
})
export class CourseListComponent implements OnInit {
  private readonly courseService = inject(CourseService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly courses = signal<CourseResponse[]>([]);
  readonly isLoading = signal(true);
  readonly error = signal('');
  readonly deletingId = signal<number | null>(null);

  ngOnInit(): void {
    this.loadCourses();
  }

  loadCourses(): void {
    this.isLoading.set(true);
    this.error.set('');

    this.courseService.getCourses().subscribe({
      next: (items) => {
        const user = this.authService.getCurrentUser();
        const filtered =
          user?.role === 'infoproductor' && user.id
            ? items.filter((c) => c.professorId === user.id)
            : items;
        this.courses.set(filtered);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudieron cargar los cursos.'));
        this.isLoading.set(false);
      },
    });
  }

  editCourse(id: number): void {
    void this.router.navigate(['/courses', id, 'edit']);
  }

  deleteCourse(id: number): void {
    if (!confirm('¿Eliminar este curso? Esta acción no se puede deshacer.')) {
      return;
    }

    this.deletingId.set(id);
    this.courseService.deleteCourse(id).subscribe({
      next: () => {
        this.courses.update((list) => list.filter((c) => c.id !== id));
        this.deletingId.set(null);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo eliminar el curso.'));
        this.deletingId.set(null);
      },
    });
  }

  statusLabel(status: string): string {
    return status === 'published' ? 'Publicado' : 'Borrador';
  }

  levelLabel(level: string): string {
    const labels: Record<string, string> = {
      beginner: 'Principiante',
      intermediate: 'Intermedio',
      advanced: 'Avanzado',
    };
    return labels[level] ?? level;
  }
}
