import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map, firstValueFrom } from 'rxjs';
import { CourseService } from '../../../core/services/course.service';
import { CourseBuilderService } from '../../../core/services/course-builder.service';
import { CourseManageContextService } from '../../../core/services/course-manage-context.service';
import { ManageCurriculumService } from '../../../core/services/manage-curriculum.service';
import { ManageLayoutSaveService } from '../../../core/services/manage-layout-save.service';
import { ProducerCoursesService } from '../../../core/services/producer-courses.service';
import { ToastService } from '../../../core/services/toast.service';
import { ActionFeedbackButtonComponent } from '../../../shared/components/action-feedback-button/action-feedback-button.component';
import type { ActionFeedbackPhase } from '../../../shared/components/action-feedback-button/action-feedback-button.component';
import { courseManagePath, normalizeCourseSlugForUrl } from '../../../shared/utils/course-slug.util';
import { messageFromHttpError } from '../../../shared/utils/http-error.util';
import { MANAGE_NAV_SECTIONS } from '../data/manage-nav.data';

type CourseStatusLabel = 'BORRADOR' | 'PUBLICADO';

@Component({
  selector: 'app-course-manage-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ActionFeedbackButtonComponent],
  templateUrl: './course-manage-layout.component.html',
  styles: [
    `
      .publish-btn-spinner {
        width: 12px;
        height: 12px;
        border: 2px solid currentColor;
        border-top-color: transparent;
        border-radius: 50%;
        animation: publish-btn-spin 0.65s linear infinite;
        flex-shrink: 0;
      }

      @keyframes publish-btn-spin {
        to {
          transform: rotate(360deg);
        }
      }
    `,
  ],
})
export class CourseManageLayoutComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly courseService = inject(CourseService);
  private readonly producerCourses = inject(ProducerCoursesService);
  private readonly builder = inject(CourseBuilderService);
  private readonly toast = inject(ToastService);
  private readonly manageContext = inject(CourseManageContextService);
  readonly curriculum = inject(ManageCurriculumService);
  readonly manageSave = inject(ManageLayoutSaveService);

  readonly navSections = MANAGE_NAV_SECTIONS;

  readonly routeSlug = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('slug') ?? '')),
    { initialValue: '' },
  );

  readonly courseName = signal('Cargando...');
  readonly courseStatus = signal<CourseStatusLabel>('BORRADOR');
  readonly statusUpdating = signal(false);
  readonly statusFeedback = signal<{ target: CourseStatusLabel; phase: ActionFeedbackPhase } | null>(
    null,
  );
  private statusResetTimer: ReturnType<typeof setTimeout> | null = null;

  readonly sectionsComplete = signal<Record<string, boolean>>({
    audience: false,
    curriculum: false,
    basics: false,
    pricing: false,
    promotions: false,
    messages: false,
  });

  readonly manageBasePath = this.manageContext.manageBasePath;

  readonly isPublished = computed(() => this.courseStatus() === 'PUBLICADO');

  constructor() {
    effect(() => {
      const slugParam = this.routeSlug();
      if (!slugParam) {
        this.courseName.set('Curso sin identificador');
        return;
      }
      untracked(() => void this.resolveCourse(slugParam));
    });

    effect(() => {
      const hasCurriculum = this.curriculum.hasModules();
      this.sectionsComplete.update((s) => ({ ...s, curriculum: hasCurriculum }));
    });
  }

  goBack(): void {
    void this.router.navigate(['/courses']);
  }

  setStatus(status: CourseStatusLabel): void {
    if (status === this.courseStatus() || this.statusUpdating()) {
      return;
    }
    void this.persistStatus(status);
  }

  private async resolveCourse(slugParam: string): Promise<void> {
    this.courseName.set('Cargando...');
    try {
      if (/^\d+$/.test(slugParam)) {
        const course = await firstValueFrom(this.courseService.getCourse(Number(slugParam)));
        const canonicalSlug = normalizeCourseSlugForUrl(course.slug);
        const childPath = this.route.firstChild?.snapshot.url.map((s) => s.path).join('/') ?? '';
        await this.router.navigate(
          [courseManagePath(canonicalSlug, childPath || undefined)],
          { replaceUrl: true },
        );
        return;
      }

      const course = await firstValueFrom(this.courseService.getCourseBySlug(slugParam));
      const canonicalSlug = normalizeCourseSlugForUrl(course.slug);
      this.manageContext.setCourse(canonicalSlug, course.id);

      if (canonicalSlug !== slugParam) {
        const childPath = this.route.firstChild?.snapshot.url.map((s) => s.path).join('/') ?? '';
        await this.router.navigate(
          [courseManagePath(canonicalSlug, childPath || undefined)],
          { replaceUrl: true },
        );
        return;
      }

      this.curriculum.loadCurriculum(course.id);
      this.courseName.set(course.title);
      this.courseStatus.set(course.status === 'published' ? 'PUBLICADO' : 'BORRADOR');
      const hasBasics = Boolean(course.description?.trim()) && (course.price ?? 0) >= 0;
      this.sectionsComplete.update((s) => ({ ...s, basics: hasBasics }));
    } catch {
      this.courseName.set(slugParam);
      this.toast.error('No se pudo cargar el curso.');
    }
  }

  private async persistStatus(status: CourseStatusLabel): Promise<void> {
    const id = this.manageContext.courseId();
    if (id == null) {
      return;
    }

    const previous = this.courseStatus();
    this.statusUpdating.set(true);
    this.statusFeedback.set({ target: status, phase: 'saving' });
    this.courseStatus.set(status);

    try {
      await this.builder.ensureInfoproductorSession();
      const response = await firstValueFrom(
        status === 'PUBLICADO'
          ? this.producerCourses.publishCourse(id)
          : this.producerCourses.unpublishCourse(id),
      );
      const nextStatus = response.status === 'published' ? 'PUBLICADO' : 'BORRADOR';
      this.courseStatus.set(nextStatus);
      this.finishStatusFeedback(
        nextStatus,
        response.status === 'published' ? 'Curso publicado' : 'Curso guardado como borrador',
      );
    } catch (err) {
      this.courseStatus.set(previous);
      this.statusFeedback.set(null);
      this.toast.error(messageFromHttpError(err, 'No se pudo actualizar el estado del curso.'));
    } finally {
      this.statusUpdating.set(false);
    }
  }

  private finishStatusFeedback(status: CourseStatusLabel, message: string): void {
    this.statusFeedback.set({ target: status, phase: 'success' });
    this.toast.success(message);
    if (this.statusResetTimer) {
      clearTimeout(this.statusResetTimer);
    }
    this.statusResetTimer = setTimeout(() => this.statusFeedback.set(null), 2000);
  }

  statusButtonPhase(status: CourseStatusLabel): ActionFeedbackPhase {
    const feedback = this.statusFeedback();
    if (!feedback || feedback.target !== status) {
      return 'idle';
    }
    return feedback.phase;
  }

  statusButtonLabel(status: CourseStatusLabel, idleLabel: string): string {
    const phase = this.statusButtonPhase(status);
    if (phase === 'saving') {
      return 'Guardando…';
    }
    if (phase === 'success') {
      return status === 'PUBLICADO' ? 'Publicado' : 'Guardado';
    }
    return idleLabel;
  }

  onSave(): void {
    void this.manageSave.triggerSave();
  }

  isSectionComplete(sectionKey: string): boolean {
    return this.sectionsComplete()[sectionKey] ?? false;
  }
}
