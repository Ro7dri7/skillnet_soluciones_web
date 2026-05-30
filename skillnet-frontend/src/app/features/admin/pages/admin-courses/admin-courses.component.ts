import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
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

  ngOnInit(): void {
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

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      published: 'Publicado',
      draft: 'Borrador',
      deleted: 'Eliminado',
    };
    return map[status] ?? status;
  }
}
