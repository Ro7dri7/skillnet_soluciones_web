import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthNavbarComponent } from '../../../../core/layout/auth-navbar/auth-navbar.component';
import { AuthService, PENDING_EMAIL_KEY } from '../../../../core/services/auth.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';
import { returnUrlFromQuery } from '../../../../shared/utils/return-url.util';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthNavbarComponent],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.scss',
})
export class VerifyEmailComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly loading = signal(false);
  readonly resending = signal(false);
  readonly error = signal('');
  readonly info = signal('');

  readonly form = this.fb.nonNullable.group({
    email: [
      this.route.snapshot.queryParamMap.get('email') ??
        sessionStorage.getItem(PENDING_EMAIL_KEY) ??
        '',
      [Validators.required, Validators.email],
    ],
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set('');
    const { email, code } = this.form.getRawValue();
    this.authService.verifyEmailCode(email, code).subscribe({
      next: () => {
        sessionStorage.removeItem(PENDING_EMAIL_KEY);
        const returnUrl = returnUrlFromQuery(this.route.snapshot.queryParams);
        void this.router.navigateByUrl(returnUrl ?? this.authService.dashboardPathForCurrentUser());
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'Código inválido o expirado.'));
        this.loading.set(false);
      },
      complete: () => this.loading.set(false),
    });
  }

  resend(): void {
    const email = this.form.controls.email.value.trim();
    if (!email) {
      this.error.set('Indica tu correo electrónico.');
      return;
    }
    this.resending.set(true);
    this.error.set('');
    this.info.set('');
    this.authService.resendVerification(email).subscribe({
      next: () => {
        sessionStorage.setItem(PENDING_EMAIL_KEY, email);
        this.info.set('Te enviamos un código nuevo. Revisa tu bandeja (y spam).');
        this.resending.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo reenviar el código.'));
        this.resending.set(false);
      },
    });
  }
}
