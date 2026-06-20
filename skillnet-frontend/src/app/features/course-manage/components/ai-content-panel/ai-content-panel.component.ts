import { Component, inject, input, OnDestroy, OnInit, output, signal } from '@angular/core';

import { HttpClient } from '@angular/common/http';

import { FormsModule } from '@angular/forms';

import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { RouterLink } from '@angular/router';

import { GammaService } from '../../../../core/services/gamma.service';

import { PodcastAiService } from '../../../../core/services/podcast-ai.service';

import { ProducerPlansService } from '../../../../core/services/producer-plans.service';

import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

import { absoluteMediaUrl, mediaPreviewFetchUrl } from '../../../../shared/utils/media-url.util';



@Component({

  selector: 'app-ai-content-panel',

  standalone: true,

  imports: [FormsModule, RouterLink],

  template: `

    <section class="ai-panel">

      @if (courseFormat() === 'ebook') {

        <h3>Generar ebook con IA (Gamma)</h3>

        @if (!hasGammaAccess()) {

          <p class="locked">

            Necesitas un plan activo para generar ebooks.

            <a routerLink="/infoproductor/plans">Ver planes</a>

          </p>

        } @else if (!platformPdfUrl()) {

          <p class="hint">

            Describe el tema del ebook. Se generará un PDF almacenado en SkillNet.

            @if (gammaUsesRemaining() > 0) {

              <span> · {{ gammaUsesRemaining() }} usos restantes</span>

            }

          </p>

          <textarea

            rows="3"

            class="ai-input"

            [(ngModel)]="gammaPrompt"

            placeholder="Ej: Guía práctica de dropshipping para principiantes en LATAM"

          ></textarea>

          <div class="ai-actions">

            <button type="button" class="ai-btn" [disabled]="gammaLoading()" (click)="startGamma()">

              {{ gammaLoading() ? 'Generando…' : 'Generar ebook' }}

            </button>

          </div>

          @if (gammaLoading() && gammaStatusText()) {

            <p class="status">{{ gammaStatusText() }}</p>

          }

        } @else {

          <div class="gamma-ready">

            <p class="gamma-ready__title">

              <i class="ri-checkbox-circle-line"></i>

              Tu ebook está listo

            </p>

            @if (gammaDevMock()) {

              <p class="gamma-dev-notice">

                <strong>Modo desarrollo.</strong> No hay <code>GAMMA_API_KEY</code> configurada: solo se genera un PDF

                de prueba con el título del tema. Para un ebook completo con IA (como en Lernymart), define

                <code>GAMMA_API_KEY</code> en el backend y reinicia el servidor.

              </p>

            }

            <div class="gamma-preview">

              <div class="gamma-preview__bar">Vista previa (PDF)</div>

              @if (previewLoading()) {

                <p class="gamma-preview__loading">Cargando vista previa…</p>

              } @else if (previewBlobUrl(); as previewSrc) {

              <iframe

                title="Vista previa del PDF"

                [src]="previewSrc"

                class="gamma-preview__frame"

              ></iframe>

              } @else {

                <p class="gamma-preview__loading">No se pudo cargar la vista previa del PDF.</p>

              }

            </div>

            <div class="gamma-ready__actions">

              <a

                [href]="gammaDownloadUrl()"

                target="_blank"

                rel="noopener"

                class="ai-btn secondary"

              >

                Descargar PDF

              </a>

              @if (activeLessonId()) {

                <button type="button" class="ai-btn" (click)="attachGammaToLesson()">

                  Usar PDF en la lección activa

                </button>

              } @else {

                <p class="hint">Selecciona una lección en el panel derecho para adjuntar el PDF.</p>

              }

              <button type="button" class="ai-btn ghost" (click)="resetGamma()">Generar otro</button>

            </div>

          </div>

        }

      }



      @if (courseFormat() === 'podcast') {

        <h3>Generar podcast con IA (ElevenLabs)</h3>

        @if (!hasPodcastAccess()) {

          <p class="locked">

            Necesitas un plan activo para generar podcasts.

            <a routerLink="/infoproductor/plans">Ver planes</a>

          </p>

        } @else {

          <p class="hint">

            Indica el tema del episodio. Se creará un guion y audio MP3.

            @if (podcastUsesRemaining() > 0) {

              <span> · {{ podcastUsesRemaining() }} usos restantes</span>

            }

          </p>

          <input

            type="text"

            class="ai-input"

            [(ngModel)]="podcastTopic"

            placeholder="Título del episodio"

          />

          <textarea

            rows="3"

            class="ai-input"

            [(ngModel)]="podcastText"

            placeholder="Contenido fuente opcional (notas, outline, etc.)"

          ></textarea>

          <div class="ai-actions">

            <button type="button" class="ai-btn" [disabled]="podcastLoading()" (click)="startPodcast()">

              {{ podcastLoading() ? 'Generando…' : 'Generar podcast' }}

            </button>

          </div>

          @if (podcastAudioUrl()) {

            <p class="success">

              <audio controls [src]="podcastAudioUrl()!"></audio>

            </p>

          }

          @if (activeLessonId()) {

            <button

              type="button"

              class="ai-btn secondary"

              [disabled]="!podcastJobId() || podcastLoading()"

              (click)="attachPodcast()"

            >

              Adjuntar audio a la lección activa

            </button>

          }

        }

      }



      @if (error()) {

        <p class="error">{{ error() }}</p>

      }

    </section>

  `,

  styles: [

    `

      .ai-panel {

        margin-bottom: 1rem;

        border: 1px solid #dbeafe;

        border-radius: 12px;

        background: #f8fbff;

        padding: 1rem;

      }

      h3 {

        margin: 0 0 0.35rem;

        font-size: 0.95rem;

        font-weight: 700;

        color: #032b60;

      }

      .hint {

        margin: 0 0 0.75rem;

        font-size: 0.8rem;

        color: #64748b;

      }

      .status {

        margin-top: 0.5rem;

        font-size: 0.78rem;

        color: #475569;

      }

      .ai-input {

        width: 100%;

        margin-bottom: 0.5rem;

        border: 1px solid #cbd5e1;

        border-radius: 8px;

        padding: 0.5rem 0.65rem;

        font-size: 0.85rem;

      }

      .ai-actions {

        display: flex;

        gap: 0.5rem;

        flex-wrap: wrap;

      }

      .ai-btn {

        border: none;

        border-radius: 8px;

        background: #145bff;

        color: #fff;

        font-size: 0.8rem;

        font-weight: 700;

        padding: 0.45rem 0.85rem;

        cursor: pointer;

        text-decoration: none;

        display: inline-flex;

        align-items: center;

      }

      .ai-btn.secondary {

        background: #032b60;

        margin-top: 0.5rem;

      }

      .ai-btn.ghost {

        background: transparent;

        color: #475569;

        border: 1px solid #cbd5e1;

      }

      .ai-btn:disabled {

        opacity: 0.6;

        cursor: not-allowed;

      }

      .gamma-ready__title {

        display: flex;

        align-items: center;

        gap: 0.35rem;

        margin: 0 0 0.75rem;

        font-size: 0.95rem;

        font-weight: 700;

        color: #0f172a;

      }

      .gamma-ready__title i {

        color: #059669;

        font-size: 1.1rem;

      }

      .gamma-dev-notice {

        margin: 0 0 0.75rem;

        padding: 0.6rem 0.75rem;

        border-radius: 8px;

        border: 1px solid #fde68a;

        background: #fffbeb;

        font-size: 0.78rem;

        line-height: 1.45;

        color: #92400e;

      }

      .gamma-dev-notice code {

        font-size: 0.72rem;

        background: #fef3c7;

        padding: 0 0.2rem;

        border-radius: 3px;

      }

      .gamma-preview {

        position: relative;

        height: 360px;

        border: 1px solid #e2e8f0;

        border-radius: 10px;

        overflow: hidden;

        background: #fff;

        margin-bottom: 0.75rem;

      }

      .gamma-preview__bar {

        position: absolute;

        top: 0;

        left: 0;

        right: 0;

        z-index: 1;

        padding: 0.4rem 0.75rem;

        font-size: 0.68rem;

        font-weight: 700;

        text-transform: uppercase;

        letter-spacing: 0.04em;

        color: #64748b;

        background: #f1f5f9;

        border-bottom: 1px solid #e2e8f0;

      }

      .gamma-preview__frame {

        width: 100%;

        height: 100%;

        border: 0;

        padding-top: 28px;

        box-sizing: border-box;

        background: #f8fafc;

      }

      .gamma-preview__loading {

        display: flex;

        align-items: center;

        justify-content: center;

        height: 100%;

        padding-top: 28px;

        font-size: 0.8rem;

        color: #64748b;

      }

      .gamma-ready__actions {

        display: flex;

        flex-wrap: wrap;

        gap: 0.5rem;

        align-items: center;

      }

      .success {

        margin-top: 0.65rem;

        font-size: 0.8rem;

      }

      .error {

        margin-top: 0.5rem;

        color: #dc2626;

        font-size: 0.8rem;

      }

      audio {

        width: 100%;

        margin-top: 0.35rem;

      }

      .locked {

        margin: 0;

        padding: 0.65rem 0.75rem;

        border-radius: 8px;

        background: #fff7ed;

        border: 1px solid #fed7aa;

        color: #9a3412;

        font-size: 0.8rem;

      }

      .locked a {

        color: #145bff;

        font-weight: 700;

      }

    `,

  ],

})

export class AiContentPanelComponent implements OnInit, OnDestroy {

  private readonly gammaService = inject(GammaService);

  private readonly http = inject(HttpClient);

  private readonly sanitizer = inject(DomSanitizer);

  private readonly podcastAiService = inject(PodcastAiService);

  private readonly producerPlansService = inject(ProducerPlansService);



  readonly courseId = input<number | null>(null);

  readonly courseFormat = input<string>('course');

  readonly activeLessonId = input<number | null>(null);



  readonly attachGammaPdf = output<string>();



  readonly error = signal<string | null>(null);

  readonly gammaLoading = signal(false);

  readonly podcastLoading = signal(false);

  readonly platformPdfUrl = signal<string | null>(null);

  readonly previewBlobUrl = signal<SafeResourceUrl | null>(null);

  readonly previewLoading = signal(false);

  readonly gammaDevMock = signal(false);

  readonly gammaStatusText = signal('');

  readonly podcastAudioUrl = signal<string | null>(null);

  readonly podcastJobId = signal<number | null>(null);

  readonly hasGammaAccess = signal(false);

  readonly hasPodcastAccess = signal(false);

  readonly gammaUsesRemaining = signal(0);

  readonly podcastUsesRemaining = signal(0);



  gammaPrompt = '';

  podcastTopic = '';

  podcastText = '';



  ngOnInit(): void {

    this.producerPlansService.capabilities().subscribe({

      next: (caps) => {

        this.hasGammaAccess.set(caps['gamma_ebook']?.active === true);

        this.hasPodcastAccess.set(caps['podcast_ai']?.active === true);

        this.gammaUsesRemaining.set(caps['gamma_ebook']?.usesRemaining ?? 0);

        this.podcastUsesRemaining.set(caps['podcast_ai']?.usesRemaining ?? 0);

      },

      error: () => {

        this.hasGammaAccess.set(false);

        this.hasPodcastAccess.set(false);

      },

    });

  }



  ngOnDestroy(): void {

    this.revokePreviewBlob();

  }



  gammaDownloadUrl(): string {

    return absoluteMediaUrl(this.platformPdfUrl());

  }

  private previewObjectUrl: string | null = null;

  private loadPreviewBlob(pdfUrl: string): void {

    this.revokePreviewBlob();

    const fetchUrl = mediaPreviewFetchUrl(pdfUrl);

    if (!fetchUrl) {

      this.error.set('URL del PDF no válida.');

      return;

    }

    this.previewLoading.set(true);

    this.http.get(fetchUrl, { responseType: 'blob' }).subscribe({

      next: (blob) => {

        this.previewLoading.set(false);

        if (blob.size === 0) {

          this.error.set('El PDF está vacío. Comprueba que el backend esté en marcha (puerto 8080).');

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

        this.error.set(

          'No se pudo cargar la vista previa. Comprueba que el backend esté en marcha (puerto 8080).',

        );

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



  startGamma(): void {

    const courseId = this.courseId();

    if (!courseId) {

      this.error.set('Curso no cargado. Recarga la página.');

      return;

    }

    const prompt = this.gammaPrompt.trim();

    if (!prompt) {

      this.error.set('Escribe un tema para el ebook.');

      return;

    }

    this.error.set(null);

    this.platformPdfUrl.set(null);

    this.gammaLoading.set(true);

    this.gammaStatusText.set('Iniciando generación…');

    this.gammaService

      .start({ prompt, pages: 10, format: 'document', courseId })

      .subscribe({

        next: (res) => {

          const id = res.id;

          if (!id) {

            this.error.set('Gamma no devolvió un id de generación.');

            this.gammaLoading.set(false);

            return;

          }

          const pdfUrl = this.pickPdfUrl(res);

          if (pdfUrl) {

            this.finishGamma(pdfUrl, this.isDevMockResponse(res));

            return;

          }

          this.gammaStatusText.set('Generando contenido (puede tardar varios minutos)…');

          this.pollGamma(id, 0);

        },

        error: (err) => {

          this.error.set(messageFromHttpError(err, 'No se pudo iniciar Gamma.'));

          this.gammaLoading.set(false);

          this.gammaStatusText.set('');

        },

      });

  }



  private pollGamma(generationId: string, attempt: number): void {

    this.gammaService.status(generationId).subscribe({

      next: (res) => {

        const status = (res.status ?? '').toLowerCase();

        const pdfUrl = this.pickPdfUrl(res);

        if (pdfUrl) {

          this.finishGamma(pdfUrl, this.isDevMockResponse(res));

          return;

        }

        if (status === 'failed' || status === 'error') {

          this.error.set('La generación en Gamma falló. Intenta de nuevo.');

          this.gammaLoading.set(false);

          this.gammaStatusText.set('');

          return;

        }

        if (attempt >= 45) {

          this.error.set('La generación del ebook no terminó a tiempo.');

          this.gammaLoading.set(false);

          this.gammaStatusText.set('');

          return;

        }

        setTimeout(() => this.pollGamma(generationId, attempt + 1), 5000);

      },

      error: (err) => {

        this.error.set(messageFromHttpError(err, 'Error consultando Gamma.'));

        this.gammaLoading.set(false);

        this.gammaStatusText.set('');

      },

    });

  }



  private pickPdfUrl(res: {
    platformExportUrl?: string | null;
    exportUrl?: string | null;
    status?: string | null;
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



  private finishGamma(platformUrl: string, devMock = false): void {

    this.platformPdfUrl.set(platformUrl);

    this.gammaDevMock.set(devMock);

    this.gammaLoading.set(false);

    this.gammaStatusText.set('');

    this.error.set(null);

    this.loadPreviewBlob(platformUrl);

  }



  resetGamma(): void {

    this.revokePreviewBlob();

    this.platformPdfUrl.set(null);

    this.gammaDevMock.set(false);

    this.gammaPrompt = '';

    this.error.set(null);

  }



  attachGammaToLesson(): void {

    const url = this.platformPdfUrl();

    if (!url) {

      return;

    }

    this.attachGammaPdf.emit(url);

  }



  startPodcast(): void {

    const courseId = this.courseId();

    if (!courseId) {

      this.error.set('Curso no cargado.');

      return;

    }

    const topic = this.podcastTopic.trim();

    const text = this.podcastText.trim();

    if (!topic && !text) {

      this.error.set('Indica un tema o texto fuente.');

      return;

    }

    this.error.set(null);

    this.podcastLoading.set(true);

    this.podcastAiService

      .generate({

        courseId,

        lessonId: this.activeLessonId() ?? undefined,

        topic: topic || undefined,

        text: text || undefined,

      })

      .subscribe({

        next: (job) => {

          this.podcastJobId.set(job.jobId);

          this.pollPodcast(job.jobId, 0);

        },

        error: (err) => {

          this.error.set(messageFromHttpError(err, 'No se pudo iniciar el podcast.'));

          this.podcastLoading.set(false);

        },

      });

  }



  attachPodcast(): void {

    const jobId = this.podcastJobId();

    const lessonId = this.activeLessonId();

    if (!jobId || !lessonId) {

      return;

    }

    this.podcastAiService.attach(jobId, lessonId).subscribe({

      next: () => {

        this.error.set(null);

      },

      error: (err) => {

        this.error.set(messageFromHttpError(err, 'No se pudo adjuntar el audio.'));

      },

    });

  }



  private pollPodcast(jobId: number, attempt: number): void {

    this.podcastAiService.jobStatus(jobId).subscribe({

      next: (job) => {

        const status = (job.status ?? '').toLowerCase();

        if (status === 'completed' && job.audioUrl) {

          this.podcastAudioUrl.set(job.audioUrl);

          this.podcastLoading.set(false);

          return;

        }

        if (status === 'failed') {

          this.error.set(job.errorMessage || 'Falló la generación del podcast.');

          this.podcastLoading.set(false);

          return;

        }

        if (attempt >= 45) {

          this.error.set('La generación del podcast tardó demasiado.');

          this.podcastLoading.set(false);

          return;

        }

        setTimeout(() => this.pollPodcast(jobId, attempt + 1), 3000);

      },

      error: (err) => {

        this.error.set(messageFromHttpError(err, 'Error consultando el job de podcast.'));

        this.podcastLoading.set(false);

      },

    });

  }

}


