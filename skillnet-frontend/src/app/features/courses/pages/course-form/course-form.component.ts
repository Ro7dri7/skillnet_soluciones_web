import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CourseService } from '../../../../core/services/course.service';
import { AuthService } from '../../../../core/services/auth.service';
import { CourseLevel, CourseStatus } from '../../../../shared/models/course.model';
import { slugifyCourseTitle } from '../../../../shared/utils/course-slug.util';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

@Component({
  selector: 'app-course-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './course-form.component.html',
})
export class CourseFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly courseService = inject(CourseService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly isLoading = signal(false);
  readonly error = signal('');
  readonly isEditMode = signal(false);
  private courseId: number | null = null;

  readonly levelOptions: { value: CourseLevel; label: string }[] = [
    { value: 'beginner', label: 'Principiante' },
    { value: 'intermediate', label: 'Intermedio' },
    { value: 'advanced', label: 'Avanzado' },
  ];

  readonly statusOptions: { value: CourseStatus; label: string }[] = [
    { value: 'draft', label: 'Borrador' },
    { value: 'published', label: 'Publicado' },
  ];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    slug: ['', [Validators.required, Validators.maxLength(300)]],
    description: ['', Validators.required],
    level: ['beginner' as CourseLevel, Validators.required],
    status: ['draft' as CourseStatus, Validators.required],
    price: [0, [Validators.required, Validators.min(0.01)]],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.courseId = Number(idParam);
      this.isEditMode.set(true);
      this.loadCourse(this.courseId);
    }

    this.form.controls.title.valueChanges.subscribe((title) => {
      const slugControl = this.form.controls.slug;
      if (!slugControl.dirty && title) {
        slugControl.setValue(this.slugify(title), { emitEvent: false });
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.error.set('');

    const payload = this.form.getRawValue();
    const professorId = this.authService.getCurrentUser()?.id;

    const request$ =
      this.isEditMode() && this.courseId != null
        ? this.courseService.updateCourse(this.courseId, payload, professorId)
        : this.courseService.createCourse(payload, professorId);

    request$.subscribe({
      next: () => void this.router.navigate(['/courses']),
      error: (err) => {
        this.error.set(
          messageFromHttpError(
            err,
            this.isEditMode()
              ? 'No se pudo actualizar el curso.'
              : 'No se pudo crear el curso.',
          ),
        );
        this.isLoading.set(false);
      },
    });
  }

  cancel(): void {
    void this.router.navigate(['/courses']);
  }

  private loadCourse(id: number): void {
    this.isLoading.set(true);
    this.courseService.getCourse(id).subscribe({
      next: (course) => {
        this.form.patchValue({
          title: course.title,
          slug: course.slug,
          description: course.description,
          level: course.level as CourseLevel,
          status: course.status as CourseStatus,
          price: course.price,
        });
        this.form.controls.slug.markAsDirty();
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo cargar el curso.'));
        this.isLoading.set(false);
      },
    });
  }

  private slugify(value: string): string {
    return slugifyCourseTitle(value);
  }
}
