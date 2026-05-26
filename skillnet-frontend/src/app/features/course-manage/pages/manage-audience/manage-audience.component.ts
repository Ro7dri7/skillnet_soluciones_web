import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { CourseBuilderService } from '../../../../core/services/course-builder.service';
import { CourseService } from '../../../../core/services/course.service';
import { ManageLayoutSaveService } from '../../../../core/services/manage-layout-save.service';
import { ProducerCoursesService } from '../../../../core/services/producer-courses.service';
import {
  isCourseOwnershipError,
  messageFromHttpError,
} from '../../../../shared/utils/http-error.util';

@Component({
  selector: 'app-manage-audience',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './manage-audience.component.html',
})
export class ManageAudienceComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly builder = inject(CourseBuilderService);
  private readonly courseService = inject(CourseService);
  private readonly producerCourses = inject(ProducerCoursesService);
  private readonly manageSave = inject(ManageLayoutSaveService);

  readonly goals = signal<string[]>(['', '']);
  readonly audienceLines = signal<string[]>(['']);
  readonly error = signal<string | null>(null);
  readonly loading = signal(true);
  readonly productNoun = signal('curso');

  private courseId: number | null = null;

  ngOnInit(): void {
    const idParam = this.route.parent?.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : NaN;
    if (Number.isNaN(id)) {
      this.loading.set(false);
      return;
    }
    this.courseId = id;
    this.manageSave.registerSaveHandler(() => this.persistAudience());
    void this.loadCourse(id);
  }

  ngOnDestroy(): void {
    this.manageSave.unregisterSaveHandler();
  }

  private async loadCourse(id: number): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      await this.builder.ensureInfoproductorSession();
      const owned = await firstValueFrom(this.producerCourses.getMyCourses());
      if (!owned.some((c) => c.id === id)) {
        this.error.set(
          'Este curso no pertenece a tu cuenta. Abre uno de tus borradores desde Mis cursos o crea uno nuevo.',
        );
        return;
      }
      const course = await firstValueFrom(this.courseService.getCourseWithAudience(id));
      this.goals.set(this.parseLines(course.whatYouWillLearn ?? '', 2));
      this.audienceLines.set(this.parseLines(course.targetAudience ?? '', 1));
    } catch (err) {
      this.error.set(messageFromHttpError(err, 'No se pudo cargar la audiencia del curso.'));
    } finally {
      this.loading.set(false);
    }
  }

  private parseLines(text: string, minLength = 1): string[] {
    const list = text ? text.split('\n').filter((line) => line.trim() !== '') : [];
    while (list.length < minLength) {
      list.push('');
    }
    return list;
  }

  updateGoal(index: number, value: string): void {
    this.goals.update((items) => {
      const next = [...items];
      next[index] = value.slice(0, 160);
      return next;
    });
  }

  updateAudience(index: number, value: string): void {
    this.audienceLines.update((items) => {
      const next = [...items];
      next[index] = value;
      return next;
    });
  }

  addGoal(): void {
    this.goals.update((items) => [...items, '']);
  }

  removeGoal(index: number): void {
    if (this.goals().length <= 2) {
      return;
    }
    this.goals.update((items) => items.filter((_, i) => i !== index));
  }

  addAudienceLine(): void {
    this.audienceLines.update((items) => [...items, '']);
  }

  removeAudience(index: number): void {
    if (this.audienceLines().length <= 1) {
      return;
    }
    this.audienceLines.update((items) => items.filter((_, i) => i !== index));
  }

  canContinue(): boolean {
    return this.goals().some((g) => g.trim()) && this.audienceLines().some((a) => a.trim());
  }

  private async persistAudience(): Promise<void> {
    if (!this.courseId) {
      return;
    }
    if (!this.canContinue()) {
      this.error.set('Completa al menos una meta y un perfil de estudiante ideal.');
      throw new Error('Formulario inválido');
    }

    const whatYouWillLearn = this.goals()
      .filter((g) => g.trim())
      .join('\n');
    const targetAudience = this.audienceLines()
      .filter((a) => a.trim())
      .join('\n');

    this.builder.setWhatYouWillLearn(whatYouWillLearn);
    this.builder.setTargetAudience(targetAudience);
    this.error.set(null);

    await this.builder.ensureInfoproductorSession();
    try {
      await firstValueFrom(
        this.producerCourses.updateBasics(this.courseId, {
          whatYouWillLearn,
          targetAudience,
        }),
      );
    } catch (err) {
      if (err instanceof HttpErrorResponse && (err.status === 403 || isCourseOwnershipError(err))) {
        this.error.set(
          'No puedes guardar este curso porque no pertenece a tu cuenta. Usa Mis cursos o crea un borrador nuevo.',
        );
      }
      throw err;
    }
  }
}
