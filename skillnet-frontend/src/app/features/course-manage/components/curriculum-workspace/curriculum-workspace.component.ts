import {
  Component,
  computed,
  effect,
  inject,
  input,
  OnDestroy,
  OnInit,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CourseBuilderService } from '../../../../core/services/course-builder.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ManageCurriculumService } from '../../../../core/services/manage-curriculum.service';
import { ManageLayoutSaveService } from '../../../../core/services/manage-layout-save.service';
import { ToastService } from '../../../../core/services/toast.service';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import type { ContentBlockDTO, ContentType, LessonDTO, QuizQuestionDraft } from '../../../../shared/models/curriculum.model';

interface ConfirmDialogState {
  title: string;
  message: string;
  onConfirm: () => void | Promise<void>;
}

interface BlockTypeOption {
  id: ContentType;
  label: string;
  bgClass: string;
  textClass: string;
  icon: string;
}

@Component({
  selector: 'app-curriculum-workspace',
  standalone: true,
  imports: [FormsModule, ConfirmDialogComponent],
  templateUrl: './curriculum-workspace.component.html',
  styleUrl: './curriculum-workspace.component.scss',
})
export class CurriculumWorkspaceComponent implements OnInit, OnDestroy {
  readonly initialCourseId = input<number | null>(null);
  readonly curriculumChanged = output<boolean>();
  readonly courseReady = output<number>();

  readonly curriculum = inject(ManageCurriculumService);
  private readonly builder = inject(CourseBuilderService);
  private readonly manageSave = inject(ManageLayoutSaveService);
  readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly resolvedCourseId = signal<number | null>(null);
  readonly showLoginBanner = computed(() => !this.auth.isLoggedIn());
  readonly isSaving = computed(() => this.curriculum.saving() || this.curriculum.loading());

  readonly activeTab = signal<'content' | 'evaluation' | 'resources'>('content');
  readonly editingModuleId = signal<string | null>(null);
  readonly editingModuleTitle = signal('');
  readonly editingLessonId = signal<string | null>(null);
  readonly editingLessonTitle = signal('');
  readonly confirmDialog = signal<ConfirmDialogState | null>(null);

  readonly draftTitle = signal('');
  readonly draftResourceUrl = signal('');
  readonly draftQuizQuestion = signal('');
  readonly quizQuestions = signal<QuizQuestionDraft[]>([]);
  readonly quizPassingScore = signal(80);
  readonly quizTimeLimit = signal(30);

  readonly targetModuleId = computed(() => {
    const active = this.curriculum.activeModuleId();
    if (active) {
      return active;
    }
    const modules = this.curriculum.modules();
    return modules.length > 0 ? modules[0].id : null;
  });

  readonly targetModuleTitle = computed(() => {
    const id = this.targetModuleId();
    if (!id) {
      return '';
    }
    return this.curriculum.modules().find((m) => m.id === id)?.title ?? 'el módulo activo';
  });

  readonly blockTypes: BlockTypeOption[] = [
    { id: 'text', label: 'Texto', bgClass: 'bg-[#B2B4FF]', textClass: 'text-indigo-900', icon: 'ri-text' },
    { id: 'image', label: 'Imagen', bgClass: 'bg-[#B4FFB4]', textClass: 'text-green-900', icon: 'ri-image-line' },
    { id: 'video', label: 'Video', bgClass: 'bg-[#FFB4B4]', textClass: 'text-red-900', icon: 'ri-play-mini-fill' },
    { id: 'pdf', label: 'PDF', bgClass: 'bg-[#D4B4FF]', textClass: 'text-purple-900', icon: 'ri-file-pdf-line' },
    { id: 'quiz', label: 'Quizz', bgClass: 'bg-[#FFE0A0]', textClass: 'text-amber-900', icon: 'ri-question-mark' },
    { id: 'audio', label: 'Audio', bgClass: 'bg-[#FFACFC]', textClass: 'text-pink-900', icon: 'ri-mic-line' },
  ];

  private saveDebounce: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    effect(() => {
      const lesson = this.activeLesson();
      if (!lesson) {
        return;
      }
      this.draftTitle.set(lesson.title);

      const quizBlock = lesson.blocks.find((b) => b.contentType === 'quiz');
      if (quizBlock?.quizData) {
        this.quizQuestions.set([...quizBlock.quizData.questions]);
        this.quizPassingScore.set(quizBlock.quizData.passingScore);
        this.quizTimeLimit.set(quizBlock.quizData.timeLimitMinutes);
      } else if (lesson.quizData) {
        this.quizQuestions.set([...lesson.quizData.questions]);
        this.quizPassingScore.set(lesson.quizData.passingScore);
        this.quizTimeLimit.set(lesson.quizData.timeLimitMinutes);
      }

      if (this.isLessonQuiz()) {
        // keep current tab unless switching from non-quiz lesson
      } else if (this.activeTab() === 'evaluation') {
        this.activeTab.set('content');
      }
    });

    effect(() => {
      const modules = this.curriculum.modules();
      if (modules.length === 0 || this.curriculum.activeLessonId()) {
        return;
      }
      const moduleId = this.curriculum.activeModuleId() ?? modules[0].id;
      const module = modules.find((m) => m.id === moduleId) ?? modules[0];
      if (module.lessons.length > 0) {
        this.curriculum.selectLesson(module.id, module.lessons[0].id);
      }
    });

    effect(() => {
      this.curriculumChanged.emit(this.curriculum.hasModules());
    });

    effect(() => {
      const modules = this.curriculum.modules();
      if (modules.length === 0) {
        return;
      }
      if (!this.curriculum.activeModuleId()) {
        this.curriculum.activeModuleId.set(modules[0].id);
      }
    });
  }

  ngOnInit(): void {
    this.manageSave.registerSaveHandler(() => this.flushCurriculumSave());
    void this.bootstrapCourse();
  }

  private async bootstrapCourse(): Promise<void> {
    const initial = this.initialCourseId();
    if (initial) {
      this.resolvedCourseId.set(initial);
      await this.curriculum.loadCurriculumAsync(initial);
      if (this.curriculum.modules().length === 0 && this.curriculum.errorMessage()) {
        await this.retryWithOwnedCourse();
      }
      return;
    }
    const id = await this.resolveCourseId();
    if (id) {
      await this.curriculum.loadCurriculumAsync(id, true);
    }
  }

  private async retryWithOwnedCourse(): Promise<void> {
    this.builder.clearStoredCourseId();
    this.resolvedCourseId.set(null);
    const id = await this.resolveCourseId();
    if (id) {
      await this.curriculum.loadCurriculumAsync(id, true);
    }
  }

  ngOnDestroy(): void {
    this.manageSave.unregisterSaveHandler();
    this.clearSaveDebounce();
  }

  private clearSaveDebounce(): void {
    if (this.saveDebounce) {
      clearTimeout(this.saveDebounce);
      this.saveDebounce = null;
    }
  }

  activeLesson(): LessonDTO | null {
    const lessonId = this.curriculum.activeLessonId();
    const moduleId = this.curriculum.activeModuleId();
    if (!lessonId || !moduleId) {
      return null;
    }
    return this.curriculum.getLesson(moduleId, lessonId) ?? null;
  }

  isLessonQuiz(): boolean {
    const lesson = this.activeLesson();
    return lesson?.blocks.some((b) => b.contentType === 'quiz') ?? false;
  }

  setTab(tab: 'content' | 'evaluation' | 'resources'): void {
    if (tab === 'evaluation' && !this.isLessonQuiz()) {
      return;
    }
    this.activeTab.set(tab);
  }

  async confirmNewModule(): Promise<void> {
    if (!(await this.requireCourseId())) {
      return;
    }
    await this.curriculum.confirmAddModule();
  }

  startEditModule(moduleId: string, title: string): void {
    this.editingModuleId.set(moduleId);
    this.editingModuleTitle.set(title);
  }

  async saveModuleTitle(moduleId: string): Promise<void> {
    const title = this.editingModuleTitle().trim();
    if (title) {
      await this.curriculum.persistModuleTitle(moduleId, title);
    }
    this.editingModuleId.set(null);
  }

  async handleAddModule(): Promise<void> {
    const courseId = await this.resolveCourseId();
    if (!courseId) {
      return;
    }
    await this.curriculum.addModule(courseId, `Módulo ${this.curriculum.modules().length + 1}`);
  }

  private async resolveCourseId(): Promise<number | null> {
    const current = this.resolvedCourseId();
    if (current) {
      return current;
    }
    try {
      const id = await this.builder.ensureCourseId();
      this.resolvedCourseId.set(id);
      this.courseReady.emit(id);
      this.curriculum.loadCurriculum(id, true);
      this.curriculum.clearError();
      return id;
    } catch (error) {
      this.curriculum.setError(this.builder.getApiErrorMessage(error));
      return null;
    }
  }

  private async requireCourseId(): Promise<number | null> {
    return this.resolveCourseId();
  }

  async handleAddLesson(moduleId: string): Promise<void> {
    if (!(await this.requireCourseId())) {
      return;
    }
    const lessonId = await this.curriculum.addQuickLesson(moduleId);
    this.curriculum.selectLesson(moduleId, lessonId);
  }

  async handleAddQuiz(moduleId: string): Promise<void> {
    if (!(await this.requireCourseId())) {
      return;
    }
    const lessonId = await this.curriculum.addQuizLesson(moduleId);
    this.curriculum.selectLesson(moduleId, lessonId);
  }

  async createLessonFromBlockCenter(_contentType: ContentType): Promise<void> {
    if (!this.activeLesson()) {
      this.toast.error('Selecciona o crea una lección antes de añadir contenido.');
      return;
    }
    await this.addContentBlock(_contentType);
  }

  async addContentBlock(contentType: ContentType): Promise<void> {
    const lesson = this.activeLesson();
    const moduleId = this.curriculum.activeModuleId();
    if (!lesson || !moduleId) {
      this.toast.error('Selecciona una lección antes de añadir contenido.');
      return;
    }
    if (!(await this.requireCourseId())) {
      return;
    }
    this.clearSaveDebounce();
    await this.curriculum.addContentBlock(moduleId, lesson.id, contentType);
    if (contentType === 'quiz') {
      this.activeTab.set('evaluation');
    }
  }

  updateBlockField(
    blockId: string,
    field: 'resourceUrl' | 'textContent',
    value: string,
  ): void {
    const lesson = this.activeLesson();
    const moduleId = this.curriculum.activeModuleId();
    if (!lesson || !moduleId) {
      return;
    }
    const blocks = lesson.blocks.map((block) =>
      block.id === blockId ? { ...block, [field]: value } : block,
    );
    this.curriculum.updateLesson(moduleId, lesson.id, { blocks });
    this.scheduleSave();
  }

  updateBlockQuizData(): void {
    const lesson = this.activeLesson();
    const moduleId = this.curriculum.activeModuleId();
    if (!lesson || !moduleId) {
      return;
    }
    const quizData = {
      passingScore: this.quizPassingScore(),
      timeLimitMinutes: this.quizTimeLimit(),
      questions: this.quizQuestions(),
    };
    const blocks = lesson.blocks.map((block) =>
      block.contentType === 'quiz' ? { ...block, quizData } : block,
    );
    this.curriculum.updateLesson(moduleId, lesson.id, { blocks, quizData });
    this.scheduleSave();
  }

  selectLesson(moduleId: string, lessonId: string): void {
    this.curriculum.selectLesson(moduleId, lessonId);
  }

  scheduleSave(): void {
    if (this.saveDebounce) {
      clearTimeout(this.saveDebounce);
    }
    this.saveDebounce = setTimeout(() => void this.persistActiveLesson(), 600);
  }

  async persistActiveLesson(): Promise<void> {
    const lesson = this.activeLesson();
    const moduleId = this.curriculum.activeModuleId();
    if (!lesson || !moduleId) {
      return;
    }

    const quizData = this.isLessonQuiz()
      ? {
          passingScore: this.quizPassingScore(),
          timeLimitMinutes: this.quizTimeLimit(),
          questions: this.quizQuestions(),
        }
      : undefined;

    const blocks = lesson.blocks.map((block) =>
      block.contentType === 'quiz' && quizData ? { ...block, quizData } : block,
    );

    const payload = {
      title: this.draftTitle().trim() || lesson.title,
      blocks,
      quizData,
    };

    this.curriculum.updateLesson(moduleId, lesson.id, {
      title: payload.title,
      blocks,
      quizData,
    });

    try {
      await this.curriculum.persistLesson(moduleId, lesson.id, payload);
    } catch {
      // error surfaced via service + toast
    }
  }

  /** Guardado del botón superior del layout de gestión del curso. */
  private async flushCurriculumSave(): Promise<void> {
    this.clearSaveDebounce();
    await this.curriculum.flushPendingModuleTitles();

    const lesson = this.activeLesson();
    const moduleId = this.curriculum.activeModuleId();
    if (lesson && moduleId) {
      await this.persistActiveLesson();
      const err = this.curriculum.errorMessage();
      if (err) {
        throw new Error(err);
      }
    }
  }

  async saveActiveLessonManual(): Promise<void> {
    try {
      await this.flushCurriculumSave();
      if (!this.curriculum.errorMessage()) {
        this.toast.success('Cambios guardados');
      }
    } catch {
      // persistActiveLesson ya notifica vía servicio
    }
  }

  updateQuizQuestion(index: number, text: string): void {
    this.quizQuestions.update((items) => {
      const next = [...items];
      next[index] = { ...next[index], text };
      return next;
    });
    this.updateBlockQuizData();
  }

  addQuizQuestion(): void {
    this.quizQuestions.update((items) => [
      ...items,
      {
        id: `q-${Date.now()}`,
        text: `Pregunta ${items.length + 1}`,
        options: ['Opción A', 'Opción B'],
        correctIndex: 0,
      },
    ]);
    this.updateBlockQuizData();
  }

  removeQuizQuestion(index: number): void {
    if (this.quizQuestions().length <= 1) {
      return;
    }
    this.quizQuestions.update((items) => items.filter((_, i) => i !== index));
    this.updateBlockQuizData();
  }

  lessonIcon(contentType: ContentType): string {
    return this.blockTypes.find((b) => b.id === contentType)?.icon ?? 'ri-book-line';
  }

  contentTypeLabel(contentType: ContentType): string {
    return this.blockTypes.find((b) => b.id === contentType)?.label ?? contentType;
  }

  blockBgClass(contentType: ContentType): string {
    return this.blockTypes.find((b) => b.id === contentType)?.bgClass ?? 'bg-gray-100';
  }

  blockTypeColor(contentType: ContentType): string {
    const colors: Record<ContentType, string> = {
      text: '#B2B4FF',
      image: '#B4FFB4',
      video: '#FFB4B4',
      pdf: '#D4B4FF',
      quiz: '#FFE0A0',
      audio: '#FFACFC',
    };
    return colors[contentType];
  }

  async addBlockToActiveModule(contentType: ContentType): Promise<void> {
    await this.addContentBlock(contentType);
  }

  requestDeleteContentBlock(blockId: string): void {
    const lesson = this.activeLesson();
    const moduleId = this.curriculum.activeModuleId();
    if (!lesson || !moduleId) {
      return;
    }
    this.openConfirm({
      title: '¿Eliminar bloque?',
      message: 'Esta acción no se puede deshacer.',
      onConfirm: async () => {
        this.clearSaveDebounce();
        await this.curriculum.removeContentBlock(moduleId, lesson.id, blockId);
      },
    });
  }

  requestDeleteLesson(moduleId: string, lessonId: string, event: Event): void {
    event.stopPropagation();
    this.openConfirm({
      title: '¿Eliminar elemento?',
      message: 'Esta acción eliminará permanentemente la lección o evaluación.',
      onConfirm: () => this.removeLesson(moduleId, lessonId),
    });
  }

  startEditLesson(lessonId: string, title: string, event: Event): void {
    event.stopPropagation();
    this.editingLessonId.set(lessonId);
    this.editingLessonTitle.set(title);
  }

  async saveLessonTitle(moduleId: string, lessonId: string): Promise<void> {
    const title = this.editingLessonTitle().trim();
    this.editingLessonId.set(null);
    if (!title) {
      return;
    }

    const lesson = this.curriculum.getLesson(moduleId, lessonId);
    if (!lesson || lesson.title === title) {
      return;
    }

    if (this.curriculum.activeLessonId() === lessonId) {
      this.draftTitle.set(title);
    }

    this.curriculum.updateLesson(moduleId, lessonId, { title });
    try {
      await this.curriculum.persistLesson(moduleId, lessonId, {
        title,
        blocks: lesson.blocks,
      });
    } catch {
      // surfaced via service + toast
    }
  }

  closeConfirmDialog(): void {
    this.confirmDialog.set(null);
  }

  async confirmDialogAction(): Promise<void> {
    const dialog = this.confirmDialog();
    if (!dialog) {
      return;
    }
    const action = dialog.onConfirm;
    this.closeConfirmDialog();
    try {
      await action();
    } catch {
      // El servicio de curriculum muestra toast y revierte estado optimista.
    }
  }

  private openConfirm(state: ConfirmDialogState): void {
    this.confirmDialog.set(state);
  }

  private async removeLesson(moduleId: string, lessonId: string): Promise<void> {
    await this.curriculum.removeLesson(moduleId, lessonId);
  }

  goToEvaluationTab(): void {
    this.activeTab.set('evaluation');
  }

  optionLetter(index: number): string {
    return String.fromCharCode(65 + index);
  }

  moduleLessons(moduleId: string) {
    return this.curriculum.modules().find((m) => m.id === moduleId)?.lessons ?? [];
  }

  lessonBlockCount(lesson: LessonDTO): string {
    const count = lesson.blocks.length;
    if (count === 0) {
      return 'Sin contenido';
    }
    if (count === 1) {
      return '1 bloque';
    }
    return `${count} bloques`;
  }
}
