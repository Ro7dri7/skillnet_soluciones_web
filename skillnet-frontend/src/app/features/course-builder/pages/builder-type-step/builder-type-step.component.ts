import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CourseBuilderService } from '../../../../core/services/course-builder.service';
import { PRODUCT_TYPES, ProductTypeOption } from '../../data/product-types.data';
import { builderStepRoute } from '../../data/builder-steps.data';
import type { ProductType } from '../../../../shared/models/course-builder.model';

@Component({
  selector: 'app-builder-type-step',
  standalone: true,
  imports: [],
  templateUrl: './builder-type-step.component.html',
  styleUrl: './builder-type-step.component.scss',
})
export class BuilderTypeStepComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly builder = inject(CourseBuilderService);

  readonly productTypes = PRODUCT_TYPES;
  readonly selectedType = signal<ProductType | null>(null);
  readonly showComingSoon = signal(false);
  readonly loading = signal(false);

  readonly rowOne = PRODUCT_TYPES.slice(0, 5);
  readonly rowTwo = PRODUCT_TYPES.slice(5);

  ngOnInit(): void {
    this.builder.reset();
  }

  handleCardClick(type: ProductTypeOption): void {
    if (type.comingSoon) {
      this.showComingSoon.set(true);
      this.selectedType.set(null);
      return;
    }
    this.selectedType.set(type.id);
  }

  goBack(): void {
    void this.router.navigate(['/courses']);
  }

  goNext(): void {
    const type = this.selectedType();
    if (!type || this.loading()) {
      return;
    }
    const option = PRODUCT_TYPES.find((t) => t.id === type);
    this.builder.setProductType(type, option?.label ?? 'Producto');
    void this.router.navigateByUrl(builderStepRoute('title'));
  }

  closeComingSoon(): void {
    this.showComingSoon.set(false);
  }

  isSelected(type: ProductTypeOption): boolean {
    return this.selectedType() === type.id;
  }
}
