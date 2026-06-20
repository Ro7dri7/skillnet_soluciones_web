import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  AdminEnrollment,
  AdminService,
} from '../../../../core/services/admin.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

@Component({
  selector: 'app-admin-enrollments',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './admin-enrollments.component.html',
  styleUrl: './admin-enrollments.component.scss',
})
export class AdminEnrollmentsComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly enrollments = signal<AdminEnrollment[]>([]);

  readonly form = this.fb.nonNullable.group({
    userId: [null as number | null, Validators.required],
    courseId: [null as number | null, Validators.required],
    enrollmentType: ['MANUAL'],
  });

  ngOnInit(): void {
    this.loadEnrollments();
  }

  loadEnrollments(): void {
    this.adminService.getEnrollments().subscribe({
      next: (rows) => {
        this.enrollments.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudieron cargar las inscripciones.'));
        this.loading.set(false);
      },
    });
  }

  createEnrollment(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { userId, courseId, enrollmentType } = this.form.getRawValue();
    if (userId == null || courseId == null) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.adminService
      .createEnrollment({ userId, courseId, enrollmentType: enrollmentType || 'MANUAL' })
      .subscribe({
        next: (created) => {
          this.enrollments.update((list) => [created, ...list]);
          this.form.reset({ userId: null, courseId: null, enrollmentType: 'MANUAL' });
          this.success.set('Inscripción creada correctamente.');
          this.saving.set(false);
        },
        error: (err) => {
          this.error.set(messageFromHttpError(err, 'No se pudo crear la inscripción.'));
          this.saving.set(false);
        },
      });
  }
}
