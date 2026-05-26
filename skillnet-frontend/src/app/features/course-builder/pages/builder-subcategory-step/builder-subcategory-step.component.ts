import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { getSubcategories } from '../../../marketplace/data/categories.data';
import { CourseBuilderService } from '../../../../core/services/course-builder.service';
import { SearchableSelectComponent } from '../../components/searchable-select/searchable-select.component';
import { builderStepRoute } from '../../data/builder-steps.data';

@Component({
  selector: 'app-builder-subcategory-step',
  standalone: true,
  imports: [SearchableSelectComponent],
  templateUrl: './builder-subcategory-step.component.html',
})
export class BuilderSubcategoryStepComponent {
  private readonly router = inject(Router);
  private readonly builder = inject(CourseBuilderService);

  readonly productLabel = this.builder.productLabel;
  readonly selectedCategory = this.builder.category;
  readonly selectedSubcategory = signal(this.builder.subcategory());

  readonly subcategories = computed(() => {
    const subs = getSubcategories(this.selectedCategory());
    return subs.length ? subs : [];
  });

  onSubcategoryChange(value: string): void {
    this.selectedSubcategory.set(value);
    this.builder.setSubcategory(value);
  }

  goBack(): void {
    void this.router.navigateByUrl(builderStepRoute('category'));
  }

  async goNext(): Promise<void> {
    if (!this.selectedSubcategory()) {
      return;
    }
    try {
      const courseId = await this.builder.ensureCourseId();
      await this.router.navigate(['/instructor/courses', courseId, 'manage', 'audience']);
    } catch {
      await this.router.navigateByUrl(builderStepRoute('audience'));
    }
  }
}
