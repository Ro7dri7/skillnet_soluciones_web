import { Component, inject } from '@angular/core';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast-host',
  standalone: true,
  template: `
    <div
      class="pointer-events-none fixed bottom-4 right-4 z-[9999] flex max-w-sm flex-col gap-2"
      aria-live="polite"
    >
      @for (toast of toastService.toasts(); track toast.id) {
        <div
          class="pointer-events-auto rounded-lg border px-4 py-3 text-sm shadow-lg"
          [class]="toastClass(toast.type)"
          role="status"
        >
          <div class="flex items-start justify-between gap-3">
            <span>{{ toast.message }}</span>
            <button
              type="button"
              class="shrink-0 text-xs font-semibold opacity-70 hover:opacity-100"
              (click)="toastService.dismiss(toast.id)"
            >
              Cerrar
            </button>
          </div>
        </div>
      }
    </div>
  `,
})
export class ToastHostComponent {
  readonly toastService = inject(ToastService);

  toastClass(type: 'success' | 'error' | 'info'): string {
    switch (type) {
      case 'success':
        return 'border-emerald-200 bg-emerald-50 text-emerald-900';
      case 'error':
        return 'border-red-200 bg-red-50 text-red-900';
      default:
        return 'border-slate-200 bg-white text-slate-900';
    }
  }
}
