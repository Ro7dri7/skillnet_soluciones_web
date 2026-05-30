import {
  Component,
  ElementRef,
  OnDestroy,
  effect,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { firstValueFrom } from 'rxjs';
import { mediaBackendUrl } from '../../../../shared/utils/media-url.util';

@Component({
  selector: 'app-pdf-block-viewer',
  standalone: true,
  templateUrl: './pdf-block-viewer.component.html',
  styleUrl: './pdf-block-viewer.component.scss',
})
export class PdfBlockViewerComponent implements OnDestroy {
  readonly resourceUrl = input.required<string>();
  readonly title = input('Documento PDF');

  private readonly http = inject(HttpClient);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly containerRef = viewChild<ElementRef<HTMLElement>>('containerRef');

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly downloadUrl = signal('');
  readonly safeBlobUrl = signal<SafeResourceUrl | null>(null);
  readonly zoom = signal(1.35);
  readonly isFullscreen = signal(false);

  readonly zoomPercent = signal(135);

  private objectUrl: string | null = null;
  private fullscreenListener: (() => void) | null = null;

  constructor() {
    effect(() => {
      const url = this.resourceUrl();
      void this.loadPdf(url);
    });

    this.fullscreenListener = () => {
      this.isFullscreen.set(Boolean(document.fullscreenElement));
    };
    document.addEventListener('fullscreenchange', this.fullscreenListener);
  }

  ngOnDestroy(): void {
    if (this.fullscreenListener) {
      document.removeEventListener('fullscreenchange', this.fullscreenListener);
    }
    this.revokeBlob();
  }

  zoomIn(): void {
    this.zoom.update((z) => Math.min(z + 0.15, 2.5));
    this.syncZoomPercent();
  }

  zoomOut(): void {
    this.zoom.update((z) => Math.max(z - 0.15, 0.75));
    this.syncZoomPercent();
  }

  toggleFullscreen(): void {
    const el = this.containerRef()?.nativeElement;
    if (!el) {
      return;
    }
    if (!document.fullscreenElement) {
      void el.requestFullscreen().catch(() => undefined);
    } else {
      void document.exitFullscreen();
    }
  }

  private syncZoomPercent(): void {
    this.zoomPercent.set(Math.round(this.zoom() * 100));
  }

  private async loadPdf(raw: string): Promise<void> {
    this.revokeBlob();
    this.loading.set(true);
    this.error.set(false);
    this.zoom.set(1.35);
    this.syncZoomPercent();

    const fetchUrl = mediaBackendUrl(raw);
    this.downloadUrl.set(fetchUrl);
    if (!fetchUrl) {
      this.loading.set(false);
      this.error.set(true);
      return;
    }

    try {
      const blob = await firstValueFrom(
        this.http.get(fetchUrl, { responseType: 'blob' }),
      );
      this.objectUrl = URL.createObjectURL(blob);
      this.safeBlobUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrl));
    } catch {
      this.error.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  private revokeBlob(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
    this.safeBlobUrl.set(null);
  }
}
