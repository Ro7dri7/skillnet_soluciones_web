import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  CourseLearnPage,
  LearnLesson,
  LearnModule,
  StudentService,
} from '../../../../core/services/student.service';
import { ContentBlockDTO } from '../../../../shared/models/curriculum.model';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';
import { courseLandingPath, normalizeCourseSlugForUrl } from '../../../../shared/utils/course-slug.util';

@Component({
  selector: 'app-course-learn',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './course-learn.component.html',
  styleUrl: './course-learn.component.scss',
})
export class CourseLearnComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly studentService = inject(StudentService);

  readonly page = signal<CourseLearnPage | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly expandedModules = signal<Set<number>>(new Set());
  readonly activeLessonId = signal<number | null>(null);
  readonly searchQuery = signal('');

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

  readonly canonicalSlug = computed(() => {
    const data = this.page();
    return data ? normalizeCourseSlugForUrl(data.slug) : '';
  });

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (!slug) {
      this.error.set('Curso no encontrado.');
      this.loading.set(false);
      return;
    }
    this.studentService.getLearnPage(slug).subscribe({
      next: (data) => {
        this.page.set(data);
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

  moduleProgress(mod: LearnModule): number {
    if (!mod.lessons.length) return 0;
    return 0;
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

  landingPath(): string {
    const slug = this.canonicalSlug();
    return slug ? courseLandingPath(slug) : '/marketplace';
  }
}
