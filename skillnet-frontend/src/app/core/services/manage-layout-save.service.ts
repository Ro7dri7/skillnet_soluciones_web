import { computed, inject, Injectable, signal } from '@angular/core';
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
  readonly pendingChanges = signal<Record<string, unknown>>({});

  readonly showSaveButton = computed(
    () => this.canSave() || Object.keys(this.pendingChanges()).length > 0,
  );

  registerSaveHandler(handler: () => Promise<void>): void {
    this.saveHandler = handler;
    this.canSave.set(true);
  }

  unregisterSaveHandler(): void {
    this.saveHandler = null;
    this.canSave.set(false);
    this.savePhase.set('idle');
  }

  registerChange(data: Record<string, unknown>): void {
    this.pendingChanges.update((prev) => ({ ...prev, ...data }));
  }

  clearPendingChanges(): void {
    this.pendingChanges.set({});
  }

  async triggerSave(): Promise<boolean> {
    if (this.savePhase() === 'saving') {
      return false;
    }

    const hasHandler = Boolean(this.saveHandler);
    const hasPending = Object.keys(this.pendingChanges()).length > 0;

    if (!hasHandler && !hasPending) {
      this.toast.info('No hay cambios pendientes en esta sección.');
      return true;
    }

    this.savePhase.set('saving');
    try {
      if (hasHandler && this.saveHandler) {
        await this.saveHandler();
      }
      if (hasPending) {
        this.clearPendingChanges();
      }
      this.savePhase.set('success');
      this.toast.success('Cambios guardados');
      this.scheduleReset();
      return true;
    } catch (err) {
      this.savePhase.set('idle');
      if (err instanceof Error && err.message === 'Formulario inválido') {
        return false;
      }
      this.toast.error(messageFromHttpError(err, 'No se pudieron guardar los cambios.'));
      return false;
    }
  }

  private scheduleReset(): void {
    if (this.resetTimer) {
      clearTimeout(this.resetTimer);
    }
    this.resetTimer = setTimeout(() => this.savePhase.set('idle'), 2000);
  }
}
