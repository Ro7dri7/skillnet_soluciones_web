import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { StudentService, MyCourse } from '../../../../core/services/student.service';
import { courseLearnPath, normalizeCourseSlugForUrl } from '../../../../shared/utils/course-slug.util';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

@Component({
  selector: 'app-my-products',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './my-products.component.html',
  styleUrl: './my-products.component.scss',
})
export class MyProductsComponent implements OnInit {
  private readonly studentService = inject(StudentService);

  readonly courses = signal<MyCourse[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.studentService.getMyCourses().subscribe({
      next: (items) => {
        this.courses.set(items);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudieron cargar tus cursos.'));
        this.loading.set(false);
      },
    });
  }

  courseRoute(slug: string): string {
    return courseLearnPath(slug);
  }
}
