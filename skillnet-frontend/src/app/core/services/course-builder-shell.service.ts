import { inject, Injectable, signal } from '@angular/core';
import { ToastService } from './toast.service';
import type { ActionFeedbackPhase } from '../../shared/components/action-feedback-button/action-feedback-button.component';

@Injectable({ providedIn: 'root' })
export class CourseBuilderShellService {
  private saveHandler: (() => Promise<void>) | null = null;
  private resetTimer: ReturnType<typeof setTimeout> | null = null;

  private readonly toast = inject(ToastService);

  readonly canSave = signal(false);
  readonly saving = signal(false);
  readonly savePhase = signal<ActionFeedbackPhase>('idle');
  readonly sectionsStatus = signal<Record<string, boolean>>({});

  registerSaveHandler(handler: () => Promise<void>): void {
    this.saveHandler = handler;
    this.canSave.set(true);
  }

  unregisterSaveHandler(): void {
    this.saveHandler = null;
    this.canSave.set(false);
  }

  setSectionStatus(name: string, status: boolean): void {
    this.sectionsStatus.update((prev) =>
      prev[name] === status ? prev : { ...prev, [name]: status },
    );
  }

  async triggerSave(): Promise<void> {
    if (!this.saveHandler || this.savePhase() === 'saving') {
      return;
    }
    this.saving.set(true);
    this.savePhase.set('saving');
    try {
      await this.saveHandler();
      this.savePhase.set('success');
      this.toast.success('Cambios guardados');
      this.scheduleReset();
    } catch (err) {
      this.savePhase.set('idle');
      if (!(err instanceof Error && err.message === 'Audience step incomplete')) {
        this.toast.error('No se pudieron guardar los cambios.');
      }
    } finally {
      this.saving.set(false);
    }
  }

  private scheduleReset(): void {
    if (this.resetTimer) {
      clearTimeout(this.resetTimer);
    }
    this.resetTimer = setTimeout(() => this.savePhase.set('idle'), 2000);
  }
}
