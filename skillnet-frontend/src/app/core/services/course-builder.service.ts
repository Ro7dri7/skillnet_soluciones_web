import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  canAssumeInfoproductorRole,
  isInfoproductorRole,
  resolveUserRole,
} from '../../shared/utils/user-role.util';
import { AuthService } from './auth.service';
import {
  BuilderLessonDraft,
  BuilderModuleDraft,
  CourseBuilderDraft,
  CourseBuilderRequest,
  LessonType,
  ProductType,
} from '../../shared/models/course-builder.model';
import { CourseBuilderApiService } from './course-builder-api.service';
import { ProducerCoursesService } from './producer-courses.service';
import { environment } from '../../../environments/environment';
import type { CreateCourseRequest } from '../../shared/models/producer-course.model';

function newClientId(): string {
  return `cb-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
}

const EMPTY_DRAFT: CourseBuilderDraft = {
  productType: null,
  productLabel: '',
  title: '',
  category: '',
  subcategory: '',
  whatYouWillLearn: '',
  targetAudience: '',
  curriculum: [],
  courseId: null,
};

const BUILDER_COURSE_ID_KEY = 'skillnet_builder_course_id';
const DEFAULT_DRAFT_TITLE = 'Nuevo curso';

@Injectable({ providedIn: 'root' })
export class CourseBuilderService {
  private readonly api = inject(CourseBuilderApiService);
  private readonly producerCourses = inject(ProducerCoursesService);
  private readonly auth = inject(AuthService);

  private readonly draft = signal<CourseBuilderDraft>({
    ...EMPTY_DRAFT,
    courseId: this.readStoredCourseId(),
  });

  readonly state = this.draft.asReadonly();

  readonly productType = computed(() => this.draft().productType);
  readonly title = computed(() => this.draft().title);
  readonly category = computed(() => this.draft().category);
  readonly subcategory = computed(() => this.draft().subcategory);
  readonly whatYouWillLearn = computed(() => this.draft().whatYouWillLearn);
  readonly targetAudience = computed(() => this.draft().targetAudience);
  readonly curriculum = computed(() => this.draft().curriculum);
  readonly productLabel = computed(() => this.draft().productLabel);

  setProductType(type: ProductType, label: string): void {
    this.draft.update((d) => ({ ...d, productType: type, productLabel: label }));
  }

  setTitle(title: string): void {
    this.draft.update((d) => ({ ...d, title }));
  }

  setCategory(category: string): void {
    this.draft.update((d) => ({ ...d, category }));
  }

  setSubcategory(subcategory: string): void {
    this.draft.update((d) => ({ ...d, subcategory }));
  }

  setWhatYouWillLearn(value: string): void {
    this.draft.update((d) => ({ ...d, whatYouWillLearn: value }));
  }

  setTargetAudience(value: string): void {
    this.draft.update((d) => ({ ...d, targetAudience: value }));
  }

  addModule(title = 'Nuevo módulo'): void {
    this.draft.update((d) => {
      const orderIndex = d.curriculum.length;
      const module: BuilderModuleDraft = {
        clientId: newClientId(),
        title,
        orderIndex,
        expanded: true,
        lessons: [],
      };
      return { ...d, curriculum: [...d.curriculum, module] };
    });
  }

  updateModuleTitle(clientId: string, title: string): void {
    this.patchModule(clientId, (m) => ({ ...m, title }));
  }

  toggleModule(clientId: string): void {
    this.patchModule(clientId, (m) => ({ ...m, expanded: !m.expanded }));
  }

  removeModule(clientId: string): void {
    this.draft.update((d) => ({
      ...d,
      curriculum: d.curriculum
        .filter((m) => m.clientId !== clientId)
        .map((m, index) => ({ ...m, orderIndex: index })),
    }));
  }

  moveModule(clientId: string, direction: -1 | 1): void {
    this.draft.update((d) => {
      const list = [...d.curriculum];
      const index = list.findIndex((m) => m.clientId === clientId);
      const target = index + direction;
      if (index < 0 || target < 0 || target >= list.length) {
        return d;
      }
      const [item] = list.splice(index, 1);
      list.splice(target, 0, item);
      return { ...d, curriculum: list.map((m, i) => ({ ...m, orderIndex: i })) };
    });
  }

  addLesson(moduleClientId: string): void {
    this.patchModule(moduleClientId, (m) => ({
      ...m,
      expanded: true,
      lessons: [
        ...m.lessons,
        {
          clientId: newClientId(),
          title: 'Nueva lección',
          contentUrl: '',
          lessonType: 'VIDEO' as LessonType,
          durationMinutes: 0,
          orderIndex: m.lessons.length,
        },
      ],
    }));
  }

  updateLesson(
    moduleClientId: string,
    lessonClientId: string,
    patch: Partial<BuilderLessonDraft>,
  ): void {
    this.patchModule(moduleClientId, (m) => ({
      ...m,
      lessons: m.lessons.map((l) =>
        l.clientId === lessonClientId ? { ...l, ...patch } : l,
      ),
    }));
  }

  removeLesson(moduleClientId: string, lessonClientId: string): void {
    this.patchModule(moduleClientId, (m) => ({
      ...m,
      lessons: m.lessons
        .filter((l) => l.clientId !== lessonClientId)
        .map((l, index) => ({ ...l, orderIndex: index })),
    }));
  }

  reset(): void {
    this.draft.set({ ...EMPTY_DRAFT, curriculum: [] });
    this.clearStoredCourseId();
  }

  toCreateCoursePayload(): CreateCourseRequest {
    const d = this.draft();
    return {
      title: d.title.trim() || DEFAULT_DRAFT_TITLE,
      courseFormat: d.productType ?? 'course',
      category: d.category ?? '',
      subcategory: d.subcategory ?? '',
      whatYouWillLearn: d.whatYouWillLearn ?? '',
      targetAudience: d.targetAudience ?? '',
    };
  }

  toApiPayload(): CourseBuilderRequest {
    const d = this.draft();
    const audienceCombined = [d.whatYouWillLearn, d.targetAudience].filter(Boolean).join('\n\n');
    return {
      format: d.productType ?? 'course',
      title: d.title.trim(),
      category: d.category,
      subcategory: d.subcategory,
      audience: audienceCombined,
      curriculum: d.curriculum.map((module) => ({
        title: module.title,
        orderIndex: module.orderIndex,
        lessons: module.lessons.map((lesson) => ({
          title: lesson.title,
          contentUrl: lesson.contentUrl,
          lessonType: lesson.lessonType,
          durationMinutes: lesson.durationMinutes,
          orderIndex: lesson.orderIndex,
        })),
      })),
    };
  }

  async saveDraftCourse(): Promise<number> {
    await this.ensureInfoproductorSession();
    const response = await firstValueFrom(
      this.producerCourses.createDraft(this.toCreateCoursePayload()),
    );
    this.setCourseId(response.id);
    return response.id;
  }

  /**
   * Obtiene o crea un borrador en servidor. No exige pasos previos del wizard.
   */
  async ensureCourseId(): Promise<number> {
    await this.ensureInfoproductorSession();
    const existing = this.draft().courseId ?? this.readStoredCourseId();
    if (existing) {
      this.draft.update((d) => ({ ...d, courseId: existing }));
      return existing;
    }
    return this.saveDraftCourse();
  }

  /**
   * APIs de productor exigen JWT con vista infoproductor (claim + token renovado).
   */
  async ensureInfoproductorSession(): Promise<void> {
    if (!this.auth.isLoggedIn()) {
      throw new HttpErrorResponse({
        status: 401,
        statusText: 'Unauthorized',
        url: `${environment.apiUrl}/producer/courses`,
      });
    }

    const user = this.auth.getCurrentUser();
    const token = this.auth.getToken();
    if (isInfoproductorRole(user, token) || resolveUserRole(user, token) === 'admin') {
      return;
    }

    if (canAssumeInfoproductorRole(user)) {
      await firstValueFrom(this.auth.switchRole('infoproductor'));
      return;
    }

    throw new HttpErrorResponse({
      status: 403,
      statusText: 'Forbidden',
      error: {
        message:
          'Tu cuenta no tiene permisos de infoproductor. Regístrate o contacta soporte.',
      },
      url: `${environment.apiUrl}/producer/courses`,
    });
  }

  setCourseId(id: number): void {
    this.draft.update((d) => ({ ...d, courseId: id }));
    localStorage.setItem(BUILDER_COURSE_ID_KEY, String(id));
  }

  isUnauthorizedError(error: unknown): boolean {
    return error instanceof HttpErrorResponse && error.status === 401;
  }

  isForbiddenError(error: unknown): boolean {
    return error instanceof HttpErrorResponse && error.status === 403;
  }

  getApiErrorMessage(error: unknown): string {
    if (this.isUnauthorizedError(error)) {
      return 'Tu sesión expiró. Inicia sesión de nuevo.';
    }
    if (this.isForbiddenError(error)) {
      const body = (error as HttpErrorResponse).error as { message?: string } | null;
      return (
        body?.message ??
        'No tienes permisos de infoproductor. Usa el conmutador del menú para cambiar a Infoproductor.'
      );
    }
    return 'No se pudo guardar el curso. Revisa la conexión con el servidor.';
  }

  private readStoredCourseId(): number | null {
    const raw = localStorage.getItem(BUILDER_COURSE_ID_KEY);
    if (!raw) {
      return null;
    }
    const id = Number(raw);
    return Number.isNaN(id) ? null : id;
  }

  /** Limpia el id de borrador local (p. ej. curso ajeno o sesión distinta). */
  clearStoredCourseId(): void {
    this.draft.update((d) => ({ ...d, courseId: null }));
    localStorage.removeItem(BUILDER_COURSE_ID_KEY);
  }

  async saveDraftToServer(): Promise<number> {
    const response = await firstValueFrom(this.api.saveDraft(this.toApiPayload()));
    this.draft.update((d) => ({ ...d, courseId: response.id }));
    return response.id;
  }

  private patchModule(
    clientId: string,
    updater: (module: BuilderModuleDraft) => BuilderModuleDraft,
  ): void {
    this.draft.update((d) => ({
      ...d,
      curriculum: d.curriculum.map((m) => (m.clientId === clientId ? updater(m) : m)),
    }));
  }
}
