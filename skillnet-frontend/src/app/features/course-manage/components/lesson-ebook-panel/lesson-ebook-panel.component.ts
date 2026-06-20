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
  untracked,
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import {
  GammaService,
  type GammaFormat,
  type GammaImageSource,
  type GammaTextAmount,
} from '../../../../core/services/gamma.service';
import { ProducerPlansService } from '../../../../core/services/producer-plans.service';
import { MediaService } from '../../../../core/services/media.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';
import {
  absoluteMediaUrl,
  mediaPreviewFetchUrl,
} from '../../../../shared/utils/media-url.util';

type GammaPhase = 'idle' | 'starting' | 'generating' | 'exporting' | 'completed' | 'failed';
type PanelMode = 'generate' | 'upload';

@Component({
  selector: 'app-lesson-ebook-panel',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './lesson-ebook-panel.component.html',
  styleUrl: './lesson-ebook-panel.component.scss',
})
export class LessonEbookPanelComponent implements OnInit, OnDestroy {
  private readonly gammaService = inject(GammaService);
  private readonly http = inject(HttpClient);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly producerPlansService = inject(ProducerPlansService);
  private readonly mediaService = inject(MediaService);

  readonly courseId = input<number | null>(null);
  readonly lessonId = input<number | null>(null);
  readonly lessonTitle = input<string>('');
  /** PDF ya guardado en el bloque de la lección (persistido en el temario). */
  readonly attachedPdfUrl = input<string | null>(null);

  readonly attachPdf = output<string>();
  readonly removeAttached = output<void>();

  readonly replaceRequested = signal(false);
  readonly showAttachedView = computed(() => {
    const attached = this.attachedPdfUrl()?.trim();
    return Boolean(attached) && !this.replaceRequested();
  });

  readonly error = signal<string | null>(null);
  readonly hasGammaAccess = signal(false);
  readonly gammaUsesRemaining = signal(0);
  readonly panelMode = signal<PanelMode>('generate');
  readonly phase = signal<GammaPhase>('idle');
  readonly gammaDevMock = signal(false);
  readonly platformPdfUrl = signal<string | null>(null);
  readonly previewBlobUrl = signal<SafeResourceUrl | null>(null);
  readonly previewLoading = signal(false);
  readonly referenceExtracting = signal(false);
  readonly referenceFileName = signal<string | null>(null);
  readonly uploadProgress = signal(0);
  readonly uploadingOwnPdf = signal(false);
  readonly uploadingFileName = signal<string | null>(null);

  gammaPrompt = '';
  gammaTitle = '';
  gammaPages = 10;
  gammaFormat: GammaFormat = 'document';
  gammaLanguage = 'es';
  gammaTone = 'profesional';
  gammaAudience = '';
  gammaTextAmount: GammaTextAmount = 'medium';
  gammaImageSource: GammaImageSource = 'aiGenerated';
  gammaImageStyle = '';
  gammaAdditionalInstructions = '';
  sourceMaterial = '';

  private previewObjectUrl: string | null = null;
  private lastLessonId: number | null = null;

  readonly phases: { id: GammaPhase; label: string }[] = [
    { id: 'starting', label: 'Iniciando' },
    { id: 'generating', label: 'Generando contenido' },
    { id: 'exporting', label: 'Preparando PDF' },
    { id: 'completed', label: 'Completado' },
  ];

  constructor() {
    effect(() => {
      const lessonId = this.lessonId();
      const attached = this.attachedPdfUrl()?.trim() || null;

      if (this.lastLessonId !== null && this.lastLessonId !== lessonId) {
        untracked(() => {
          this.replaceRequested.set(false);
          this.resetPreview();
          this.phase.set('idle');
          this.gammaPrompt = '';
          this.error.set(null);
        });
      }
      this.lastLessonId = lessonId;

      if (attached && !this.replaceRequested()) {
        untracked(() => this.finishGeneration(attached, false));
      }
    });
  }

  ngOnInit(): void {
    this.producerPlansService.capabilities().subscribe({
      next: (caps) => {
        this.hasGammaAccess.set(caps['gamma_ebook']?.active === true);
        this.gammaUsesRemaining.set(caps['gamma_ebook']?.usesRemaining ?? 0);
      },
      error: () => this.hasGammaAccess.set(false),
    });
  }

  ngOnDestroy(): void {
    this.revokePreviewBlob();
  }

  setMode(mode: PanelMode): void {
    this.panelMode.set(mode);
    this.error.set(null);
  }

  onReferencePdfSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }
    this.referenceExtracting.set(true);
    this.error.set(null);
    this.gammaService.extractReferencePdf(file).subscribe({
      next: (res) => {
        this.sourceMaterial = res.extractedText;
        this.referenceFileName.set(file.name);
        this.referenceExtracting.set(false);
      },
      error: (err) => {
        this.referenceExtracting.set(false);
        this.referenceFileName.set(null);
        this.sourceMaterial = '';
        this.error.set(messageFromHttpError(err, 'No se pudo leer el PDF de referencia.'));
      },
    });
  }

  clearReference(): void {
    this.sourceMaterial = '';
    this.referenceFileName.set(null);
  }

  onUploadOwnPdf(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }
    const courseId = this.courseId();
    if (!courseId) {
      this.error.set('Curso no cargado.');
      return;
    }
    this.uploadingOwnPdf.set(true);
    this.uploadingFileName.set(file.name);
    this.uploadProgress.set(0);
    this.phase.set('starting');
    this.error.set(null);
    this.mediaService.uploadLessonResourceWithProgress(courseId, file, 'pdf').subscribe({
      next: ({ progress, response }) => {
        this.uploadProgress.set(progress);
        if (response?.url) {
          this.uploadingOwnPdf.set(false);
          this.attachPdf.emit(response.url);
          this.phase.set('completed');
          this.error.set(null);
        }
      },
      error: (err) => {
        this.uploadingOwnPdf.set(false);
        this.uploadProgress.set(0);
        this.phase.set('failed');
        this.error.set(messageFromHttpError(err, 'No se pudo subir el PDF.'));
      },
    });
  }

  startGamma(): void {
    const courseId = this.courseId();
    const lessonId = this.lessonId();
    if (!courseId || !lessonId) {
      this.error.set('Selecciona una lección guardada antes de generar.');
      return;
    }
    const prompt = this.gammaPrompt.trim();
    if (!prompt) {
      this.error.set('Describe el tema de esta lección.');
      return;
    }

    this.error.set(null);
    this.resetPreview();
    this.phase.set('starting');

    const title =
      this.gammaTitle.trim() ||
      (this.lessonTitle() ? `${this.lessonTitle()} — ebook` : 'Ebook SkillNet');

    this.gammaService
      .start({
        prompt,
        title,
        pages: this.gammaPages,
        format: this.gammaFormat,
        courseId,
        lessonId,
        language: this.gammaLanguage,
        tone: this.gammaTone.trim() || undefined,
        audience: this.gammaAudience.trim() || undefined,
        textAmount: this.gammaTextAmount,
        imageSource: this.gammaImageSource,
        imageStyle: this.gammaImageStyle.trim() || undefined,
        additionalInstructions: this.gammaAdditionalInstructions.trim() || undefined,
        sourceMaterial: this.sourceMaterial.trim() || undefined,
      })
      .subscribe({
        next: (res) => {
          const pdfUrl = this.pickPdfUrl(res);
          if (pdfUrl) {
            this.finishGeneration(pdfUrl, this.isDevMockResponse(res));
            return;
          }
          const id = res.id;
          if (!id) {
            this.phase.set('failed');
            this.error.set('Gamma no devolvió un id de generación.');
            return;
          }
          this.phase.set('generating');
          this.pollGamma(id, 0, this.isDevMockResponse(res));
        },
        error: (err) => {
          this.phase.set('failed');
          this.error.set(messageFromHttpError(err, 'No se pudo iniciar la generación.'));
        },
      });
  }

  assignToLesson(): void {
    const url = this.platformPdfUrl();
    if (url) {
      this.attachPdf.emit(url);
      this.replaceRequested.set(false);
    }
  }

  requestReplace(): void {
    this.replaceRequested.set(true);
    this.resetPreview();
    this.phase.set('idle');
    this.gammaPrompt = '';
    this.error.set(null);
  }

  detachFromLesson(): void {
    this.removeAttached.emit();
    this.requestReplace();
  }

  resetPanel(): void {
    this.requestReplace();
  }

  gammaDownloadUrl(): string {
    return absoluteMediaUrl(this.platformPdfUrl());
  }

  phaseIndex(): number {
    const order: GammaPhase[] = ['starting', 'generating', 'exporting', 'completed'];
    return order.indexOf(this.phase());
  }

  isPhaseDone(step: GammaPhase): boolean {
    const order: GammaPhase[] = ['starting', 'generating', 'exporting', 'completed'];
    return order.indexOf(step) < order.indexOf(this.phase());
  }

  isPhaseActive(step: GammaPhase): boolean {
    return this.phase() === step;
  }

  isGenerating(): boolean {
    const p = this.phase();
    return p === 'starting' || p === 'generating' || p === 'exporting';
  }

  private pollGamma(generationId: string, attempt: number, devMock: boolean): void {
    this.gammaService.status(generationId).subscribe({
      next: (res) => {
        const status = (res.status ?? '').toLowerCase();
        const pdfUrl = this.pickPdfUrl(res);

        if (pdfUrl) {
          this.finishGeneration(pdfUrl, devMock || this.isDevMockResponse(res));
          return;
        }

        if (status === 'completed' || status === 'complete') {
          this.phase.set('exporting');
        } else if (status) {
          this.phase.set('generating');
        }

        if (status === 'failed' || status === 'error') {
          this.phase.set('failed');
          this.error.set('La generación en Gamma falló. Intenta de nuevo.');
          return;
        }

        if (attempt >= 60) {
          this.phase.set('failed');
          this.error.set('La generación tardó demasiado. Intenta de nuevo.');
          return;
        }

        setTimeout(() => this.pollGamma(generationId, attempt + 1, devMock), 5000);
      },
      error: (err) => {
        this.phase.set('failed');
        this.error.set(messageFromHttpError(err, 'Error consultando Gamma.'));
      },
    });
  }

  private finishGeneration(pdfUrl: string, devMock: boolean): void {
    this.platformPdfUrl.set(pdfUrl);
    this.gammaDevMock.set(devMock);
    this.phase.set('completed');
    this.loadPreviewBlob(pdfUrl);
  }

  private resetPreview(): void {
    this.revokePreviewBlob();
    this.platformPdfUrl.set(null);
    this.gammaDevMock.set(false);
  }

  private loadPreviewBlob(pdfUrl: string): void {
    this.revokePreviewBlob();
    const fetchUrl = mediaPreviewFetchUrl(pdfUrl);
    if (!fetchUrl) {
      return;
    }
    this.previewLoading.set(true);
    this.http.get(fetchUrl, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        this.previewLoading.set(false);
        if (blob.size === 0) {
          return;
        }
        const typed =
          blob.type === 'application/pdf' || blob.type === ''
            ? new Blob([blob], { type: 'application/pdf' })
            : blob;
        this.previewObjectUrl = URL.createObjectURL(typed);
        this.previewBlobUrl.set(
          this.sanitizer.bypassSecurityTrustResourceUrl(this.previewObjectUrl),
        );
      },
      error: () => {
        this.previewLoading.set(false);
      },
    });
  }

  private revokePreviewBlob(): void {
    if (this.previewObjectUrl) {
      URL.revokeObjectURL(this.previewObjectUrl);
      this.previewObjectUrl = null;
    }
    this.previewBlobUrl.set(null);
    this.previewLoading.set(false);
  }

  private pickPdfUrl(res: {
    platformExportUrl?: string | null;
    exportUrl?: string | null;
  }): string | null {
    if (res.platformExportUrl?.trim()) {
      return res.platformExportUrl.trim();
    }
    const exportUrl = res.exportUrl?.trim();
    if (!exportUrl) {
      return null;
    }
    if (exportUrl.includes('/api/v1/media/files/') || exportUrl.startsWith('courses/')) {
      return exportUrl;
    }
    if (/^https?:\/\//i.test(exportUrl)) {
      return exportUrl;
    }
    return null;
  }

  private isDevMockResponse(res: {
    id?: string | null;
    raw?: Record<string, unknown> | null;
  }): boolean {
    if (res.raw?.['mode'] === 'dev_mock') {
      return true;
    }
    return (res.id ?? '').startsWith('dev_gamma_');
  }
}
