import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { startWith } from 'rxjs';
import {
  ChangePasswordPayload,
  ProfileService,
  ProfileUser,
  UpdateProfilePayload,
} from '../../../../core/services/profile.service';
import { PaymentItem, PaymentService } from '../../../../core/services/payment.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ProfileTwoFactorComponent } from '../../components/profile-two-factor/profile-two-factor.component';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

type ProfileTab = 'info' | 'security' | 'purchases';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule, CurrencyPipe, DatePipe, ProfileTwoFactorComponent],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit {
  private readonly profileService = inject(ProfileService);
  private readonly paymentService = inject(PaymentService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly activeTab = signal<ProfileTab>('info');
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly payments = signal<PaymentItem[]>([]);
  readonly paymentsLoading = signal(false);
  readonly profile = signal<ProfileUser | null>(null);
  readonly bioTouched = signal(false);

  readonly isInfoproductor = computed(
    () => this.profile()?.infoproductor === true || this.authService.getCurrentUser()?.infoproductor === true,
  );

  readonly infoForm = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(30)]],
    lastName: ['', [Validators.required, Validators.maxLength(30)]],
    bio: [''],
    professionalTitle: ['', Validators.maxLength(200)],
    phone: ['', Validators.maxLength(20)],
    company: ['', Validators.maxLength(200)],
    location: ['', Validators.maxLength(200)],
    website: ['', Validators.maxLength(200)],
    linkedinUrl: ['', Validators.maxLength(200)],
    instagramUrl: ['', Validators.maxLength(200)],
    youtubeUrl: ['', Validators.maxLength(200)],
    postalCode: ['', Validators.maxLength(20)],
    address: ['', Validators.maxLength(255)],
  });

  readonly passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required],
  });

  private readonly formValues = toSignal(
    this.infoForm.valueChanges.pipe(startWith(this.infoForm.getRawValue())),
    { initialValue: this.infoForm.getRawValue() },
  );

  readonly hasInfoChanges = computed(() => {
    this.formValues();
    const baseline = this.profile();
    if (!baseline) {
      return false;
    }
    const current = this.infoForm.getRawValue();
    const fields: (keyof typeof current)[] = [
      'firstName',
      'lastName',
      'bio',
      'professionalTitle',
      'phone',
      'company',
      'location',
      'website',
      'linkedinUrl',
      'instagramUrl',
      'youtubeUrl',
      'postalCode',
      'address',
    ];
    return fields.some((key) => this.normalize(current[key]) !== this.normalize(baseline[key as keyof ProfileUser] as string));
  });

  ngOnInit(): void {
    this.loadProfile();
  }

  setTab(tab: ProfileTab): void {
    this.activeTab.set(tab);
    this.success.set(null);
    this.error.set(null);
    if (tab === 'purchases' && this.payments().length === 0 && !this.paymentsLoading()) {
      this.loadPayments();
    }
  }

  private loadProfile(): void {
    this.loading.set(true);
    this.error.set(null);
    this.profileService.getMe().subscribe({
      next: (user) => {
        this.applyProfileToForm(user);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo cargar el perfil.'));
        this.loading.set(false);
      },
    });
  }

  private applyProfileToForm(user: ProfileUser): void {
    this.profile.set(user);
    this.infoForm.patchValue({
      firstName: user.firstName ?? '',
      lastName: user.lastName ?? '',
      bio: user.bio ?? '',
      professionalTitle: user.professionalTitle ?? '',
      phone: user.phone ?? '',
      company: user.company ?? '',
      location: user.location ?? '',
      website: user.website ?? '',
      linkedinUrl: user.linkedinUrl ?? '',
      instagramUrl: user.instagramUrl ?? '',
      youtubeUrl: user.youtubeUrl ?? '',
      postalCode: user.postalCode ?? '',
      address: user.address ?? '',
    });
    this.infoForm.markAsPristine();
  }

  private loadPayments(): void {
    this.paymentsLoading.set(true);
    this.paymentService.myPayments().subscribe({
      next: (rows) => {
        this.payments.set(rows);
        this.paymentsLoading.set(false);
      },
      error: () => this.paymentsLoading.set(false),
    });
  }

  saveInfo(): void {
    this.bioTouched.set(true);
    if (this.isInfoproductor() && !this.infoForm.controls.bio.value.trim()) {
      this.error.set('La biografía es obligatoria para infoproductores.');
      return;
    }
    if (this.infoForm.invalid) {
      this.infoForm.markAllAsTouched();
      this.error.set('Revisa los campos marcados antes de guardar.');
      return;
    }
    if (!this.hasInfoChanges()) {
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);

    const raw = this.infoForm.getRawValue();
    const payload: UpdateProfilePayload = {
      firstName: raw.firstName.trim(),
      lastName: raw.lastName.trim(),
      bio: raw.bio.trim(),
      professionalTitle: raw.professionalTitle.trim(),
      phone: raw.phone.trim(),
      company: raw.company.trim(),
      location: raw.location.trim(),
      website: raw.website.trim(),
      linkedinUrl: raw.linkedinUrl.trim(),
      instagramUrl: raw.instagramUrl.trim(),
      youtubeUrl: raw.youtubeUrl.trim(),
      postalCode: raw.postalCode.trim(),
      address: raw.address.trim(),
    };

    this.profileService.updateMe(payload).subscribe({
      next: (updated) => {
        this.applyProfileToForm(updated);
        this.authService.mergeProfile(updated);
        this.success.set('Perfil actualizado correctamente.');
        this.saving.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo guardar el perfil.'));
        this.saving.set(false);
      },
    });
  }

  changePassword(): void {
    const { currentPassword, newPassword, confirmPassword } = this.passwordForm.getRawValue();
    if (newPassword !== confirmPassword) {
      this.error.set('Las contraseñas no coinciden.');
      return;
    }
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    const payload: ChangePasswordPayload = { currentPassword, newPassword };
    this.profileService.changePassword(payload).subscribe({
      next: () => {
        this.success.set('Contraseña actualizada.');
        this.passwordForm.reset();
        this.saving.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo cambiar la contraseña.'));
        this.saving.set(false);
      },
    });
  }

  avatarInitials(): string {
    const p = this.profile();
    const a = (p?.firstName?.[0] ?? '').toUpperCase();
    const b = (p?.lastName?.[0] ?? '').toUpperCase();
    return a + b || '?';
  }

  private normalize(value: string | null | undefined): string {
    return (value ?? '').trim();
  }
}
