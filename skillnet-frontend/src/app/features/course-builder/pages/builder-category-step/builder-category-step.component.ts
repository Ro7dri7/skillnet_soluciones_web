import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { OFFICIAL_CATEGORIES } from '../../../marketplace/data/categories.data';
import { CourseBuilderService } from '../../../../core/services/course-builder.service';
import { SearchableSelectComponent } from '../../components/searchable-select/searchable-select.component';
import { builderStepRoute } from '../../data/builder-steps.data';

@Component({
  selector: 'app-builder-category-step',
  standalone: true,
  imports: [SearchableSelectComponent],
  templateUrl: './builder-category-step.component.html',
})
export class BuilderCategoryStepComponent {
  private readonly router = inject(Router);
  private readonly builder = inject(CourseBuilderService);

  readonly categories = OFFICIAL_CATEGORIES;
  readonly productLabel = this.builder.productLabel;
  readonly selectedCategory = signal(this.builder.category());

  onCategoryChange(category: string): void {
    this.selectedCategory.set(category);
    this.builder.setCategory(category);
  }

  goBack(): void {
    void this.router.navigateByUrl(builderStepRoute('title'));
  }

  goNext(): void {
    if (!this.selectedCategory()) {
      return;
    }
    void this.router.navigateByUrl(builderStepRoute('subcategory'));
  }
}
