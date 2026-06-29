import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CourseService } from '../../../../core/services/course.service';
import { OwnedCoursesService } from '../../../../core/services/owned-courses.service';
import {
  MARKETPLACE_CATEGORIES,
  getParentCategory,
  getSubcategories,
  isOfficialCategory,
} from '../../data/categories.data';
import { MarketplaceCourse } from '../../models/marketplace-course.model';
import { MarketplaceCourseCardComponent } from '../../components/marketplace-course-card/marketplace-course-card.component';
import { courseToMarketplace } from '../../../../shared/utils/marketplace-course.util';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [RouterLink, MarketplaceCourseCardComponent],
  templateUrl: './catalog.component.html',
})
export class CatalogComponent implements OnInit {
  private readonly courseService = inject(CourseService);
  private readonly ownedCourses = inject(OwnedCoursesService);
  private readonly route = inject(ActivatedRoute);

  readonly isLoading = signal(true);
  readonly courses = signal<MarketplaceCourse[]>([]);
  readonly categories = MARKETPLACE_CATEGORIES;
  readonly selectedCategory = signal<string>('');

  ngOnInit(): void {
    this.ownedCourses.refresh();
    this.route.queryParamMap.subscribe((params) => {
      this.selectedCategory.set(params.get('category') ?? '');
      this.load();
    });
  }

  filteredCourses(): MarketplaceCourse[] {
    const cat = this.selectedCategory();
    const all = this.courses();
    if (!cat) {
      return all;
    }
    if (isOfficialCategory(cat)) {
      const subs = getSubcategories(cat);
      return all.filter((c) => c.category === cat || subs.includes(c.category));
    }
    const parent = getParentCategory(cat);
    return all.filter((c) => c.category === cat || c.category === parent);
  }

  selectCategory(cat: string): void {
    this.selectedCategory.set(cat === this.selectedCategory() ? '' : cat);
  }

  private load(): void {
    this.isLoading.set(true);
    this.courseService.getCourses().subscribe({
      next: (items) => {
        const list = items
          .filter((c) => c.status === 'published')
          .map((c) => courseToMarketplace(c));
        this.courses.set(list);
        this.isLoading.set(false);
      },
      error: () => {
        this.courses.set([]);
        this.isLoading.set(false);
      },
    });
  }
}
