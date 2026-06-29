import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CourseService } from '../../../../core/services/course.service';
import { AuthService } from '../../../../core/services/auth.service';
import { OwnedCoursesService } from '../../../../core/services/owned-courses.service';
import { User } from '../../../../shared/models/auth.model';
import { MARKETPLACE_CATEGORIES, OFFER_FORMATS } from '../../data/categories.data';
import { MarketplaceCourse } from '../../models/marketplace-course.model';
import { MarketplaceCarouselComponent } from '../../components/marketplace-carousel/marketplace-carousel.component';
import { courseToMarketplace } from '../../../../shared/utils/marketplace-course.util';

@Component({
  selector: 'app-marketplace',
  standalone: true,
  imports: [RouterLink, MarketplaceCarouselComponent],
  templateUrl: './marketplace.component.html',
})
export class MarketplaceComponent implements OnInit, OnDestroy {
  private readonly courseService = inject(CourseService);
  private readonly authService = inject(AuthService);
  private readonly ownedCourses = inject(OwnedCoursesService);
  private readonly router = inject(Router);

  private heroAdvanceTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly heroVideoSlideMs = 18_000;
  private readonly heroContentSlideMs = 6_000;

  readonly isLoading = signal(true);
  readonly currentHeroSlide = signal(0);
  readonly courses = signal<MarketplaceCourse[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly categorized = signal<Record<string, MarketplaceCourse[]>>({});

  readonly offerFormats = OFFER_FORMATS;
  readonly user = this.authService.getCurrentUser();

  ngOnInit(): void {
    this.ownedCourses.refresh();
    this.loadCourses();
    this.scheduleHeroAdvance();
  }

  ngOnDestroy(): void {
    this.clearHeroAdvanceTimer();
  }

  loadCourses(): void {
    this.isLoading.set(true);
    this.loadError.set(null);
    this.courseService.getCourses().subscribe({
      next: (items) => {
        const list = items
          .filter((c) => c.status === 'published')
          .map((c) => courseToMarketplace(c));
        this.courses.set(list);
        this.buildCategories(list);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.courses.set([]);
        this.buildCategories([]);
        this.loadError.set('No se pudieron cargar los cursos del marketplace.');
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
    this.scheduleHeroAdvance();
  }

  private scheduleHeroAdvance(): void {
    this.clearHeroAdvanceTimer();
    const delay =
      this.currentHeroSlide() === 0 ? this.heroVideoSlideMs : this.heroContentSlideMs;
    this.heroAdvanceTimer = setTimeout(() => {
      this.currentHeroSlide.update((v) => (v === 0 ? 1 : 0));
      this.scheduleHeroAdvance();
    }, delay);
  }

  private clearHeroAdvanceTimer(): void {
    if (this.heroAdvanceTimer !== null) {
      clearTimeout(this.heroAdvanceTimer);
      this.heroAdvanceTimer = null;
    }
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

  categoryEntries(): [string, MarketplaceCourse[]][] {
    return Object.entries(this.categorized()).filter(([, list]) => list.length > 0);
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
}
