import {
  Component,
  ElementRef,
  OnInit,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  CourseLearnPage,
  LearnLesson,
  LearnModule,
  StudentService,
} from '../../../../core/services/student.service';
import { ContentBlockDTO, QuizData } from '../../../../shared/models/curriculum.model';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';
import { absoluteMediaUrl, mediaBackendUrl } from '../../../../shared/utils/media-url.util';
import { normalizeQuizData } from '../../../course-manage/utils/quiz.util';
import { LearnQuizPlayerComponent } from '../../components/learn-quiz-player/learn-quiz-player.component';
import { PdfBlockViewerComponent } from '../../components/pdf-block-viewer/pdf-block-viewer.component';

interface FlatLesson {
  lesson: LearnLesson;
  moduleId: number;
}

@Component({
  selector: 'app-course-learn',
  standalone: true,
  imports: [RouterLink, PdfBlockViewerComponent, LearnQuizPlayerComponent],
  templateUrl: './course-learn.component.html',
  styleUrl: './course-learn.component.scss',
})
export class CourseLearnComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly studentService = inject(StudentService);
  private readonly sanitizer = inject(DomSanitizer);

  private readonly contentAreaRef = viewChild<ElementRef<HTMLElement>>('contentAreaRef');

  readonly page = signal<CourseLearnPage | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly expandedModules = signal<Set<number>>(new Set());
  readonly activeLessonId = signal<number | null>(null);
  readonly searchQuery = signal('');
  readonly sidebarCollapsed = signal(false);
  readonly completedLessonIds = signal<Set<number>>(new Set());
  readonly savingProgress = signal(false);

  private courseSlug = '';

  readonly activeLesson = computed(() => {
    const lessonId = this.activeLessonId();
    const data = this.page();
    if (!lessonId || !data) return null;
    for (const mod of data.modules) {
      const hit = mod.lessons.find((l) => l.id === lessonId);
      if (hit) return hit;
    }
    return null;
  });

  readonly allLessonsFlat = computed((): FlatLesson[] => {
    const data = this.page();
    if (!data) return [];
    return data.modules.flatMap((mod) =>
      mod.lessons.map((lesson) => ({ lesson, moduleId: mod.id })),
    );
  });

  readonly currentLessonIndex = computed(() => {
    const id = this.activeLessonId();
    if (id == null) return -1;
    return this.allLessonsFlat().findIndex((item) => item.lesson.id === id);
  });

  readonly courseProgressPercent = computed(() => {
    const total = this.allLessonsFlat().length;
    if (total === 0) return 0;
    const done = this.completedLessonIds().size;
    return Math.round((done / total) * 100);
  });

  readonly activeLessonHasPdf = computed(() => {
    const lesson = this.activeLesson();
    if (!lesson) return false;
    return this.lessonBlocks(lesson).some((b) => b.contentType === 'pdf' && b.resourceUrl);
  });

  readonly filteredModules = computed(() => {
    const data = this.page();
    const q = this.searchQuery().trim().toLowerCase();
    if (!data) return [] as LearnModule[];
    if (!q) return data.modules;
    return data.modules
      .map((mod) => ({
        ...mod,
        lessons: mod.lessons.filter(
          (l) =>
            l.title.toLowerCase().includes(q) || mod.title.toLowerCase().includes(q),
        ),
      }))
      .filter((mod) => mod.lessons.length > 0 || mod.title.toLowerCase().includes(q));
  });

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (!slug) {
      this.error.set('Curso no encontrado.');
      this.loading.set(false);
      return;
    }
    this.courseSlug = slug;
    this.studentService.getLearnPage(slug).subscribe({
      next: (data) => {
        this.page.set(data);
        this.completedLessonIds.set(new Set(data.completedLessonIds ?? []));
        this.loading.set(false);
        const firstLesson = data.modules.flatMap((m) => m.lessons)[0];
        if (firstLesson) {
          this.activeLessonId.set(firstLesson.id);
          this.expandedModules.set(new Set([data.modules[0]?.id].filter(Boolean) as number[]));
        }
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo cargar el curso.'));
        this.loading.set(false);
      },
    });
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update((v) => !v);
  }

  toggleModule(moduleId: number): void {
    this.expandedModules.update((prev) => {
      const next = new Set(prev);
      if (next.has(moduleId)) {
        next.delete(moduleId);
      } else {
        next.add(moduleId);
      }
      return next;
    });
  }

  isModuleExpanded(moduleId: number): boolean {
    return this.expandedModules().has(moduleId);
  }

  selectLesson(lesson: LearnLesson, moduleId: number): void {
    this.activeLessonId.set(lesson.id);
    this.expandedModules.update((prev) => new Set(prev).add(moduleId));
  }

  goPrevLesson(): void {
    const idx = this.currentLessonIndex();
    if (idx <= 0) return;
    const prev = this.allLessonsFlat()[idx - 1];
    this.selectLesson(prev.lesson, prev.moduleId);
  }

  goNextLesson(): void {
    const idx = this.currentLessonIndex();
    const flat = this.allLessonsFlat();
    if (idx < 0 || flat.length === 0) return;

    const currentId = this.activeLessonId();
    if (currentId == null) return;

    const advance = (): void => {
      if (idx < flat.length - 1) {
        const next = flat[idx + 1];
        this.selectLesson(next.lesson, next.moduleId);
      }
    };

    if (this.completedLessonIds().has(currentId)) {
      advance();
      return;
    }

    this.savingProgress.set(true);
    this.studentService.markLessonComplete(this.courseSlug, currentId).subscribe({
      next: (result) => {
        this.completedLessonIds.update((set) => new Set(set).add(result.lessonId));
        this.savingProgress.set(false);
        advance();
      },
      error: () => {
        this.savingProgress.set(false);
        advance();
      },
    });
  }

  canGoPrev(): boolean {
    return this.currentLessonIndex() > 0;
  }

  canGoNext(): boolean {
    const idx = this.currentLessonIndex();
    return idx >= 0 && idx < this.allLessonsFlat().length - 1;
  }

  toggleContentFullscreen(): void {
    const el = this.contentAreaRef()?.nativeElement;
    if (!el) return;
    if (!document.fullscreenElement) {
      void el.requestFullscreen().catch(() => undefined);
    } else {
      void document.exitFullscreen();
    }
  }

  exitCourse(): void {
    void this.router.navigate(['/mis-cursos']);
  }

  moduleProgress(mod: LearnModule): number {
    if (!mod.lessons.length) return 0;
    const completed = this.completedLessonIds();
    const done = mod.lessons.filter((l) => completed.has(l.id)).length;
    return Math.round((done / mod.lessons.length) * 100);
  }

  lessonBlocks(lesson: LearnLesson): ContentBlockDTO[] {
    if (lesson.blocks?.length) {
      return [...lesson.blocks].sort((a, b) => a.orderIndex - b.orderIndex);
    }
    return [
      {
        id: `legacy-${lesson.id}`,
        contentType: this.normalizeContentType(lesson.contentType),
        resourceUrl: lesson.resourceUrl ?? '',
        textContent: lesson.textContent ?? '',
        orderIndex: 0,
        quizData: lesson.quizData,
      },
    ];
  }

  normalizeContentType(value: string): ContentBlockDTO['contentType'] {
    const allowed: ContentBlockDTO['contentType'][] = [
      'text',
      'image',
      'video',
      'pdf',
      'quiz',
      'audio',
    ];
    return allowed.includes(value as ContentBlockDTO['contentType'])
      ? (value as ContentBlockDTO['contentType'])
      : 'text';
  }

  isVideoUrl(url: string): boolean {
    return /\.(mp4|webm|ogg|mov)(\?|$)/i.test(url) || url.includes('video');
  }

  trustedResourceUrl(url: string | null | undefined): SafeResourceUrl | null {
    const resolved = mediaBackendUrl(url);
    if (!resolved) {
      return null;
    }
    return this.sanitizer.bypassSecurityTrustResourceUrl(resolved);
  }

  mediaUrl(url: string | null | undefined): string {
    return mediaBackendUrl(url);
  }

  mediaAbsoluteUrl(url: string | null | undefined): string {
    return absoluteMediaUrl(url);
  }

  quizDataForBlock(block: ContentBlockDTO): QuizData | null {
    if (!block.quizData) {
      return null;
    }
    return normalizeQuizData({
      passingScore: block.quizData.passingScore,
      timeLimitMinutes: block.quizData.timeLimitMinutes,
      maxAttempts: block.quizData.maxAttempts ?? 3,
      questions: block.quizData.questions ?? [],
    });
  }

  onQuizPassed(lessonId: number): void {
    if (this.completedLessonIds().has(lessonId)) {
      return;
    }
    this.studentService.markLessonComplete(this.courseSlug, lessonId).subscribe({
      next: (result) => {
        this.completedLessonIds.update((set) => new Set(set).add(result.lessonId));
      },
    });
  }
}
