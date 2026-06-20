import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CourseBuilderService } from '../../../../core/services/course-builder.service';
import { CourseService } from '../../../../core/services/course.service';
import { courseManagePath, normalizeCourseSlugForUrl } from '../../../../shared/utils/course-slug.util';

/** Redirige rutas legacy del wizard a la shell unificada de gestión. */
@Component({
  selector: 'app-builder-manage-redirect',
  standalone: true,
  template: `<p class="p-8 text-sm text-skillnet-muted">Redirigiendo…</p>`,
})
export class BuilderManageRedirectComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly builder = inject(CourseBuilderService);
  private readonly courseService = inject(CourseService);

  ngOnInit(): void {
    void this.redirect();
  }

  private async redirect(): Promise<void> {
    const step = this.route.snapshot.data['manageStep'] as string | undefined;
    const targetStep = step ?? 'audience';

    try {
      const courseId = await this.builder.ensureCourseId();
      const draftSlug = this.builder.state().courseSlug;
      let slug = draftSlug ? normalizeCourseSlugForUrl(draftSlug) : '';
      if (!slug) {
        const course = await firstValueFrom(this.courseService.getCourse(courseId));
        slug = normalizeCourseSlugForUrl(course.slug);
        this.builder.setCourseId(courseId, slug);
      }
      await this.router.navigate([
        courseManagePath(slug, targetStep, this.builder.state().productType),
      ]);
    } catch {
      await this.router.navigate(['/infoproductor/courses/new/subcategory']);
    }
  }
}
