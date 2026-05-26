import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map, firstValueFrom } from 'rxjs';
import { CourseService } from '../../../core/services/course.service';
import { CourseBuilderService } from '../../../core/services/course-builder.service';
import { ManageCurriculumService } from '../../../core/services/manage-curriculum.service';
import { ManageLayoutSaveService } from '../../../core/services/manage-layout-save.service';
import { ProducerCoursesService } from '../../../core/services/producer-courses.service';
import { ToastService } from '../../../core/services/toast.service';
import { ActionFeedbackButtonComponent } from '../../../shared/components/action-feedback-button/action-feedback-button.component';
import type { ActionFeedbackPhase } from '../../../shared/components/action-feedback-button/action-feedback-button.component';
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
  readonly curriculum = inject(ManageCurriculumService);
  readonly manageSave = inject(ManageLayoutSaveService);

  readonly navSections = MANAGE_NAV_SECTIONS;

  readonly courseId = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('id') ?? '')),
    { initialValue: '' },
  );

  readonly courseName = signal('Cargando...');
  readonly courseStatus = signal<CourseStatusLabel>('BORRADOR');
  readonly statusUpdating = signal(false);
  readonly statusFeedback = signal<{ target: CourseStatusLabel; phase: ActionFeedbackPhase } | null>(
    null,
  );
  private statusResetTimer: ReturnType<typeof setTimeout> | null = null;

  /** Simulación de secciones completadas (fase posterior: datos reales). */
  readonly sectionsComplete = signal<Record<string, boolean>>({
    audience: false,
    curriculum: false,
    basics: false,
    pricing: false,
    promotions: false,
    messages: false,
  });

  readonly manageBasePath = computed(() => {
    const id = this.courseId();
    return id ? `/instructor/courses/${id}/manage` : '/courses';
  });

  readonly isPublished = computed(() => this.courseStatus() === 'PUBLICADO');

  constructor() {
    effect(() => {
      const id = this.courseId();
      if (!id || Number.isNaN(Number(id))) {
        this.courseName.set('Curso sin ID');
        return;
      }
      untracked(() => {
        this.curriculum.loadCurriculum(Number(id));
        this.courseName.set('Cargando...');
        this.courseService.getCourse(Number(id)).subscribe({
          next: (course) => {
            this.courseName.set(course.title);
            this.courseStatus.set(
              course.status === 'published' ? 'PUBLICADO' : 'BORRADOR',
            );
            const hasBasics =
              Boolean(course.description?.trim()) && (course.price ?? 0) >= 0;
            this.sectionsComplete.update((s) => ({ ...s, basics: hasBasics }));
          },
          error: () => {
            this.courseName.set(`Curso #${id}`);
          },
        });
      });
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

  private async persistStatus(status: CourseStatusLabel): Promise<void> {
    const id = Number(this.courseId());
    if (Number.isNaN(id)) {
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
