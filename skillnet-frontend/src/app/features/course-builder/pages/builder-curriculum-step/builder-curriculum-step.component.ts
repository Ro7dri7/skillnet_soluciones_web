import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CourseBuilderService } from '../../../../core/services/course-builder.service';
import { CourseBuilderShellService } from '../../../../core/services/course-builder-shell.service';
import { CurriculumWorkspaceComponent } from '../../../course-manage/components/curriculum-workspace/curriculum-workspace.component';

@Component({
  selector: 'app-builder-curriculum-step',
  standalone: true,
  imports: [CurriculumWorkspaceComponent, RouterLink],
  template: `
    @if (bootstrapping()) {
      <div class="flex min-h-[400px] items-center justify-center p-8 text-sm text-skillnet-muted">
        Preparando el editor de temario…
      </div>
    } @else {
      @if (authError()) {
        <div class="border-b border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-950">
          {{ authError() }}
          <a routerLink="/login" class="ml-2 font-semibold underline">Iniciar sesión</a>
        </div>
      }
      <app-curriculum-workspace
        [initialCourseId]="courseId()"
        (curriculumChanged)="onCurriculumChanged($event)"
        (courseReady)="onCourseReady($event)"
      />
    }
  `,
})
export class BuilderCurriculumStepComponent implements OnInit, OnDestroy {
  private readonly builder = inject(CourseBuilderService);
  private readonly shell = inject(CourseBuilderShellService);

  readonly bootstrapping = signal(true);
  readonly courseId = signal<number | null>(null);
  readonly authError = signal<string | null>(null);

  ngOnInit(): void {
    this.shell.registerSaveHandler(() => this.handleSave());
    void this.bootstrap();
  }

  ngOnDestroy(): void {
    this.shell.unregisterSaveHandler();
  }

  onCurriculumChanged(hasModules: boolean): void {
    this.shell.setSectionStatus('curriculum', hasModules);
  }

  onCourseReady(id: number): void {
    this.courseId.set(id);
    this.authError.set(null);
  }

  private async bootstrap(): Promise<void> {
    this.bootstrapping.set(true);
    this.authError.set(null);
    try {
      const id = await this.builder.ensureCourseId();
      this.courseId.set(id);
    } catch (error) {
      if (this.builder.isUnauthorizedError(error)) {
        this.authError.set('Tu sesión expiró. Inicia sesión de nuevo.');
      } else {
        this.authError.set(this.builder.getApiErrorMessage(error));
      }
    } finally {
      this.bootstrapping.set(false);
    }
  }

  private async handleSave(): Promise<void> {
    try {
      const id = await this.builder.ensureCourseId();
      this.courseId.set(id);
      this.authError.set(null);
    } catch (error) {
      if (this.builder.isUnauthorizedError(error)) {
        this.authError.set('Tu sesión expiró. Inicia sesión de nuevo.');
      } else {
        this.authError.set(this.builder.getApiErrorMessage(error));
      }
      throw error;
    }
  }
}
