import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { filter, map, startWith, firstValueFrom } from 'rxjs';
import { CourseBuilderShellService } from '../../../core/services/course-builder-shell.service';
import { CourseBuilderService } from '../../../core/services/course-builder.service';
import { ProducerCoursesService } from '../../../core/services/producer-courses.service';
import { ToastService } from '../../../core/services/toast.service';
import { ActionFeedbackButtonComponent } from '../../../shared/components/action-feedback-button/action-feedback-button.component';
import type { ActionFeedbackPhase } from '../../../shared/components/action-feedback-button/action-feedback-button.component';
import { messageFromHttpError } from '../../../shared/utils/http-error.util';
import {
  courseManagePath,
  normalizeCourseSlugForUrl,
  slugifyCourseTitle,
} from '../../../shared/utils/course-slug.util';
import {
  BUILDER_SHELL_NAV,
  BuilderShellNavItem,
} from '../data/builder-shell-nav.data';
import { COURSE_NEW_BASE } from '../data/builder-steps.data';

const WIZARD_STEPS = new Set(['title', 'category', 'subcategory', 'audience', 'curriculum']);
const MANAGE_LINKS = new Set(['basics', 'pricing', 'curriculum', 'promotions', 'messages']);
const REQUIRES_COURSE_ID = new Set(['basics', 'pricing', 'promotions', 'messages']);

type PublishStatus = 'BORRADOR' | 'PUBLICADO';

function hasMultilineContent(text: string): boolean {
  if (!text) {
    return false;
  }
  return text.split('\n').some((line) => line.trim() !== '');
}

@Component({
  selector: 'app-course-builder-shell-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ActionFeedbackButtonComponent],
  templateUrl: './course-builder-shell-layout.component.html',
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
export class CourseBuilderShellLayoutComponent {
  readonly shell = inject(CourseBuilderShellService);
  private readonly builder = inject(CourseBuilderService);
  private readonly producerCourses = inject(ProducerCoursesService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  readonly navSections = BUILDER_SHELL_NAV;
  readonly publishStatus = signal<PublishStatus>('BORRADOR');
  readonly statusUpdating = signal(false);
  readonly statusFeedback = signal<{ target: PublishStatus; phase: ActionFeedbackPhase } | null>(
    null,
  );
  private statusResetTimer: ReturnType<typeof setTimeout> | null = null;

  readonly isPublished = computed(() => this.publishStatus() === 'PUBLICADO');

  private readonly currentPath = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.router.url.split('?')[0]),
      startWith(this.router.url.split('?')[0]),
    ),
    { initialValue: this.router.url.split('?')[0] },
  );

  readonly courseTitle = computed(() => this.builder.title());
  readonly courseId = computed(() => this.builder.state().courseId);

  readonly courseManageBasicsLink = computed(() => {
    const id = this.courseId();
    if (!id) {
      return null;
    }
    const slug =
      this.builder.state().courseSlug ?? slugifyCourseTitle(this.builder.title() || 'curso');
    return courseManagePath(normalizeCourseSlugForUrl(slug), 'basics', this.builder.state().productType);
  });

  readonly sectionsComplete = computed(() => {
    const d = this.builder.state();
    return {
      audience:
        hasMultilineContent(d.whatYouWillLearn) && hasMultilineContent(d.targetAudience),
      curriculum: d.curriculum.length > 0,
      basics: false,
      pricing: false,
      promotions: false,
      messages: false,
    };
  });

  setPublishStatus(status: PublishStatus): void {
    if (status === this.publishStatus() || this.statusUpdating()) {
      return;
    }
    void this.persistPublishStatus(status);
  }

  private async persistPublishStatus(status: PublishStatus): Promise<void> {
    const id = this.courseId();
    const previous = this.publishStatus();

    this.statusUpdating.set(true);
    this.statusFeedback.set({ target: status, phase: 'saving' });
    this.publishStatus.set(status);

    if (!id) {
      this.finishStatusFeedback(status, status === 'PUBLICADO' ? 'Curso marcado como publicado' : 'Guardado como borrador');
      return;
    }

    try {
      await this.builder.ensureInfoproductorSession();
      const response = await firstValueFrom(
        status === 'PUBLICADO'
          ? this.producerCourses.publishCourse(id)
          : this.producerCourses.unpublishCourse(id),
      );
      this.publishStatus.set(response.status === 'published' ? 'PUBLICADO' : 'BORRADOR');
      this.finishStatusFeedback(
        this.publishStatus(),
        response.status === 'published' ? 'Curso publicado' : 'Curso guardado como borrador',
      );
    } catch (err) {
      this.publishStatus.set(previous);
      this.statusFeedback.set(null);
      this.toast.error(messageFromHttpError(err, 'No se pudo actualizar el estado del curso.'));
    } finally {
      this.statusUpdating.set(false);
    }
  }

  private finishStatusFeedback(status: PublishStatus, message: string): void {
    this.statusUpdating.set(false);
    this.statusFeedback.set({ target: status, phase: 'success' });
    this.toast.success(message);
    if (this.statusResetTimer) {
      clearTimeout(this.statusResetTimer);
    }
    this.statusResetTimer = setTimeout(() => this.statusFeedback.set(null), 2000);
  }

  statusButtonPhase(status: PublishStatus): ActionFeedbackPhase {
    const feedback = this.statusFeedback();
    if (!feedback || feedback.target !== status) {
      return 'idle';
    }
    return feedback.phase;
  }

  statusButtonLabel(status: PublishStatus, idleLabel: string): string {
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
    void this.shell.triggerSave();
  }

  resolveNavLink(item: BuilderShellNavItem): string[] {
    const id = this.courseId();
    if (id && MANAGE_LINKS.has(item.wizardPath)) {
      const slug =
        this.builder.state().courseSlug ??
        slugifyCourseTitle(this.builder.title() || 'curso');
      return [
        courseManagePath(
          normalizeCourseSlugForUrl(slug),
          item.managePath,
          this.builder.state().productType,
        ),
      ];
    }
    return [`${COURSE_NEW_BASE}/${item.wizardPath}`];
  }

  isNavEnabled(item: BuilderShellNavItem): boolean {
    if (item.wizardPath === 'curriculum') {
      return true;
    }
    const id = this.courseId();
    if (REQUIRES_COURSE_ID.has(item.wizardPath)) {
      return id !== null;
    }
    return WIZARD_STEPS.has(item.wizardPath);
  }

  isActive(item: BuilderShellNavItem): boolean {
    const path = this.currentPath();
    return path.endsWith(`/${item.wizardPath}`);
  }

  isSectionComplete(sectionKey: string): boolean {
    const liveStatus = this.shell.sectionsStatus()[sectionKey];
    if (liveStatus !== undefined) {
      return liveStatus;
    }
    const status = this.sectionsComplete();
    return status[sectionKey as keyof typeof status] ?? false;
  }
}
