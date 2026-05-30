import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { AdminService } from '../../../../core/services/admin.service';
import { CourseResponse } from '../../../../shared/models/course.model';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

@Component({
  selector: 'app-admin-courses',
  standalone: true,
  imports: [CurrencyPipe],
  templateUrl: './admin-courses.component.html',
  styleUrl: './admin-courses.component.scss',
})
export class AdminCoursesComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly courses = signal<CourseResponse[]>([]);
  readonly actionCourseId = signal<number | null>(null);
  readonly toast = signal<string | null>(null);

  ngOnInit(): void {
    this.loadCourses();
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      published: 'Publicado',
      draft: 'Borrador',
      deleted: 'Dado de baja',
    };
    return map[status] ?? status;
  }

  publishCourse(course: CourseResponse): void {
    if (!confirm(`¿Publicar "${course.title}" en el marketplace?`)) {
      return;
    }
    this.runAction(course.id, () => this.adminService.publishCourse(course.id), 'Curso publicado.');
  }

  setDraft(course: CourseResponse): void {
    if (!confirm(`¿Mover "${course.title}" a borrador? Dejará de mostrarse en el marketplace.`)) {
      return;
    }
    this.runAction(course.id, () => this.adminService.setCourseDraft(course.id), 'Curso en borrador.');
  }

  takedownCourse(course: CourseResponse): void {
    if (
      !confirm(
        `¿Dar de baja "${course.title}"?\n\nNo aparecerá en el marketplace. Los alumnos matriculados conservan acceso.`,
      )
    ) {
      return;
    }
    this.runAction(course.id, () => this.adminService.takedownCourse(course.id), 'Curso dado de baja.');
  }

  isBusy(courseId: number): boolean {
    return this.actionCourseId() === courseId;
  }

  canPublish(course: CourseResponse): boolean {
    return course.status !== 'published';
  }

  canDraft(course: CourseResponse): boolean {
    return course.status === 'published';
  }

  canTakedown(course: CourseResponse): boolean {
    return course.status !== 'deleted';
  }

  private loadCourses(): void {
    this.loading.set(true);
    this.error.set(null);
    this.adminService.getCourses().subscribe({
      next: (rows) => {
        this.courses.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudieron cargar los cursos.'));
        this.loading.set(false);
      },
    });
  }

  private runAction(
    courseId: number,
    request: () => Observable<CourseResponse>,
    successMessage: string,
  ): void {
    this.actionCourseId.set(courseId);
    this.toast.set(null);
    request().subscribe({
      next: (updated) => {
        this.courses.update((rows) => rows.map((row) => (row.id === updated.id ? updated : row)));
        this.actionCourseId.set(null);
        this.toast.set(successMessage);
      },
      error: (err) => {
        this.actionCourseId.set(null);
        this.toast.set(messageFromHttpError(err, 'No se pudo actualizar el curso.'));
      },
    });
  }
}
