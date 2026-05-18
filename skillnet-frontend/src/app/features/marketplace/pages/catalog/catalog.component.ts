import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CourseService } from '../../../../core/services/course.service';
import { MARKETPLACE_CATEGORIES } from '../../data/categories.data';
import { MarketplaceCourse } from '../../models/marketplace-course.model';
import { MarketplaceCourseCardComponent } from '../../components/marketplace-course-card/marketplace-course-card.component';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [RouterLink, MarketplaceCourseCardComponent],
  templateUrl: './catalog.component.html',
})
export class CatalogComponent implements OnInit {
  private readonly courseService = inject(CourseService);
  private readonly route = inject(ActivatedRoute);

  readonly isLoading = signal(true);
  readonly courses = signal<MarketplaceCourse[]>([]);
  readonly categories = MARKETPLACE_CATEGORIES;
  readonly selectedCategory = signal<string>('');

  ngOnInit(): void {
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
    return all.filter((c) => c.category === cat);
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
          .map((c, i) => this.mapCourse(c, i));
        this.courses.set(list);
        this.isLoading.set(false);
      },
      error: () => {
        this.courses.set([]);
        this.isLoading.set(false);
      },
    });
  }

  private mapCourse(
    c: { id: number; title: string; slug: string; description: string; level: string; status: string; price: number },
    index: number,
  ): MarketplaceCourse {
    return {
      id: c.id,
      title: c.title,
      slug: c.slug,
      description: c.description,
      level: c.level,
      status: c.status,
      price: c.price,
      category: MARKETPLACE_CATEGORIES[index % MARKETPLACE_CATEGORIES.length],
      format: 'Curso',
      rating: 4.6,
      enrollmentCount: 100,
      lessonsCount: 20,
      professorName: 'Skillnet Academy',
      imageUrl: null,
    };
  }
}
