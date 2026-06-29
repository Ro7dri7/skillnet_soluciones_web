import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  GoogleSigninButtonDirective,
  SocialAuthService,
  SocialUser,
} from '@abacritt/angularx-social-login';
import { Subscription, filter } from 'rxjs';
import { AuthNavbarComponent } from '../../../../core/layout/auth-navbar/auth-navbar.component';
import { AuthService } from '../../../../core/services/auth.service';
import { AuthResponse } from '../../../../shared/models/auth.model';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';
import { returnUrlFromQuery } from '../../../../shared/utils/return-url.util';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    GoogleSigninButtonDirective,
    AuthNavbarComponent,
  ],
  templateUrl: './login.component.html',
})
export class LoginComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly socialAuthService = inject(SocialAuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private authStateSub?: Subscription;
  private googleLoginInProgress = false;

  readonly isLoading = signal(false);
  readonly error = signal('');
  readonly showPassword = signal(false);
  readonly isMobile = signal(typeof window !== 'undefined' && window.innerWidth <= 900);
  readonly googleSignInEnabled =
    environment.googleSignInEnabled && !!environment.googleClientId;

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.navigateAfterAuth();
      return;
    }

    if (typeof window !== 'undefined') {
      const onResize = () => this.isMobile.set(window.innerWidth <= 900);
      window.addEventListener('resize', onResize);
    }

    if (this.googleSignInEnabled) {
      void this.socialAuthService.signOut(true).catch(() => undefined);
      this.authStateSub = this.socialAuthService.authState
        .pipe(filter((user): user is SocialUser => !!user?.idToken))
        .subscribe((user) => this.handleGoogleAuth(user));
    }
  }

  ngOnDestroy(): void {
    this.authStateSub?.unsubscribe();
  }

  togglePassword(): void {
    this.showPassword.update((v) => !v);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.error.set('');

    this.authService.login(this.form.getRawValue()).subscribe({
      next: (response) => this.handleAuthResponse(response),
      error: (err) => {
        this.error.set(
          messageFromHttpError(err, 'Error al iniciar sesión. Intenta nuevamente.'),
        );
        this.isLoading.set(false);
      },
    });
  }

  private handleGoogleAuth(user: SocialUser): void {
    if (this.googleLoginInProgress || !user.idToken || this.authService.isLoggedIn()) {
      return;
    }

    this.googleLoginInProgress = true;
    this.isLoading.set(true);
    this.error.set('');

    this.authService.loginWithGoogle(user.idToken).subscribe({
      next: (response) => this.handleAuthResponse(response),
      error: (err) => {
        this.error.set(
          messageFromHttpError(err, 'Error al iniciar sesión con Google. Intenta nuevamente.'),
        );
        this.isLoading.set(false);
        this.googleLoginInProgress = false;
      },
    });
  }

  private handleAuthResponse(response: AuthResponse): void {
    if (this.authService.isVerificationPending(response)) {
      this.isLoading.set(false);
      this.googleLoginInProgress = false;
      this.authService.redirectToEmailVerification(
        response.user.email,
        returnUrlFromQuery(this.route.snapshot.queryParams),
      );
      return;
    }
    if (this.authService.isTwoFactorPending(response)) {
      this.isLoading.set(false);
      this.googleLoginInProgress = false;
      this.authService.redirectToTwoFactorLogin(
        response.twoFactorToken!,
        returnUrlFromQuery(this.route.snapshot.queryParams),
      );
      return;
    }
    this.navigateAfterAuth();
  }

  private navigateAfterAuth(): void {
    this.isLoading.set(false);
    this.googleLoginInProgress = false;
    const returnUrl = returnUrlFromQuery(this.route.snapshot.queryParams);
    const target = returnUrl ?? this.authService.dashboardPathForCurrentUser();
    void this.router.navigateByUrl(target);
  }
}
