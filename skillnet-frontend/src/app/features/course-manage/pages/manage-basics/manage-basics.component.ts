import { Component, computed, effect, inject, OnDestroy, OnInit, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpEventType, HttpResponse } from '@angular/common/http';
import { filter, firstValueFrom, map, tap } from 'rxjs';
import { CourseService } from '../../../../core/services/course.service';
import { CourseBuilderService } from '../../../../core/services/course-builder.service';
import { CourseManageContextService } from '../../../../core/services/course-manage-context.service';
import { ManageCurriculumService } from '../../../../core/services/manage-curriculum.service';
import { ManageLayoutSaveService } from '../../../../core/services/manage-layout-save.service';
import {
  CourseMediaService,
  CourseMediaUploadResponse,
} from '../../../../core/services/course-media.service';
import { ProducerCoursesService } from '../../../../core/services/producer-courses.service';
import { ToastService } from '../../../../core/services/toast.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';
import {
  OFFICIAL_CATEGORIES,
  OFFICIAL_LANGUAGES,
  OFFICIAL_LEVELS,
  getSubcategories,
  isOfficialCategory,
} from '../../../marketplace/data/categories.data';

interface MissingItem {
  text: string;
  link: string;
}

@Component({
  selector: 'app-manage-basics',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './manage-basics.component.html',
  styleUrl: './manage-basics.component.scss',
})
export class ManageBasicsComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly manageContext = inject(CourseManageContextService);
  private readonly courseService = inject(CourseService);
  private readonly producerCourses = inject(ProducerCoursesService);
  private readonly courseMedia = inject(CourseMediaService);
  private readonly builder = inject(CourseBuilderService);
  private readonly curriculum = inject(ManageCurriculumService);
  private readonly toast = inject(ToastService);
  private readonly manageSave = inject(ManageLayoutSaveService);

  readonly loading = signal(true);
  readonly maxTitleChars = 60;

  readonly categories = OFFICIAL_CATEGORIES;
  readonly languages = OFFICIAL_LANGUAGES;
  readonly levels = OFFICIAL_LEVELS;

  readonly imagePreviewOverride = signal<string | null>(null);
  readonly imageUploadProgress = signal(0);
  readonly isImageUploading = signal(false);
  readonly imageFileName = signal<string | null>(null);

  readonly isVideoUploading = signal(false);
  readonly videoUploadProgress = signal(0);
  readonly videoFileName = signal<string | null>(null);

  private pendingImageFile: File | null = null;
  private pendingVideoFile: File | null = null;
  private objectPreviewUrl: string | null = null;

  readonly courseMeta = signal({
    whatYouWillLearn: '',
    targetAudience: '',
    price: 0,
    moduleCount: 0,
    lessonCount: 0,
  });

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(60)]],
    description: ['', [Validators.required, Validators.maxLength(5000)]],
    imageUrl: ['', [Validators.maxLength(500)]],
    videoUrl: ['', [Validators.maxLength(500)]],
    language: ['es', [Validators.required]],
    level: ['beginner', [Validators.required]],
    category: ['', [Validators.required]],
    subcategory: [''],
  });

  readonly titleLength = computed(() => this.form.controls.title.value.length);

  readonly coverPreview = computed(() => {
    const override = this.imagePreviewOverride();
    if (override) {
      return override;
    }
    const url = this.form.controls.imageUrl.value.trim();
    return url || null;
  });

  readonly videoPreview = computed(() => this.form.controls.videoUrl.value.trim() || null);

  readonly subcategoryOptions = computed(() => {
    const category = this.form.controls.category.value;
    if (!category || !isOfficialCategory(category)) {
      return [] as readonly string[];
    }
    return getSubcategories(category);
  });

  readonly manageBasePath = this.manageContext.manageBasePath;

  readonly missingItems = computed((): MissingItem[] => {
    const base = this.manageBasePath();
    const raw = this.form.getRawValue();
    const meta = this.courseMeta();
    const items: MissingItem[] = [];

    if (!raw.title.trim()) {
      items.push({ text: 'Título del curso', link: '#' });
    }
    if (!raw.description.trim()) {
      items.push({ text: 'Descripción del curso', link: '#' });
    }
    if (!raw.category) {
      items.push({ text: 'Categoría del curso', link: '#' });
    }
    if (!this.coverPreview()) {
      items.push({ text: 'Imagen de portada', link: '#' });
    }
    if (!meta.whatYouWillLearn.trim() || !meta.targetAudience.trim()) {
      items.push({
        text: 'Audiencia y Objetivos del curso',
        link: `${base}/audience`,
      });
    }
    if (meta.lessonCount === 0) {
      items.push({
        text: 'Diseñar el temario (módulos y lecciones)',
        link: `${base}/curriculum`,
      });
    }
    if (meta.price <= 0) {
      items.push({ text: 'Asignar un precio', link: `${base}/pricing` });
    }

    return items;
  });

  private loadedCourseId: number | null = null;

  constructor() {
    effect(() => {
      const id = this.manageContext.courseId();
      if (id != null && id !== this.loadedCourseId) {
        this.loadedCourseId = id;
        untracked(() => void this.loadCourse(id));
      }
    });
  }

  ngOnInit(): void {
    this.manageSave.registerSaveHandler(() => this.persistBasics());
  }

  ngOnDestroy(): void {
    this.manageSave.unregisterSaveHandler();
    this.revokeObjectPreview();
  }

  onCategoryChange(category: string): void {
    this.form.patchValue({ category, subcategory: '' });
  }

  onImageFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    if (!file.type.startsWith('image/')) {
      this.toast.error('Selecciona un archivo .jpg o .png.');
      return;
    }

    this.pendingImageFile = file;
    this.imageFileName.set(file.name);
    this.revokeObjectPreview();
    this.objectPreviewUrl = URL.createObjectURL(file);
    this.imagePreviewOverride.set(this.objectPreviewUrl);
    input.value = '';
    void this.uploadCover(file);
  }

  onVideoFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    if (!file.type.startsWith('video/')) {
      this.toast.error('Selecciona un archivo de vídeo válido.');
      return;
    }

    this.pendingVideoFile = file;
    this.videoFileName.set(file.name);
    input.value = '';
    void this.uploadPromoVideo(file);
  }

  private async uploadCover(file: File): Promise<void> {
    const courseId = this.manageContext.courseId();
    if (courseId == null) {
      return;
    }
    this.isImageUploading.set(true);
    this.imageUploadProgress.set(0);
    try {
      await this.builder.ensureInfoproductorSession();
      const result = await firstValueFrom(
        this.courseMedia.upload(courseId, 'cover', file).pipe(
          tap(({ progress }) => this.imageUploadProgress.set(progress)),
          filter(({ event }) => event.type === HttpEventType.Response),
          map(({ event }) => (event as HttpResponse<CourseMediaUploadResponse>).body!),
        ),
      );
      if (result.imageUrl) {
        this.form.patchValue({ imageUrl: result.imageUrl });
        this.imagePreviewOverride.set(result.imageUrl);
        this.pendingImageFile = null;
        this.toast.success('Imagen de portada guardada');
      }
    } catch (err) {
      this.toast.error(messageFromHttpError(err, 'No se pudo subir la imagen de portada.'));
    } finally {
      this.isImageUploading.set(false);
      this.imageUploadProgress.set(0);
    }
  }

  private async uploadPromoVideo(file: File): Promise<void> {
    const courseId = this.manageContext.courseId();
    if (courseId == null) {
      return;
    }
    this.isVideoUploading.set(true);
    this.videoUploadProgress.set(0);
    try {
      await this.builder.ensureInfoproductorSession();
      const result = await firstValueFrom(
        this.courseMedia.upload(courseId, 'promo_video', file).pipe(
          tap(({ progress }) => this.videoUploadProgress.set(progress)),
          filter(({ event }) => event.type === HttpEventType.Response),
          map(({ event }) => (event as HttpResponse<CourseMediaUploadResponse>).body!),
        ),
      );
      if (result.videoUrl) {
        this.form.patchValue({ videoUrl: result.videoUrl });
        this.pendingVideoFile = null;
        this.toast.success('Vídeo promocional guardado');
      }
    } catch (err) {
      this.toast.error(messageFromHttpError(err, 'No se pudo subir el vídeo promocional.'));
    } finally {
      this.isVideoUploading.set(false);
      this.videoUploadProgress.set(0);
    }
  }

  private revokeObjectPreview(): void {
    if (this.objectPreviewUrl) {
      URL.revokeObjectURL(this.objectPreviewUrl);
      this.objectPreviewUrl = null;
    }
  }

  private async loadCourse(id: number): Promise<void> {
    this.loading.set(true);
    try {
      await this.builder.ensureInfoproductorSession();
      const [course] = await Promise.all([
        firstValueFrom(this.courseService.getCourse(id)),
        this.curriculum.loadCurriculumAsync(id, true),
      ]);

      const modules = this.curriculum.modules();
      const lessonCount = modules.reduce((sum, m) => sum + m.lessons.length, 0);

      this.courseMeta.set({
        whatYouWillLearn: course.whatYouWillLearn ?? '',
        targetAudience: course.targetAudience ?? '',
        price: typeof course.price === 'number' ? course.price : Number(course.price) || 0,
        moduleCount: modules.length,
        lessonCount,
      });

      this.form.patchValue({
        title: course.title ?? '',
        description: course.description ?? '',
        imageUrl: course.imageUrl ?? '',
        videoUrl: course.videoUrl ?? '',
        language: course.language ?? 'es',
        level: course.level ?? 'beginner',
        category: course.category ?? '',
        subcategory: course.subcategory ?? '',
      });
      this.imagePreviewOverride.set(null);
    } catch (err) {
      this.toast.error(messageFromHttpError(err, 'No se pudo cargar la información básica.'));
    } finally {
      this.loading.set(false);
    }
  }

  private async persistBasics(): Promise<void> {
    const courseId = this.manageContext.courseId();
    if (this.form.invalid || courseId == null) {
      this.form.markAllAsTouched();
      throw new Error('Formulario inválido');
    }

    if (this.pendingImageFile) {
      await this.uploadCover(this.pendingImageFile);
    }
    if (this.pendingVideoFile) {
      await this.uploadPromoVideo(this.pendingVideoFile);
    }

    await this.builder.ensureInfoproductorSession();
    const raw = this.form.getRawValue();
    await firstValueFrom(
      this.producerCourses.updateBasics(courseId, {
        title: raw.title.trim(),
        description: raw.description.trim(),
        imageUrl: raw.imageUrl.trim(),
        videoUrl: raw.videoUrl.trim(),
        language: raw.language,
        level: raw.level,
        category: raw.category,
        subcategory: raw.subcategory.trim(),
      }),
    );
    this.builder.setTitle(raw.title.trim());
  }
}
