import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  TwoFactorService,
  type TwoFactorEnableResponse,
} from '../../../../core/services/two-factor.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

type SetupStep = 'idle' | 'qr' | 'disabling';

@Component({
  selector: 'app-profile-two-factor',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './profile-two-factor.component.html',
})
export class ProfileTwoFactorComponent implements OnInit {
  private readonly twoFactor = inject(TwoFactorService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly enabled = signal(false);
  readonly method = signal('app');
  readonly passwordRequiredForDisable = signal(true);
  readonly setupStep = signal<SetupStep>('idle');
  readonly setupData = signal<TwoFactorEnableResponse | null>(null);

  readonly verifyForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  readonly disableForm = this.fb.nonNullable.group({
    password: [''],
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  ngOnInit(): void {
    this.loadStatus();
  }

  loadStatus(): void {
    this.loading.set(true);
    this.error.set(null);
    this.twoFactor.status().subscribe({
      next: (status) => {
        this.enabled.set(status.enabled);
        this.method.set(status.method ?? 'app');
        this.passwordRequiredForDisable.set(status.passwordRequiredForDisable !== false);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo cargar el estado 2FA.'));
        this.loading.set(false);
      },
    });
  }

  startSetup(): void {
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    this.twoFactor.enable().subscribe({
      next: (data) => {
        this.setupData.set(data);
        this.setupStep.set('qr');
        this.verifyForm.reset();
        this.saving.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo iniciar la configuración 2FA.'));
        this.saving.set(false);
      },
    });
  }

  confirmSetup(): void {
    if (this.verifyForm.invalid) {
      this.verifyForm.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.twoFactor.verifySetup(this.verifyForm.controls.code.value).subscribe({
      next: () => {
        this.success.set('Autenticación de dos factores activada.');
        this.setupStep.set('idle');
        this.setupData.set(null);
        this.verifyForm.reset();
        this.enabled.set(true);
        this.method.set('app');
        this.saving.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'Código inválido. Revisa tu app autenticadora.'));
        this.saving.set(false);
      },
    });
  }

  cancelSetup(): void {
    this.setupStep.set('idle');
    this.setupData.set(null);
    this.verifyForm.reset();
    this.error.set(null);
  }

  showDisableForm(): void {
    this.setupStep.set('disabling');
    this.disableForm.reset();
    this.error.set(null);
  }

  cancelDisable(): void {
    this.setupStep.set('idle');
    this.disableForm.reset();
    this.error.set(null);
  }

  confirmDisable(): void {
    if (this.passwordRequiredForDisable() && !this.disableForm.controls.password.value.trim()) {
      this.error.set('Introduce tu contraseña actual.');
      return;
    }
    if (this.disableForm.controls.code.invalid) {
      this.disableForm.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    const { password, code } = this.disableForm.getRawValue();
    this.twoFactor.disable({
      password: password.trim(),
      code,
    }).subscribe({
      next: () => {
        this.success.set('Autenticación de dos factores desactivada.');
        this.setupStep.set('idle');
        this.disableForm.reset();
        this.enabled.set(false);
        this.saving.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo desactivar 2FA.'));
        this.saving.set(false);
      },
    });
  }

  methodLabel(): string {
    const m = this.method();
    if (m === 'app') return 'App autenticadora (Google Authenticator / Authy)';
    return m.toUpperCase();
  }
}
