import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  template: `
    @if (open()) {
      <div
        class="confirm-dialog-backdrop"
        role="presentation"
        (click)="onBackdropClick($event)"
      >
        <div class="confirm-dialog" role="dialog" aria-modal="true" [attr.aria-labelledby]="dialogId">
          <header class="confirm-dialog__header">
            <span class="confirm-dialog__brand">Skillnet</span>
          </header>
          <div class="confirm-dialog__body">
            <h2 [id]="dialogId" class="confirm-dialog__title">{{ title() }}</h2>
            @if (message()) {
              <p class="confirm-dialog__message">{{ message() }}</p>
            }
            <div class="confirm-dialog__actions">
              <button type="button" class="confirm-dialog__btn confirm-dialog__btn--cancel" (click)="cancelled.emit()">
                {{ cancelLabel() }}
              </button>
              <button type="button" class="confirm-dialog__btn confirm-dialog__btn--confirm" (click)="confirmed.emit()">
                {{ confirmLabel() }}
              </button>
            </div>
          </div>
        </div>
      </div>
    }
  `,
  styles: [
    `
      .confirm-dialog-backdrop {
        position: fixed;
        inset: 0;
        z-index: 20000;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 20px;
        background: rgba(0, 0, 0, 0.4);
        backdrop-filter: blur(4px);
      }

      .confirm-dialog {
        width: 100%;
        max-width: 400px;
        overflow: hidden;
        border: 1px solid #000;
        border-radius: 20px;
        background: #fff;
        box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
        animation: confirm-dialog-slide-up 0.3s cubic-bezier(0.16, 1, 0.3, 1);
      }

      .confirm-dialog__header {
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 12px 20px;
        border-bottom: 1.5px solid #89ceff;
        background: linear-gradient(90deg, #121d31 0%, #1e293b 100%);
      }

      .confirm-dialog__brand {
        font-size: 13px;
        font-weight: 800;
        letter-spacing: 0.12em;
        text-transform: uppercase;
        color: #fff;
      }

      .confirm-dialog__body {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 30px 24px;
        text-align: center;
      }

      .confirm-dialog__title {
        margin: 0 0 10px;
        font-size: 20px;
        font-weight: 700;
        line-height: 1.2;
        color: #000;
      }

      .confirm-dialog__message {
        margin: 0 0 28px;
        font-size: 14.5px;
        font-weight: 400;
        line-height: 1.5;
        color: #4b5563;
      }

      .confirm-dialog__actions {
        display: flex;
        gap: 12px;
        width: 100%;
        justify-content: center;
      }

      .confirm-dialog__btn {
        padding: 12px 0;
        border: 1.5px solid #000;
        border-radius: 12px;
        font-size: 14px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
        box-shadow: 0 4px 0 #000;
        transform: translateY(-2px);
      }

      .confirm-dialog__btn:active {
        transform: translateY(0);
        box-shadow: none;
      }

      .confirm-dialog__btn--cancel {
        flex: 1;
        background: #fff;
        color: #000;
      }

      .confirm-dialog__btn--confirm {
        flex: 2;
        background: #89ceff;
        color: #000;
      }

      @keyframes confirm-dialog-slide-up {
        from {
          opacity: 0;
          transform: translateY(20px) scale(0.95);
        }
        to {
          opacity: 1;
          transform: translateY(0) scale(1);
        }
      }
    `,
  ],
})
export class ConfirmDialogComponent {
  readonly dialogId = `confirm-dialog-${Math.random().toString(36).slice(2, 9)}`;

  readonly open = input(false);
  readonly title = input('');
  readonly message = input('');
  readonly confirmLabel = input('Sí');
  readonly cancelLabel = input('No');
  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cancelled.emit();
    }
  }
}
