import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CourseBuilderService } from '../../../../core/services/course-builder.service';
import { CourseBuilderShellService } from '../../../../core/services/course-builder-shell.service';

@Component({
  selector: 'app-builder-audience-step',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './builder-audience-step.component.html',
})
export class BuilderAudienceStepComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly builder = inject(CourseBuilderService);
  private readonly shell = inject(CourseBuilderShellService);

  readonly goals = signal<string[]>(this.parseLines(this.builder.whatYouWillLearn(), 2));
  readonly audienceLines = signal<string[]>(this.parseLines(this.builder.targetAudience(), 1));
  readonly error = signal<string | null>(null);

  readonly productNoun = signal(this.resolveProductNoun());

  ngOnInit(): void {
    this.shell.registerSaveHandler(() => this.persistAndContinue());
    this.syncAudienceDraft();
  }

  ngOnDestroy(): void {
    this.shell.unregisterSaveHandler();
  }

  private resolveProductNoun(): string {
    const format = this.builder.productType() ?? 'course';
    if (format === 'ebook') return 'ebook';
    if (format === 'podcast') return 'podcast';
    return 'curso';
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
    this.syncAudienceDraft();
  }

  updateAudience(index: number, value: string): void {
    this.audienceLines.update((items) => {
      const next = [...items];
      next[index] = value;
      return next;
    });
    this.syncAudienceDraft();
  }

  addGoal(): void {
    this.goals.update((items) => [...items, '']);
    this.syncAudienceDraft();
  }

  removeGoal(index: number): void {
    if (this.goals().length <= 2) {
      return;
    }
    this.goals.update((items) => items.filter((_, i) => i !== index));
    this.syncAudienceDraft();
  }

  addAudienceLine(): void {
    this.audienceLines.update((items) => [...items, '']);
    this.syncAudienceDraft();
  }

  removeAudience(index: number): void {
    if (this.audienceLines().length <= 1) {
      return;
    }
    this.audienceLines.update((items) => items.filter((_, i) => i !== index));
    this.syncAudienceDraft();
  }

  canContinue(): boolean {
    return this.goals().some((g) => g.trim()) && this.audienceLines().some((a) => a.trim());
  }

  private syncAudienceDraft(): void {
    const goalsText = this.goals().filter((g) => g.trim()).join('\n');
    const audienceText = this.audienceLines().filter((a) => a.trim()).join('\n');
    this.builder.setWhatYouWillLearn(goalsText);
    this.builder.setTargetAudience(audienceText);
    this.shell.setSectionStatus('audience', this.canContinue());
  }

  private async persistAndContinue(): Promise<void> {
    if (!this.canContinue()) {
      this.error.set('Completa al menos una meta y un perfil de estudiante ideal.');
      throw new Error('Audience step incomplete');
    }

    const goalsText = this.goals().filter((g) => g.trim()).join('\n');
    const audienceText = this.audienceLines().filter((a) => a.trim()).join('\n');
    this.builder.setWhatYouWillLearn(goalsText);
    this.builder.setTargetAudience(audienceText);
    this.shell.setSectionStatus('audience', this.canContinue());

    this.error.set(null);
    try {
      const courseId = await this.builder.ensureCourseId();
      void this.router.navigate(['/instructor/courses', courseId, 'manage', 'curriculum']);
    } catch (error) {
      if (this.builder.isUnauthorizedError(error)) {
        this.error.set('Tu sesión expiró. Inicia sesión de nuevo para guardar el borrador.');
      } else {
        this.error.set('No se pudo guardar el borrador. Revisa la conexión con el servidor.');
      }
      throw error;
    }
  }
}
