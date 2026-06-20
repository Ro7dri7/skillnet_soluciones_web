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

import { FormsModule } from '@angular/forms';

import { RouterLink } from '@angular/router';

import { GammaService } from '../../../../core/services/gamma.service';

import { MediaService } from '../../../../core/services/media.service';

import { PodcastAiService } from '../../../../core/services/podcast-ai.service';

import { ProducerPlansService } from '../../../../core/services/producer-plans.service';

import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

import { OFFICIAL_LANGUAGES } from '../../../marketplace/data/categories.data';

import { absoluteMediaUrl } from '../../../../shared/utils/media-url.util';



type PanelMode = 'generate' | 'upload';

type PodcastPhase = 'idle' | 'script_generating' | 'script_ready' | 'audio_generating' | 'completed' | 'failed';



@Component({

  selector: 'app-lesson-podcast-panel',

  standalone: true,

  imports: [FormsModule, RouterLink],

  templateUrl: './lesson-podcast-panel.component.html',

  styleUrl: './lesson-podcast-panel.component.scss',

})

export class LessonPodcastPanelComponent implements OnDestroy, OnInit {

  private readonly podcastService = inject(PodcastAiService);

  private readonly gammaService = inject(GammaService);

  private readonly mediaService = inject(MediaService);

  private readonly producerPlansService = inject(ProducerPlansService);



  readonly courseId = input<number | null>(null);

  readonly lessonId = input<number | null>(null);

  readonly lessonTitle = input<string>('');

  readonly attachedAudioUrl = input<string | null>(null);



  readonly attachAudio = output<string>();

  readonly removeAttached = output<void>();



  readonly panelMode = signal<PanelMode>('generate');

  readonly phase = signal<PodcastPhase>('idle');

  readonly replaceRequested = signal(false);

  readonly hasPodcastAccess = signal(false);

  readonly podcastUsesRemaining = signal(0);

  readonly error = signal<string | null>(null);

  readonly referenceExtracting = signal(false);

  readonly referenceFileName = signal<string | null>(null);

  readonly uploadProgress = signal(0);

  readonly uploadingAudio = signal(false);

  readonly uploadingFileName = signal<string | null>(null);



  topic = '';

  sourceText = '';

  editedTranscript = '';

  language = 'es';

  durationMinutes = 2;

  readonly languages = OFFICIAL_LANGUAGES;

  private jobId: number | null = null;

  readonly platformAudioUrl = signal<string | null>(null);

  private pollTimer: ReturnType<typeof setTimeout> | null = null;

  private lastLessonId: number | null = null;



  readonly showAttachedView = computed(() => {

    const attached = this.attachedAudioUrl()?.trim();

    return Boolean(attached) && !this.replaceRequested();

  });



  readonly attachedPlaybackUrl = computed(() =>

    absoluteMediaUrl(this.attachedAudioUrl() ?? ''),

  );



  constructor() {

    effect(() => {

      const lessonId = this.lessonId();

      if (this.lastLessonId !== null && this.lastLessonId !== lessonId) {

        untracked(() => this.resetForNewLesson());

      }

      this.lastLessonId = lessonId;

    });

  }



  ngOnDestroy(): void {

    this.stopPolling();

  }



  setMode(mode: PanelMode): void {

    this.panelMode.set(mode);

    this.error.set(null);

  }



  requestReplace(): void {

    this.replaceRequested.set(true);

    this.resetForNewLesson();

  }



  detachFromLesson(): void {

    this.removeAttached.emit();

    this.requestReplace();

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

        this.sourceText = res.extractedText;

        this.referenceFileName.set(file.name);

        this.referenceExtracting.set(false);

      },

      error: (err) => {

        this.referenceExtracting.set(false);

        this.referenceFileName.set(null);

        this.error.set(messageFromHttpError(err, 'No se pudo leer el PDF de referencia.'));

      },

    });

  }



  onUploadOwnAudio(event: Event): void {

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

    this.uploadingAudio.set(true);

    this.uploadingFileName.set(file.name);

    this.uploadProgress.set(0);

    this.phase.set('audio_generating');

    this.error.set(null);

    this.mediaService.uploadLessonResourceWithProgress(courseId, file, 'audio').subscribe({

      next: ({ progress, response }) => {

        this.uploadProgress.set(progress);

        if (response?.url) {

          this.uploadingAudio.set(false);

          this.attachAudio.emit(response.url);

          this.phase.set('completed');

          this.replaceRequested.set(false);

          this.error.set(null);

        }

      },

      error: (err) => {

        this.uploadingAudio.set(false);

        this.uploadProgress.set(0);

        this.phase.set('failed');

        this.error.set(messageFromHttpError(err, 'No se pudo subir el audio.'));

      },

    });

  }



  startScriptGeneration(): void {

    const courseId = this.courseId();

    const lessonId = this.lessonId();

    if (!courseId || !lessonId) {

      this.error.set('Selecciona una lección guardada antes de generar.');

      return;

    }

    if (!this.topic.trim() && !this.sourceText.trim()) {

      this.error.set('Indica un tema o contenido fuente para el episodio.');

      return;

    }

    if (this.durationMinutes < 1 || this.durationMinutes > 10) {

      this.error.set('La duración debe estar entre 1 y 10 minutos.');

      return;

    }



    this.error.set(null);

    this.phase.set('script_generating');

    this.podcastService

      .generate({

        topic: this.topic.trim() || undefined,

        text: this.sourceText.trim() || undefined,

        courseId,

        lessonId,

        transcriptOnly: true,

        language: this.language,

        durationMinutes: this.durationMinutes,

      })

      .subscribe({

        next: (res) => {

          this.jobId = res.jobId;

          this.pollJob('script');

        },

        error: (err) => {

          this.phase.set('failed');

          this.error.set(messageFromHttpError(err, 'No se pudo iniciar la generación del guion.'));

        },

      });

  }



  startAudioGeneration(): void {

    if (!this.jobId) {

      this.error.set('Genera el guion primero.');

      return;

    }

    const script = this.editedTranscript.trim();

    if (!script) {

      this.error.set('Aprueba o edita el guion antes de generar el audio.');

      return;

    }



    this.error.set(null);

    this.phase.set('audio_generating');

    this.podcastService.synthesizeAudio(this.jobId, script).subscribe({

      next: (res) => {

        this.jobId = res.jobId;

        this.pollJob('audio');

      },

      error: (err) => {

        this.phase.set('script_ready');

        this.error.set(messageFromHttpError(err, 'No se pudo generar el audio.'));

      },

    });

  }



  assignToLesson(): void {

    const url = this.platformAudioUrl();

    const lessonId = this.lessonId();

    if (url && lessonId && this.jobId) {

      this.podcastService.attach(this.jobId, lessonId).subscribe({

        next: (res) => {

          const audioUrl = res.audioUrl ?? url;

          this.attachAudio.emit(audioUrl);

          this.replaceRequested.set(false);

          this.phase.set('completed');

        },

        error: () => {

          this.attachAudio.emit(url);

          this.replaceRequested.set(false);

          this.phase.set('completed');

        },

      });

      return;

    }

    if (url) {

      this.attachAudio.emit(url);

      this.replaceRequested.set(false);

    }

  }



  ngOnInit(): void {

    this.producerPlansService.capabilities().subscribe({

      next: (caps) => {

        this.hasPodcastAccess.set(caps['podcast_ai']?.active === true);

        this.podcastUsesRemaining.set(caps['podcast_ai']?.usesRemaining ?? 0);

      },

      error: () => this.hasPodcastAccess.set(false),

    });

  }



  languageLabel(code: string): string {

    return this.languages.find((item) => item.value === code)?.label ?? code;

  }



  private resetForNewLesson(): void {

    this.stopPolling();

    this.replaceRequested.set(false);

    this.phase.set('idle');

    this.topic = '';

    this.sourceText = '';

    this.editedTranscript = '';

    this.jobId = null;

    this.platformAudioUrl.set(null);

    this.referenceFileName.set(null);

    this.language = 'es';

    this.durationMinutes = 2;

    this.error.set(null);

  }



  private pollJob(mode: 'script' | 'audio', attempt = 0): void {

    if (!this.jobId) {

      return;

    }

    this.podcastService.jobStatus(this.jobId).subscribe({

      next: (res) => {

        const status = (res.status ?? '').toLowerCase();

        if (status === 'completed') {

          this.stopPolling();

          if (mode === 'script') {

            const script = (res.transcript ?? '').trim();

            if (!script) {

              this.phase.set('failed');

              this.error.set('El guion se generó vacío. Intenta con más contenido.');

              return;

            }

            this.editedTranscript = script;

            if (res.language) {

              this.language = res.language;

            }

            if (res.durationMinutes != null) {

              this.durationMinutes = res.durationMinutes;

            }

            this.phase.set('script_ready');

            return;

          }

          const audioUrl = res.audioUrl?.trim();

          if (!audioUrl) {

            this.phase.set('failed');

            this.error.set('No se recibió URL de audio.');

            return;

          }

          this.platformAudioUrl.set(audioUrl);

          this.phase.set('completed');

          return;

        }

        if (status === 'failed' || status === 'error') {

          this.stopPolling();

          this.phase.set('failed');

          this.error.set(res.errorMessage ?? 'La generación falló.');

          return;

        }

        if (attempt >= 90) {

          this.stopPolling();

          this.phase.set('failed');

          this.error.set('La generación tardó demasiado.');

          return;

        }

        this.pollTimer = setTimeout(() => this.pollJob(mode, attempt + 1), 4000);

      },

      error: (err) => {

        this.stopPolling();

        this.phase.set('failed');

        this.error.set(messageFromHttpError(err, 'Error consultando el estado.'));

      },

    });

  }



  private stopPolling(): void {

    if (this.pollTimer) {

      clearTimeout(this.pollTimer);

      this.pollTimer = null;

    }

  }



  playbackUrl(): string {

    return absoluteMediaUrl(this.platformAudioUrl() ?? '');

  }



  isGenerating(): boolean {

    const p = this.phase();

    return p === 'script_generating' || p === 'audio_generating';

  }

}


