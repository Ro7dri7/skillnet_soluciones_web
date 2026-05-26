import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import {
  OFFICIAL_CATEGORIES,
  getParentCategory,
  getSubcategories,
} from '../../data/categories.data';

@Component({
  selector: 'app-marketplace-category-bar',
  standalone: true,
  templateUrl: './marketplace-category-bar.component.html',
  styleUrl: './marketplace-category-bar.component.scss',
})
export class MarketplaceCategoryBarComponent {
  private readonly router = inject(Router);

  readonly officialCategories = OFFICIAL_CATEGORIES;
  readonly hoveredCategory = signal<string | null>(null);

  private readonly categoryParam = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.readCategoryParam()),
      startWith(this.readCategoryParam()),
    ),
    { initialValue: '' },
  );

  readonly activeParent = computed(() => getParentCategory(this.categoryParam()));

  readonly hoveredSubcategories = computed(() => {
    const hovered = this.hoveredCategory();
    return hovered ? getSubcategories(hovered) : [];
  });

  readonly activeSubcategories = computed(() => {
    const parent = this.activeParent();
    return parent ? getSubcategories(parent) : [];
  });

  isCategoryHovered(cat: string): boolean {
    return this.hoveredCategory() === cat;
  }

  isSubcategoryActive(sub: string): boolean {
    return this.categoryParam() === sub;
  }

  onBarMouseLeave(): void {
    this.hoveredCategory.set(null);
  }

  onCategoryMouseEnter(cat: string): void {
    this.hoveredCategory.set(cat);
  }

  navigateCategory(category: string): void {
    void this.router.navigate(['/catalog'], { queryParams: { category } });
    this.hoveredCategory.set(null);
  }

  navigateParent(): void {
    const parent = this.activeParent();
    if (parent) {
      this.navigateCategory(parent);
    }
  }

  private readCategoryParam(): string {
    let route = this.router.routerState.root;
    while (route.firstChild) {
      route = route.firstChild;
    }
    return route.snapshot.queryParamMap.get('category') ?? '';
  }
}
