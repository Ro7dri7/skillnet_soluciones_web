import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CourseBuilderService } from '../../../../core/services/course-builder.service';
import { builderStepRoute } from '../../data/builder-steps.data';

@Component({
  selector: 'app-builder-title-step',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './builder-title-step.component.html',
})
export class BuilderTitleStepComponent {
  private readonly router = inject(Router);
  private readonly builder = inject(CourseBuilderService);

  readonly maxChars = 60;
  readonly title = signal(this.builder.title() || '');
  readonly loading = signal(false);

  onTitleChange(value: string): void {
    this.title.set(value.slice(0, this.maxChars));
  }

  continue(): void {
    const trimmed = this.title().trim();
    if (!trimmed || this.loading()) {
      return;
    }
    this.builder.setTitle(trimmed);
    void this.router.navigateByUrl(builderStepRoute('category'));
  }
}
