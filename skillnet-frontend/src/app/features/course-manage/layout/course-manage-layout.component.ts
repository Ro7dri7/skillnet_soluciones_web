import { Component, computed, effect, inject, OnDestroy, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationStart, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map, firstValueFrom } from 'rxjs';

import { CourseService } from '../../../core/services/course.service';

import { CourseBuilderService } from '../../../core/services/course-builder.service';

import { CourseManageContextService } from '../../../core/services/course-manage-context.service';

import { ManageCurriculumService } from '../../../core/services/manage-curriculum.service';

import { ManageLayoutSaveService } from '../../../core/services/manage-layout-save.service';

import { ProducerCoursesService } from '../../../core/services/producer-courses.service';

import { ProfileService } from '../../../core/services/profile.service';

import { ToastService } from '../../../core/services/toast.service';

import { ActionFeedbackButtonComponent } from '../../../shared/components/action-feedback-button/action-feedback-button.component';

import type { ActionFeedbackPhase } from '../../../shared/components/action-feedback-button/action-feedback-button.component';

import type { CourseResponse } from '../../../shared/models/course.model';

import {

  courseManagePath,
  courseLandingPath,
  courseRouteNeedsRedirect,

  normalizeCourseSlugForUrl,

  slugFromRouteParams,

} from '../../../shared/utils/course-slug.util';

import { messageFromHttpError } from '../../../shared/utils/http-error.util';

import { getPublishBlockers } from '../../../shared/utils/publish-blockers.util';

import { MANAGE_NAV_SECTIONS } from '../data/manage-nav.data';



type CourseStatusLabel = 'draft' | 'published';



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

  private readonly profileService = inject(ProfileService);

  private readonly toast = inject(ToastService);

  private readonly manageContext = inject(CourseManageContextService);

  readonly curriculum = inject(ManageCurriculumService);

  readonly manageSave = inject(ManageLayoutSaveService);



  readonly navSections = MANAGE_NAV_SECTIONS;



  readonly routeSlug = toSignal(

    this.route.paramMap.pipe(

      map((params) => slugFromRouteParams(params.get('format'), params.get('slug'))),

    ),

    { initialValue: '' },

  );



  readonly routeFormatParam = toSignal(

    this.route.paramMap.pipe(map((params) => params.get('format'))),

    { initialValue: null as string | null },

  );



  readonly routeSlugStem = toSignal(

    this.route.paramMap.pipe(map((params) => params.get('slug'))),

    { initialValue: null as string | null },

  );



  readonly manageNavLink = (section: string) => this.manageContext.manageNavLink(section);



  readonly courseName = signal('Cargando...');

  readonly courseStatus = signal<CourseStatusLabel>('draft');

  readonly loadError = signal<'not_found' | 'server' | null>(null);

  readonly statusUpdating = signal(false);

  readonly statusFeedback = signal<{ target: CourseStatusLabel; phase: ActionFeedbackPhase } | null>(

    null,

  );

  private statusResetTimer: ReturnType<typeof setTimeout> | null = null;

  private resolveGeneration = 0;

  private instructorProfile = signal<{ bio?: string | null; profilePicture?: string | null } | null>(
    null,
  );

  readonly manageBasePath = this.manageContext.manageBasePath;

  readonly isPublished = computed(() => this.courseStatus() === 'published');

  readonly statusBadgeLabel = computed(() =>
    this.isPublished() ? 'Publicado' : 'Borrador',
  );

  readonly previewLandingUrl = computed(() => {
    const slug = this.manageContext.courseSlug();
    if (!slug) {
      return null;
    }
    return `${courseLandingPath(slug, this.manageContext.courseFormat())}?instructorPreviewMode=guest`;
  });

  readonly previewLearnUrl = computed(() => {
    const slug = this.manageContext.courseSlug();
    if (!slug) {
      return null;
    }
    const base = courseLandingPath(slug, this.manageContext.courseFormat());
    return `${base.replace(/\/$/, '')}/learn?instructorPreviewMode=student`;
  });

  constructor() {

    effect(() => {

      this.manageContext.syncRouteParams(this.routeFormatParam(), this.routeSlugStem());

    });



    effect(() => {

      const slugParam = this.routeSlug();

      if (!slugParam) {

        this.courseName.set('Producto sin identificador');

        return;

      }

      untracked(() => void this.resolveCourse(slugParam));

    });



    effect(() => {
      const hasCurriculum = this.curriculum.hasModules();
      this.manageContext.setSectionStatus('curriculum', hasCurriculum);
    });

    this.router.events
      .pipe(filter((event) => event instanceof NavigationStart))
      .subscribe(() => {
        if (this.manageSave.showSaveButton()) {
          void this.manageSave.triggerSave();
        }
      });

    void this.loadInstructorProfile();

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



  private async loadInstructorProfile(): Promise<void> {

    try {

      const profile = await firstValueFrom(this.profileService.getMe());

      this.instructorProfile.set({

        bio: (profile as { bio?: string }).bio ?? null,

        profilePicture: profile.profilePicture ?? null,

      });

    } catch {

      this.instructorProfile.set(null);

    }

  }



  private async resolveCourse(slugParam: string): Promise<void> {

    const generation = ++this.resolveGeneration;

    this.loadError.set(null);

    this.courseName.set('Cargando...');



    try {

      if (/^\d+$/.test(slugParam)) {

        const course = await firstValueFrom(this.courseService.getCourse(Number(slugParam)));

        if (generation !== this.resolveGeneration) return;



        const canonicalSlug = normalizeCourseSlugForUrl(course.slug);

        const childPath = this.route.firstChild?.snapshot.url.map((s) => s.path).join('/') ?? '';

        await this.router.navigate(

          [courseManagePath(canonicalSlug, childPath || undefined, course.courseFormat)],

          { replaceUrl: true },

        );

        return;

      }



      const formatParam = this.routeFormatParam();

      const course = await firstValueFrom(

        this.courseService.getCourseBySlug(slugParam, formatParam),

      );

      if (generation !== this.resolveGeneration) return;



      const canonicalSlug = normalizeCourseSlugForUrl(course.slug);

      this.manageContext.setCourse(canonicalSlug, course.id, course.courseFormat);



      const routeFormat = this.route.snapshot.paramMap.get('format');

      const stemParam = this.route.snapshot.paramMap.get('slug');

      const childPath = this.route.firstChild?.snapshot.url.map((s) => s.path).join('/') ?? '';



      if (courseRouteNeedsRedirect(routeFormat, stemParam, canonicalSlug, course.courseFormat)) {

        await this.router.navigate(

          [courseManagePath(canonicalSlug, childPath || undefined, course.courseFormat)],

          { replaceUrl: true },

        );

        if (generation !== this.resolveGeneration) return;

      }



      this.applyLoadedCourse(course);

    } catch (err) {

      if (generation !== this.resolveGeneration) return;

      this.manageContext.setLoadedCourse(null);

      this.courseName.set('Producto no encontrado');

      const message = messageFromHttpError(err, '');

      this.loadError.set(message.toLowerCase().includes('not found') ? 'not_found' : 'server');

      this.toast.error('No se pudo cargar el producto.');

    }

  }



  private applyLoadedCourse(course: CourseResponse): void {

    this.manageContext.setLoadedCourse(course);

    this.curriculum.loadCurriculum(course.id);

    this.courseName.set(course.title?.trim() || 'Producto sin título');

    this.courseStatus.set(course.status === 'published' ? 'published' : 'draft');
  }

  private async persistStatus(status: CourseStatusLabel): Promise<void> {

    const id = this.manageContext.courseId();

    if (id == null) {

      this.toast.error('El producto aún no está cargado.');

      return;

    }



    if (status === 'published') {

      const course = this.manageContext.loadedCourse();

      const blockers = getPublishBlockers(

        this.instructorProfile(),

        course,

        this.manageBasePath(),

        { curriculumComplete: this.manageContext.sectionStatus()['curriculum'] },

      );

      if (blockers.length > 0) {

        const noun =

          course?.courseFormat?.toLowerCase() === 'ebook'

            ? 'ebook'

            : course?.courseFormat?.toLowerCase() === 'podcast'

              ? 'podcast'

              : 'curso';

        window.alert(

          `No puedes publicar el ${noun} hasta completar:\n\n` +

            blockers.map((b) => `• ${b.text}`).join('\n'),

        );

        return;

      }



      if (!this.manageContext.sectionStatus()['curriculum']) {

        const noun =

          course?.courseFormat?.toLowerCase() === 'ebook'

            ? 'ebook'

            : course?.courseFormat?.toLowerCase() === 'podcast'

              ? 'podcast'

              : 'curso';

        window.alert(`No puedes publicar un ${noun} sin contenido válido en el temario.`);

        return;

      }



      if (this.manageSave.showSaveButton()) {

        const saved = await this.manageSave.triggerSave();

        if (!saved) {

          return;

        }

      }

    }



    const previous = this.courseStatus();

    this.statusUpdating.set(true);

    this.statusFeedback.set({ target: status, phase: 'saving' });

    this.courseStatus.set(status);



    try {

      await this.builder.ensureInfoproductorSession();

      const response = await firstValueFrom(

        status === 'published'

          ? this.producerCourses.publishCourse(id)

          : this.producerCourses.unpublishCourse(id),

      );

      const nextStatus: CourseStatusLabel =

        response.status === 'published' ? 'published' : 'draft';

      this.courseStatus.set(nextStatus);

      this.manageContext.patchLoadedCourse({ status: response.status });

      this.finishStatusFeedback(

        nextStatus,

        nextStatus === 'published' ? 'Producto publicado' : 'Producto guardado como borrador',

      );

    } catch (err) {

      this.courseStatus.set(previous);

      this.statusFeedback.set(null);

      this.toast.error(messageFromHttpError(err, 'No se pudo actualizar el estado del producto.'));

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

      return status === 'published' ? 'Publicado' : 'Guardado';

    }

    return idleLabel;

  }



  onSave(): void {

    void this.manageSave.triggerSave();

  }



  isSectionComplete(sectionKey: string): boolean {
    return this.manageContext.sectionStatus()[sectionKey] ?? false;
  }

}

