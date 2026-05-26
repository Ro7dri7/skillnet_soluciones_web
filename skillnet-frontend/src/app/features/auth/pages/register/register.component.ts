import { NgTemplateOutlet } from '@angular/common';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthNavbarComponent } from '../../../../core/layout/auth-navbar/auth-navbar.component';
import { AuthService } from '../../../../core/services/auth.service';
import {
  getPasswordRequirements,
  passwordMeetsRequirements,
} from '../../../../shared/utils/password-requirements';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

export type RegisterStep = 'select' | 'student' | 'infoproductor';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    NgTemplateOutlet,
    AuthNavbarComponent,
  ],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly formRef = viewChild<ElementRef<HTMLElement>>('formSection');

  readonly step = signal<RegisterStep>('select');
  readonly isLoading = signal(false);
  readonly error = signal('');
  readonly showPassword = signal(false);
  readonly showConfirmPassword = signal(false);
  readonly acceptTerms = signal(false);
  readonly isMobile = signal(typeof window !== 'undefined' && window.innerWidth <= 1024);

  readonly form = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
    confirmPassword: ['', Validators.required],
  });

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('resize', () => this.isMobile.set(window.innerWidth <= 1024));
    }
  }

  selectRole(type: 'student' | 'infoproductor'): void {
    this.step.set(type);
    this.error.set('');
  }

  backToSelect(): void {
    this.step.set('select');
    this.error.set('');
  }

  passwordRequirements(): ReturnType<typeof getPasswordRequirements> {
    return getPasswordRequirements(this.form.controls.password.value);
  }

  passwordValid(): boolean {
    return passwordMeetsRequirements(this.form.controls.password.value);
  }

  togglePassword(): void {
    this.showPassword.update((v) => !v);
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword.update((v) => !v);
  }

  scrollToForm(): void {
    this.formRef()?.nativeElement.scrollIntoView({ behavior: 'smooth' });
  }

  onSubmit(): void {
    const { firstName, lastName, email, password, confirmPassword } = this.form.getRawValue();

    if (!firstName || !lastName) {
      this.error.set('Nombre y apellido son requeridos.');
      return;
    }
    if (!email || !/\S+@\S+\.\S+/.test(email)) {
      this.error.set('Email inválido.');
      return;
    }
    if (!this.passwordValid()) {
      this.error.set('La contraseña no cumple las políticas de seguridad.');
      return;
    }
    if (password !== confirmPassword) {
      this.error.set('Las contraseñas no coinciden.');
      return;
    }
    if (!this.acceptTerms()) {
      this.error.set('Debes aceptar los términos y condiciones.');
      return;
    }

    const role = this.step() === 'infoproductor' ? 'infoproductor' : 'student';
    const username = this.generateUsername(email);

    this.isLoading.set(true);
    this.error.set('');

    this.authService
      .register({
        username,
        email,
        password,
        firstName,
        lastName,
        role,
        activeRole: role,
        active: true,
        student: true,
        infoproductor: true,
      })
      .subscribe({
        next: () => void this.router.navigateByUrl(this.authService.dashboardPathForCurrentUser()),
        error: (err) => {
          this.error.set(messageFromHttpError(err, 'Error al crear la cuenta. Intenta nuevamente.'));
          this.isLoading.set(false);
        },
      });
  }

  private generateUsername(email: string): string {
    const local = (email.split('@')[0] || 'user').replace(/[^a-zA-Z0-9_]/g, '_').slice(0, 24);
    const suffix = Math.floor(Math.random() * 10000);
    return `${local}_${suffix}`.slice(0, 150);
  }
}
