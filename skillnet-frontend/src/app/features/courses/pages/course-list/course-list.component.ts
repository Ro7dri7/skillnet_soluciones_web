import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ProducerCoursesService } from '../../../../core/services/producer-courses.service';
import { ProducerCourseSummary } from '../../../../shared/models/producer-course.model';
import { courseManagePath, normalizeCourseSlugForUrl } from '../../../../shared/utils/course-slug.util';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

@Component({
  selector: 'app-course-list',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './course-list.component.html',
})
export class CourseListComponent implements OnInit {
  private readonly producerCoursesService = inject(ProducerCoursesService);
  private readonly router = inject(Router);

  readonly courses = signal<ProducerCourseSummary[]>([]);
  readonly isLoading = signal(true);
  readonly error = signal('');
  readonly deletingId = signal<number | null>(null);

  ngOnInit(): void {
    this.loadCourses();
  }

  loadCourses(): void {
    this.isLoading.set(true);
    this.error.set('');

    this.producerCoursesService.getMyCourses().subscribe({
      next: (items) => {
        this.courses.set(items);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudieron cargar tus cursos.'));
        this.isLoading.set(false);
      },
    });
  }

  courseSlug(course: ProducerCourseSummary): string {
    return course.slug ? normalizeCourseSlugForUrl(course.slug) : String(course.id);
  }

  manageCourse(course: ProducerCourseSummary): void {
    void this.router.navigate([
      courseManagePath(this.courseSlug(course), 'curriculum', course.courseFormat),
    ]);
  }

  metricsCourse(course: ProducerCourseSummary): void {
    void this.router.navigate(['/infoproductor/student-progress'], {
      queryParams: { course: this.courseSlug(course) },
    });
  }

  enrollmentLabel(course: ProducerCourseSummary): string {
    const count = course.enrollmentCount ?? 0;
    const noun =
      course.courseFormat === 'course' || course.courseFormat === 'podcast'
        ? 'estudiantes'
        : 'ventas';
    return `${count} ${noun}`;
  }

  isDraft(status: string): boolean {
    return status.toLowerCase() === 'draft';
  }

  statusLabel(status: string): string {
    return this.isDraft(status) ? 'Borrador' : 'Publicado';
  }

  formatLabel(format: string): string {
    const labels: Record<string, string> = {
      course: 'Curso',
      ebook: 'Ebook',
      audiobook: 'Audiolibro',
      podcast: 'Podcast',
    };
    return labels[format] ?? format;
  }
}
