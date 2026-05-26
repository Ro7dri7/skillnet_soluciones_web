import { Component, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-manage-coupon-modal',
  standalone: true,
  imports: [FormsModule],
  template: `
    @if (open()) {
      <div
        class="fixed inset-0 z-[1100] flex items-center justify-center bg-black/50 p-4"
        (click)="close.emit()"
      >
        <div
          class="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-6 shadow-2xl"
          (click)="$event.stopPropagation()"
        >
          <h3 class="text-lg font-bold text-skillnet-dark">Crear cupón</h3>
          <p class="mt-1 text-sm text-skillnet-muted">Define un código y el porcentaje de descuento.</p>

          <div class="mt-6 space-y-4">
            <div class="space-y-2">
              <label class="text-xs font-bold uppercase tracking-wide text-gray-600">Código</label>
              <div class="flex gap-2">
                <input
                  type="text"
                  class="flex-1 rounded-xl border border-gray-300 px-4 py-2.5 text-sm font-bold uppercase text-gray-900 focus:border-skillnet-accent focus:outline-none focus:ring-2 focus:ring-skillnet-accent/20"
                  [ngModel]="code()"
                  (ngModelChange)="code.set($event.toUpperCase())"
                  placeholder="EJ: VERANO20"
                />
                <button
                  type="button"
                  class="rounded-xl border border-gray-200 px-3 text-xs font-bold text-gray-600 hover:bg-gray-50"
                  (click)="generateCode()"
                >
                  Generar
                </button>
              </div>
            </div>

            <div class="space-y-2">
              <label class="text-xs font-bold uppercase tracking-wide text-gray-600"
                >Descuento (%)</label
              >
              <input
                type="number"
                min="1"
                max="100"
                class="w-full rounded-xl border border-gray-300 px-4 py-2.5 text-sm font-bold text-gray-900 focus:border-skillnet-accent focus:outline-none focus:ring-2 focus:ring-skillnet-accent/20"
                [ngModel]="percentOff()"
                (ngModelChange)="percentOff.set(+$event)"
              />
            </div>
          </div>

          <div class="mt-8 flex justify-end gap-3">
            <button
              type="button"
              class="rounded-xl px-5 py-2.5 text-sm font-semibold text-gray-600 hover:bg-gray-100"
              (click)="close.emit()"
            >
              Cancelar
            </button>
            <button
              type="button"
              class="rounded-xl bg-skillnet-dark px-5 py-2.5 text-sm font-semibold text-white hover:bg-skillnet-dark/90 disabled:opacity-50"
              [disabled]="saving() || !code().trim()"
              (click)="submit()"
            >
              {{ saving() ? 'Guardando…' : 'Crear cupón' }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class ManageCouponModalComponent {
  readonly open = input(false);
  readonly saving = input(false);

  readonly save = output<{ code: string; percentOff: number }>();
  readonly close = output<void>();

  readonly code = signal('');
  readonly percentOff = signal(10);

  generateCode(): void {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let result = '';
    for (let i = 0; i < 8; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    this.code.set(result);
  }

  submit(): void {
    const trimmed = this.code().trim();
    if (!trimmed) {
      return;
    }
    this.save.emit({ code: trimmed, percentOff: this.percentOff() });
  }
}
