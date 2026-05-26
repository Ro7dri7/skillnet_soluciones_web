import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CourseBuilderService } from '../../../../core/services/course-builder.service';

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

  ngOnInit(): void {
    void this.redirect();
  }

  private async redirect(): Promise<void> {
    const step = this.route.snapshot.data['manageStep'] as string | undefined;
    const targetStep = step ?? 'audience';

    try {
      const courseId = await this.builder.ensureCourseId();
      await this.router.navigate(['/instructor/courses', courseId, 'manage', targetStep]);
    } catch {
      await this.router.navigate(['/infoproductor/courses/new/subcategory']);
    }
  }
}
