import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthNavbarComponent } from '../../../../core/layout/auth-navbar/auth-navbar.component';
import { AuthService, PENDING_2FA_TOKEN_KEY } from '../../../../core/services/auth.service';
import { TwoFactorService } from '../../../../core/services/two-factor.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';
import { returnUrlFromQuery } from '../../../../shared/utils/return-url.util';

@Component({
  selector: 'app-login-2fa',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, AuthNavbarComponent],
  templateUrl: './login-2fa.component.html',
})
export class Login2faComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly twoFactor = inject(TwoFactorService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly loading = signal(false);
  readonly error = signal('');

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  submit(): void {
    const token = sessionStorage.getItem(PENDING_2FA_TOKEN_KEY);
    if (!token) {
      void this.router.navigate(['/login']);
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.twoFactor.verifyLogin({ twoFactorToken: token, code: this.form.controls.code.value }).subscribe({
      next: (response) => {
        sessionStorage.removeItem(PENDING_2FA_TOKEN_KEY);
        this.authService.completeTwoFactorLogin(response);
        const returnUrl = returnUrlFromQuery(this.route.snapshot.queryParams);
        void this.router.navigateByUrl(returnUrl ?? this.authService.dashboardPathForCurrentUser());
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'Código inválido o sesión expirada.'));
        this.loading.set(false);
      },
      complete: () => this.loading.set(false),
    });
  }
}
