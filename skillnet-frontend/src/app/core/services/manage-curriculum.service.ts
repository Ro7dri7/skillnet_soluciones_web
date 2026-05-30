import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  isCourseOwnershipError,
  messageFromHttpError,
} from '../../shared/utils/http-error.util';
import { CourseBuilderService } from './course-builder.service';
import { ToastService } from './toast.service';
import {
  AddLessonData,
  ContentBlockDTO,
  ContentType,
  LessonDTO,
  PendingLessonForm,
  QuizData,
  SectionDTO,
} from '../../shared/models/curriculum.model';

interface ApiContentBlock {
  id: string;
  contentType: string;
  resourceUrl?: string;
  textContent?: string;
  quizData?: QuizData;
  orderIndex: number;
}

interface ApiCurriculumLesson {
  id: number;
  title: string;
  contentType: string;
  resourceUrl: string;
  textContent: string;
  quizData?: QuizData;
  orderIndex: number;
  blocks?: ApiContentBlock[];
}

interface ApiCurriculumModule {
  id: number;
  title: string;
  orderIndex: number;
  lessons: ApiCurriculumLesson[];
}

const VALID_CONTENT_TYPES: ContentType[] = [
  'text',
  'image',
  'video',
  'pdf',
  'quiz',
  'audio',
];

@Injectable({ providedIn: 'root' })
export class ManageCurriculumService {
  private readonly http = inject(HttpClient);
  private readonly courseBuilder = inject(CourseBuilderService);
  private readonly toast = inject(ToastService);
  private readonly apiUrl = environment.apiUrl;

  private moduleTitleDebounce: ReturnType<typeof setTimeout> | null = null;

  private readonly _modules = signal<SectionDTO[]>([]);
  private readonly _loadedCourseId = signal<number | null>(null);
  private readonly _loading = signal(false);
  private readonly _saving = signal(false);
  private readonly _errorMessage = signal<string | null>(null);

  readonly modules = this._modules.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly saving = this._saving.asReadonly();
  readonly errorMessage = this._errorMessage.asReadonly();
  readonly loadedCourseId = this._loadedCourseId.asReadonly();

  readonly activeLessonId = signal<string | null>(null);
  readonly activeModuleId = signal<string | null>(null);
  readonly pendingLessonForm = signal<PendingLessonForm | null>(null);
  readonly showNewModuleForm = signal(false);
  readonly newModuleTitle = signal('');

  readonly hasModules = computed(() => this._modules().length > 0);
  readonly totalLessons = computed(() =>
    this._modules().reduce((sum, m) => sum + m.lessons.length, 0),
  );

  loadCurriculum(courseId: number, force = false): void {
    void this.loadCurriculumAsync(courseId, force);
  }

  async loadCurriculumAsync(courseId: number, force = false): Promise<void> {
    if (!force && this._loadedCourseId() === courseId && this._modules().length > 0) {
      return;
    }

    this._loading.set(true);
    this._errorMessage.set(null);
    this._loadedCourseId.set(courseId);

    try {
      await this.ensureApiSession();
      const data = await firstValueFrom(
        this.http.get<ApiCurriculumModule[]>(`${this.apiUrl}/courses/${courseId}/curriculum`),
      );
      this._modules.set(data.map((m) => this.mapModuleFromApi(m)));
    } catch (err) {
      console.error('Error loading curriculum', err);
      this.handleApiError(err, 'No se pudo cargar el temario desde el servidor.');
      if (isCourseOwnershipError(err)) {
        this.courseBuilder.clearStoredCourseId();
      }
      this._modules.set([]);
    } finally {
      this._loading.set(false);
    }
  }

  reset(): void {
    this._modules.set([]);
    this._loadedCourseId.set(null);
    this.activeLessonId.set(null);
    this.activeModuleId.set(null);
    this.pendingLessonForm.set(null);
    this.showNewModuleForm.set(false);
    this.newModuleTitle.set('');
    this._errorMessage.set(null);
  }

  clearError(): void {
    this._errorMessage.set(null);
  }

  setError(message: string | null): void {
    this._errorMessage.set(message);
  }

  startAddModule(): void {
    this.showNewModuleForm.set(true);
    this.newModuleTitle.set('');
  }

  cancelAddModule(): void {
    this.showNewModuleForm.set(false);
    this.newModuleTitle.set('');
  }

  async confirmAddModule(): Promise<void> {
    const title = this.newModuleTitle().trim();
    const courseId = this._loadedCourseId();
    if (!title || courseId == null) {
      return;
    }
    await this.addModule(courseId, title);
    this.cancelAddModule();
  }

  async addModule(courseId: number, title: string): Promise<string> {
    const orderIndex = this._modules().length;
    this._saving.set(true);
    this._errorMessage.set(null);

    try {
      await this.ensureApiSession();
      const created = await firstValueFrom(
        this.http.post<ApiCurriculumModule>(`${this.apiUrl}/courses/${courseId}/sections`, {
          title: title.trim(),
          orderIndex,
        }),
      );

      const module = this.mapModuleFromApi(created);
      module.expanded = true;
      this._modules.update((list) => [...list, module]);
      this.activeModuleId.set(module.id);
      this.toast.success('Módulo guardado');
      return module.id;
    } catch (err) {
      console.error('Error creating section', err);
      this.handleApiError(err, 'No se pudo guardar el módulo. Intenta de nuevo.');
      throw err;
    } finally {
      this._saving.set(false);
    }
  }

  updateModuleTitle(moduleId: string, title: string): void {
    this.patchModule(moduleId, (m) => ({ ...m, title: title.trim() }));
    this.schedulePersistModuleTitle(moduleId, title);
  }

  schedulePersistModuleTitle(moduleId: string, title: string): void {
    if (this.moduleTitleDebounce) {
      clearTimeout(this.moduleTitleDebounce);
    }
    this.moduleTitleDebounce = setTimeout(() => {
      void this.persistModuleTitle(moduleId, title).catch(() => undefined);
    }, 800);
  }

  async flushPendingModuleTitles(): Promise<void> {
    if (this.moduleTitleDebounce) {
      clearTimeout(this.moduleTitleDebounce);
      this.moduleTitleDebounce = null;
    }
    const moduleId = this.activeModuleId();
    if (!moduleId) {
      return;
    }
    const title = this._modules().find((m) => m.id === moduleId)?.title;
    if (title?.trim()) {
      await this.persistModuleTitle(moduleId, title);
    }
  }

  async persistModuleTitle(moduleId: string, title: string): Promise<void> {
    const trimmed = title.trim();
    if (!trimmed) {
      return;
    }
    const sectionId = Number(moduleId);
    if (Number.isNaN(sectionId)) {
      return;
    }

    const previousTitle =
      this._modules().find((m) => m.id === moduleId)?.title ?? trimmed;

    this._saving.set(true);
    this._errorMessage.set(null);
    try {
      await this.ensureApiSession();
      const updated = await firstValueFrom(
        this.http.put<ApiCurriculumModule>(`${this.apiUrl}/sections/${sectionId}`, {
          title: trimmed,
        }),
      );
      this.patchModule(moduleId, (m) => ({
        ...m,
        title: updated.title,
      }));
    } catch (err) {
      console.error('Error updating section', err);
      this.patchModule(moduleId, (m) => ({ ...m, title: previousTitle }));
      this.handleApiError(err, 'Error al guardar, reintentando…');
      throw err;
    } finally {
      this._saving.set(false);
    }
  }

  toggleModule(moduleId: string): void {
    this.patchModule(moduleId, (m) => ({ ...m, expanded: !m.expanded }));
  }

  async removeModule(moduleId: string): Promise<void> {
    const sectionId = Number(moduleId);
    if (Number.isNaN(sectionId)) {
      return;
    }

    const previous = this._modules();
    this._modules.update((list) =>
      list.filter((m) => m.id !== moduleId).map((m, index) => ({ ...m, orderIndex: index })),
    );
    if (this.activeModuleId() === moduleId) {
      this.activeModuleId.set(null);
    }
    this.pendingLessonForm.update((p) => (p?.moduleId === moduleId ? null : p));

    this._saving.set(true);
    this._errorMessage.set(null);
    try {
      await this.ensureApiSession();
      await firstValueFrom(this.http.delete<void>(`${this.apiUrl}/sections/${sectionId}`));
      this.toast.success('Módulo eliminado');
    } catch (err) {
      console.error('Error deleting section', err);
      this._modules.set(previous);
      this.handleApiError(err, 'No se pudo eliminar el módulo.');
      throw err;
    } finally {
      this._saving.set(false);
    }
  }

  startAddLesson(moduleId: string): void {
    this.activeModuleId.set(moduleId);
    this.patchModule(moduleId, (m) => ({ ...m, expanded: true }));
    this.pendingLessonForm.set(null);
  }

  selectLessonType(moduleId: string, contentType: ContentType): void {
    this.activeModuleId.set(moduleId);
    this.pendingLessonForm.set({ moduleId, contentType });
  }

  cancelPendingLesson(): void {
    this.pendingLessonForm.set(null);
  }

  async addLesson(moduleId: string, data: AddLessonData): Promise<string> {
    const sectionId = Number(moduleId);
    if (Number.isNaN(sectionId)) {
      throw new Error('Invalid section id');
    }

    this._saving.set(true);
    this._errorMessage.set(null);

    try {
      await this.ensureApiSession();
      const payload: Record<string, unknown> = {
        title: data.title.trim(),
        contentType: data.contentType ?? 'text',
        resourceUrl: data.resourceUrl ?? '',
        textContent: data.textContent ?? '',
      };
      if (data.quizData) {
        payload['quizData'] = data.quizData;
      }
      if (data.blocks !== undefined) {
        payload['blocks'] = data.blocks;
      }
      const created = await firstValueFrom(
        this.http.post<ApiCurriculumLesson>(`${this.apiUrl}/sections/${sectionId}/lessons`, payload),
      );

      const lesson = this.mapLessonFromApi(created);

      this.patchModule(moduleId, (m) => ({
        ...m,
        expanded: true,
        lessons: [...m.lessons, lesson],
      }));

      this.activeLessonId.set(lesson.id);
      this.activeModuleId.set(moduleId);
      this.pendingLessonForm.set(null);
      this.toast.success('Lección guardada');
      return lesson.id;
    } catch (err) {
      console.error('Error creating lesson', err);
      this.handleApiError(err, 'No se pudo guardar la lección.');
      throw err;
    } finally {
      this._saving.set(false);
    }
  }

  updateLesson(moduleId: string, lessonId: string, patch: Partial<LessonDTO>): void {
    this.patchModule(moduleId, (m) => ({
      ...m,
      lessons: m.lessons.map((l) => (l.id === lessonId ? { ...l, ...patch } : l)),
    }));
  }

  async persistLesson(moduleId: string, lessonId: string, data: AddLessonData): Promise<void> {
    const id = Number(lessonId);
    if (Number.isNaN(id)) {
      return;
    }

    const previous = this.getLesson(moduleId, lessonId);
    if (!previous) {
      return;
    }

    this._saving.set(true);
    this._errorMessage.set(null);
    try {
      await this.ensureApiSession();
      const payload: Record<string, unknown> = {
        title: data.title.trim(),
      };
      if (data.contentType) {
        payload['contentType'] = data.contentType;
      }
      if (data.resourceUrl !== undefined) {
        payload['resourceUrl'] = data.resourceUrl;
      }
      if (data.textContent !== undefined) {
        payload['textContent'] = data.textContent;
      }
      if (data.quizData) {
        payload['quizData'] = data.quizData;
      }
      if (data.blocks !== undefined) {
        payload['blocks'] = data.blocks;
      }

      const updated = await firstValueFrom(
        this.http.put<ApiCurriculumLesson>(`${this.apiUrl}/lessons/${id}`, payload),
      );

      const lesson = this.mapLessonFromApi(updated);
      this.patchModule(moduleId, (m) => ({
        ...m,
        lessons: m.lessons.map((l) => (l.id === lessonId ? lesson : l)),
      }));
    } catch (err) {
      console.error('Error updating lesson', err);
      this.patchModule(moduleId, (m) => ({
        ...m,
        lessons: m.lessons.map((l) => (l.id === lessonId ? previous : l)),
      }));
      this.handleApiError(err, 'Error al guardar, reintentando…');
      throw err;
    } finally {
      this._saving.set(false);
    }
  }

  async addQuickLesson(moduleId: string, title = 'Nueva Lección'): Promise<string> {
    return this.addLesson(moduleId, {
      title,
      contentType: 'text',
      blocks: [],
    });
  }

  async addQuizLesson(moduleId: string, title = 'Nueva Evaluación'): Promise<string> {
    return this.addLesson(moduleId, {
      title,
      contentType: 'quiz',
      blocks: [this.createDefaultBlock('quiz', 0)],
    });
  }

  async addContentBlock(
    moduleId: string,
    lessonId: string,
    contentType: ContentType,
  ): Promise<string> {
    const lesson = this.getLesson(moduleId, lessonId);
    if (!lesson) {
      throw new Error('Lesson not found');
    }

    const block = this.createDefaultBlock(contentType, lesson.blocks.length);
    const blocks = [...lesson.blocks, block];

    this.updateLesson(moduleId, lessonId, { blocks, contentType: blocks[0]?.contentType ?? 'text' });

    this._saving.set(true);
    this._errorMessage.set(null);
    try {
      await this.ensureApiSession();
      await this.persistLesson(moduleId, lessonId, {
        title: lesson.title,
        blocks,
      });
      return block.id;
    } catch (err) {
      this.updateLesson(moduleId, lessonId, { blocks: lesson.blocks });
      throw err;
    } finally {
      this._saving.set(false);
    }
  }

  async removeContentBlock(moduleId: string, lessonId: string, blockId: string): Promise<void> {
    const lesson = this.getLesson(moduleId, lessonId);
    if (!lesson) {
      return;
    }

    const previous = lesson.blocks;
    const blocks = lesson.blocks
      .filter((b) => b.id !== blockId)
      .map((b, index) => ({ ...b, orderIndex: index }));

    this.updateLesson(moduleId, lessonId, { blocks });

    this._saving.set(true);
    this._errorMessage.set(null);
    try {
      await this.ensureApiSession();
      await this.persistLesson(moduleId, lessonId, {
        title: lesson.title,
        blocks,
      });
    } catch (err) {
      this.updateLesson(moduleId, lessonId, { blocks: previous });
      this.handleApiError(err, 'No se pudo eliminar el bloque.');
      throw err;
    } finally {
      this._saving.set(false);
    }
  }

  createDefaultBlock(contentType: ContentType, orderIndex: number): ContentBlockDTO {
    return {
      id: `blk-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      contentType,
      resourceUrl: '',
      textContent: contentType === 'text' ? '' : '',
      orderIndex,
      quizData:
        contentType === 'quiz'
          ? {
              passingScore: 80,
              timeLimitMinutes: 30,
              maxAttempts: 3,
              questions: [
                {
                  id: `q-${Date.now()}`,
                  type: 'multiple',
                  text: '',
                  options: [''],
                  correctIndex: 0,
                },
              ],
            }
          : undefined,
    };
  }

  async removeLesson(moduleId: string, lessonId: string): Promise<void> {
    const id = Number(lessonId);
    if (Number.isNaN(id)) {
      return;
    }

    const previous = this._modules();
    this.patchModule(moduleId, (m) => ({
      ...m,
      lessons: m.lessons
        .filter((l) => l.id !== lessonId)
        .map((l, index) => ({ ...l, orderIndex: index })),
    }));
    if (this.activeLessonId() === lessonId) {
      this.activeLessonId.set(null);
    }

    this._saving.set(true);
    this._errorMessage.set(null);
    try {
      await this.ensureApiSession();
      await firstValueFrom(this.http.delete<void>(`${this.apiUrl}/lessons/${id}`));
      this.toast.success('Lección eliminada');
    } catch (err) {
      console.error('Error deleting lesson', err);
      this._modules.set(previous);
      this.handleApiError(err, 'No se pudo eliminar la lección.');
      throw err;
    } finally {
      this._saving.set(false);
    }
  }

  selectLesson(moduleId: string, lessonId: string): void {
    this.activeModuleId.set(moduleId);
    this.activeLessonId.set(lessonId);
    this.pendingLessonForm.set(null);
  }

  getLesson(moduleId: string, lessonId: string): LessonDTO | undefined {
    return this._modules().find((m) => m.id === moduleId)?.lessons.find((l) => l.id === lessonId);
  }

  private mapModuleFromApi(api: ApiCurriculumModule): SectionDTO {
    return {
      id: String(api.id),
      title: api.title,
      orderIndex: api.orderIndex,
      expanded: true,
      lessons: (api.lessons ?? []).map((l) => this.mapLessonFromApi(l)),
    };
  }

  private mapLessonFromApi(api: ApiCurriculumLesson): LessonDTO {
    const blocks = this.normalizeBlocksFromApi(api);
    const primary = blocks[0];
    return {
      id: String(api.id),
      title: api.title,
      contentType: primary ? primary.contentType : this.normalizeContentType(api.contentType),
      resourceUrl: primary?.resourceUrl ?? api.resourceUrl ?? '',
      textContent: primary?.textContent ?? api.textContent ?? '',
      orderIndex: api.orderIndex,
      quizData: primary?.quizData ?? api.quizData,
      blocks,
    };
  }

  private normalizeBlocksFromApi(api: ApiCurriculumLesson): ContentBlockDTO[] {
    if (Array.isArray(api.blocks)) {
      return api.blocks
        .map((block, index) => ({
          id: block.id || `blk-${api.id}-${index}`,
          contentType: this.normalizeContentType(block.contentType),
          resourceUrl: block.resourceUrl ?? '',
          textContent: block.textContent ?? '',
          orderIndex: block.orderIndex ?? index,
          quizData: block.quizData,
        }))
        .sort((a, b) => a.orderIndex - b.orderIndex);
    }

    return [
      {
        id: `blk-${api.id}-0`,
        contentType: this.normalizeContentType(api.contentType),
        resourceUrl: api.resourceUrl ?? '',
        textContent: api.textContent ?? '',
        orderIndex: 0,
        quizData: api.quizData,
      },
    ];
  }

  private normalizeContentType(raw: string): ContentType {
    const value = raw?.toLowerCase() as ContentType;
    return VALID_CONTENT_TYPES.includes(value) ? value : 'video';
  }

  private patchModule(
    moduleId: string,
    updater: (module: SectionDTO) => SectionDTO,
  ): void {
    this._modules.update((list) =>
      list.map((m) => (m.id === moduleId ? updater(m) : m)),
    );
  }

  private async ensureApiSession(): Promise<void> {
    await this.courseBuilder.ensureInfoproductorSession();
  }

  private handleApiError(err: unknown, fallback: string): void {
    const message = messageFromHttpError(err, fallback);
    this._errorMessage.set(message);
    this.toast.error(message);
    if (err instanceof HttpErrorResponse && err.status === 401) {
      return;
    }
    if (isCourseOwnershipError(err)) {
      this.courseBuilder.clearStoredCourseId();
    }
  }
}
