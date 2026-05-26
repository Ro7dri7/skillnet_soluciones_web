import { inject, Injectable, signal } from '@angular/core';
import { ToastService } from './toast.service';
import { messageFromHttpError } from '../../shared/utils/http-error.util';
import type { ActionFeedbackPhase } from '../../shared/components/action-feedback-button/action-feedback-button.component';

@Injectable({ providedIn: 'root' })
export class ManageLayoutSaveService {
  private saveHandler: (() => Promise<void>) | null = null;
  private resetTimer: ReturnType<typeof setTimeout> | null = null;

  private readonly toast = inject(ToastService);

  readonly canSave = signal(false);
  readonly savePhase = signal<ActionFeedbackPhase>('idle');

  registerSaveHandler(handler: () => Promise<void>): void {
    this.saveHandler = handler;
    this.canSave.set(true);
  }

  unregisterSaveHandler(): void {
    this.saveHandler = null;
    this.canSave.set(false);
    this.savePhase.set('idle');
  }

  async triggerSave(): Promise<void> {
    if (!this.saveHandler || this.savePhase() === 'saving') {
      if (!this.saveHandler) {
        this.toast.info('No hay cambios pendientes en esta sección.');
      }
      return;
    }

    this.savePhase.set('saving');
    try {
      await this.saveHandler();
      this.savePhase.set('success');
      this.toast.success('Cambios guardados');
      this.scheduleReset();
    } catch (err) {
      this.savePhase.set('idle');
      if (err instanceof Error && err.message === 'Formulario inválido') {
        return;
      }
      this.toast.error(messageFromHttpError(err, 'No se pudieron guardar los cambios.'));
    }
  }

  private scheduleReset(): void {
    if (this.resetTimer) {
      clearTimeout(this.resetTimer);
    }
    this.resetTimer = setTimeout(() => this.savePhase.set('idle'), 2000);
  }
}
