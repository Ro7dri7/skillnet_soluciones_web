import { Injectable, computed, signal } from '@angular/core';
import { courseManagePath, normalizeCourseSlugForUrl } from '../../shared/utils/course-slug.util';

/** Estado compartido del editor de curso (slug en URL, id numérico para API). */
@Injectable({ providedIn: 'root' })
export class CourseManageContextService {
  readonly courseSlug = signal('');
  readonly courseId = signal<number | null>(null);

  readonly manageBasePath = computed(() => {
    const slug = this.courseSlug();
    return slug ? courseManagePath(slug) : '/courses';
  });

  setCourse(slug: string, id: number): void {
    this.courseSlug.set(normalizeCourseSlugForUrl(slug));
    this.courseId.set(id);
  }

  clear(): void {
    this.courseSlug.set('');
    this.courseId.set(null);
  }

  requireCourseId(): number {
    const id = this.courseId();
    if (id == null) {
      throw new Error('Curso no cargado en el contexto de gestión.');
    }
    return id;
  }
}
