import { Component, input, output } from '@angular/core';

export type ActionFeedbackPhase = 'idle' | 'saving' | 'success';

@Component({
  selector: 'app-action-feedback-button',
  standalone: true,
  template: `
    <button
      type="button"
      [class]="extraClass()"
      [disabled]="disabled() || phase() === 'saving'"
      (click)="pressed.emit()"
    >
      @if (phase() === 'saving') {
        <span class="inline-flex items-center justify-center gap-2">
          <span class="action-feedback-spinner" aria-hidden="true"></span>
          <span>{{ savingLabel() }}</span>
        </span>
      } @else if (phase() === 'success') {
        <span class="inline-flex items-center justify-center gap-1.5">
          <i class="ri-check-line text-base" aria-hidden="true"></i>
          <span>{{ successLabel() }}</span>
        </span>
      } @else {
        {{ label() }}
      }
    </button>
  `,
  styles: [
    `
      .action-feedback-spinner {
        width: 14px;
        height: 14px;
        border: 2px solid currentColor;
        border-top-color: transparent;
        border-radius: 50%;
        animation: action-feedback-spin 0.65s linear infinite;
        flex-shrink: 0;
      }

      @keyframes action-feedback-spin {
        to {
          transform: rotate(360deg);
        }
      }
    `,
  ],
})
export class ActionFeedbackButtonComponent {
  readonly label = input.required<string>();
  readonly savingLabel = input('Guardando…');
  readonly successLabel = input('Guardado');
  readonly phase = input<ActionFeedbackPhase>('idle');
  readonly disabled = input(false);
  readonly extraClass = input('');
  readonly pressed = output<void>();
}
