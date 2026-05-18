import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CourseService } from '../../../../core/services/course.service';
import { AuthService } from '../../../../core/services/auth.service';
import { User } from '../../../../shared/models/auth.model';
import { CourseResponse } from '../../../../shared/models/course.model';
import { MARKETPLACE_CATEGORIES, OFFER_FORMATS } from '../../data/categories.data';
import { MarketplaceCourse } from '../../models/marketplace-course.model';
import { MarketplaceCarouselComponent } from '../../components/marketplace-carousel/marketplace-carousel.component';

@Component({
  selector: 'app-marketplace',
  standalone: true,
  imports: [RouterLink, MarketplaceCarouselComponent],
  templateUrl: './marketplace.component.html',
})
export class MarketplaceComponent implements OnInit {
  private readonly courseService = inject(CourseService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly isLoading = signal(true);
  readonly currentHeroSlide = signal(0);
  readonly courses = signal<MarketplaceCourse[]>([]);
  readonly categorized = signal<Record<string, MarketplaceCourse[]>>({});

  readonly offerFormats = OFFER_FORMATS;
  readonly user = this.authService.getCurrentUser();

  ngOnInit(): void {
    this.loadCourses();
    setInterval(() => {
      this.currentHeroSlide.update((v) => (v === 0 ? 1 : 0));
    }, 6000);
  }

  loadCourses(): void {
    this.isLoading.set(true);
    this.courseService.getCourses().subscribe({
      next: (items) => {
        const published = items
          .filter((c) => c.status === 'published')
          .map((c, i) => this.toMarketplaceCourse(c, i));
        this.courses.set(published);
        this.buildCategories(published);
        this.isLoading.set(false);
      },
      error: () => {
        this.courses.set(this.mockCourses());
        this.buildCategories(this.courses());
        this.isLoading.set(false);
      },
    });
  }

  welcomeName(user: User | null): string {
    if (!user) {
      return 'Estudiante';
    }
    return user.firstName || user.username;
  }

  userInitial(user: User | null): string {
    const n = user?.firstName || user?.username || 'U';
    return n.charAt(0).toUpperCase();
  }

  setHeroSlide(index: number): void {
    this.currentHeroSlide.set(index);
  }

  scrollOffers(direction: 'left' | 'right'): void {
    const el = document.getElementById('offer-carousel');
    if (el) {
      el.scrollBy({ left: direction === 'left' ? -280 : 280, behavior: 'smooth' });
    }
  }

  goCatalog(category?: string): void {
    void this.router.navigate(['/catalog'], {
      queryParams: category ? { category } : {},
    });
  }

  private buildCategories(list: MarketplaceCourse[]): void {
    const map: Record<string, MarketplaceCourse[]> = {};
    for (const cat of MARKETPLACE_CATEGORIES) {
      map[cat] = list.filter((c) => c.category === cat).slice(0, 10);
    }
    const uncategorized = list.filter(
      (c) => !(MARKETPLACE_CATEGORIES as readonly string[]).includes(c.category),
    );
    if (uncategorized.length) {
      map['Otros'] = uncategorized.slice(0, 10);
    }
    this.categorized.set(map);
  }

  private toMarketplaceCourse(course: CourseResponse, index: number): MarketplaceCourse {
    const categories = [...MARKETPLACE_CATEGORIES];
    return {
      id: course.id,
      title: course.title,
      slug: course.slug,
      description: course.description,
      level: course.level,
      status: course.status,
      price: course.price,
      originalPrice: Math.round(course.price * 1.25),
      category: categories[index % categories.length],
      format: index % 2 === 0 ? 'Curso' : 'Ebook',
      rating: 4.2 + (index % 8) * 0.1,
      enrollmentCount: 120 + index * 37,
      lessonsCount: 12 + (index % 20),
      professorName: 'Skillnet Academy',
      imageUrl: null,
    };
  }

  private mockCourses(): MarketplaceCourse[] {
    return Array.from({ length: 12 }).map((_, i) => ({
      id: i + 1,
      title: `Curso profesional ${i + 1}: habilidades para el mercado digital`,
      slug: `curso-${i + 1}`,
      description: 'Descripción del curso',
      level: 'intermediate',
      status: 'published',
      price: 49 + i * 10,
      originalPrice: 79 + i * 10,
      category: MARKETPLACE_CATEGORIES[i % MARKETPLACE_CATEGORIES.length],
      format: 'Curso',
      rating: 4.5,
      enrollmentCount: 200 + i * 10,
      lessonsCount: 24,
      professorName: 'Prof. Skillnet',
      imageUrl: null,
    }));
  }

  categoryEntries(): [string, MarketplaceCourse[]][] {
    return Object.entries(this.categorized()).filter(([, list]) => list.length > 0);
  }
}
