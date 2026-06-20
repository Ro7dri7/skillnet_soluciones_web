import { CurrencyPipe, DecimalPipe } from '@angular/common';
import {
  Component,
  ElementRef,
  HostListener,
  OnInit,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { CartService } from '../../../../core/services/cart.service';
import { CheckoutService } from '../../../../core/services/checkout.service';
import { CourseService } from '../../../../core/services/course.service';
import { ReviewService, CourseReview } from '../../../../core/services/review.service';
import { StudentService } from '../../../../core/services/student.service';
import { ToastService } from '../../../../core/services/toast.service';
import { CourseResponse, ProfessorSummary } from '../../../../shared/models/course.model';
import {
  formatCatalogPrice,
  getCatalogDisplayPricesWithOverride,
} from '../../../../shared/utils/catalog-price.util';
import {
  courseCoverUrl,
  courseToMarketplace,
  professorDisplayName,
} from '../../../../shared/utils/marketplace-course.util';
import { absoluteMediaUrl } from '../../../../shared/utils/media-url.util';
import {
  getProductLandingCopy,
  parseMultilineList,
} from '../../../../shared/utils/product-landing-copy.util';
import {
  courseLandingRouterLink,
  courseLearnPath,
  courseRouteNeedsRedirect,
  normalizeCourseSlugForUrl,
  slugFromRouteParams,
} from '../../../../shared/utils/course-slug.util';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';
import { MarketplaceCourseCardComponent } from '../../components/marketplace-course-card/marketplace-course-card.component';
import { MarketplaceCourse } from '../../models/marketplace-course.model';

@Component({
  selector: 'app-course-landing',
  standalone: true,
  imports: [
    RouterLink,
    CurrencyPipe,
    DecimalPipe,
    ReactiveFormsModule,
    MarketplaceCourseCardComponent,
  ],
  templateUrl: './course-landing.component.html',
})
export class CourseLandingComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly courseService = inject(CourseService);
  private readonly studentService = inject(StudentService);
  private readonly reviewService = inject(ReviewService);
  private readonly authService = inject(AuthService);
  private readonly cartService = inject(CartService);
  private readonly checkoutService = inject(CheckoutService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly instructorCarousel = viewChild<ElementRef<HTMLElement>>('instructorCarousel');
  readonly relatedCarousel = viewChild<ElementRef<HTMLElement>>('relatedCarousel');

  readonly course = signal<CourseResponse | null>(null);
  readonly isLoading = signal(true);
  readonly notFound = signal(false);
  readonly isEnrolled = signal(false);
  readonly reviews = signal<CourseReview[]>([]);
  readonly reviewsLoading = signal(false);
  readonly reviewSubmitting = signal(false);
  readonly reviewError = signal<string | null>(null);
  readonly expandedModuleId = signal<number | string | null>(null);
  readonly sidebarCollapsed = signal(false);
  readonly instructorCourses = signal<MarketplaceCourse[]>([]);
  readonly relatedCourses = signal<MarketplaceCourse[]>([]);
  readonly couponCode = signal('');
  readonly couponStatus = signal<{ type: 'error' | 'success'; message: string } | null>(null);
  readonly discountedPrice = signal<number | null>(null);
  readonly couponChecking = signal(false);

  readonly reviewForm = this.fb.nonNullable.group({
    rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: [''],
  });

  readonly coverUrl = computed(() => courseCoverUrl(this.course()));
  readonly introVideoUrl = computed(() => {
    const url = this.course()?.videoUrl?.trim();
    return url ? absoluteMediaUrl(url) : null;
  });
  readonly landingCopy = computed(() => getProductLandingCopy(this.course()?.courseFormat));
  readonly goals = computed(() => parseMultilineList(this.course()?.whatYouWillLearn));
  readonly audience = computed(() => parseMultilineList(this.course()?.targetAudience));
  readonly requirements = computed(() => parseMultilineList(this.course()?.requirements));
  readonly canReview = computed(() => this.isEnrolled() && this.authService.isLoggedIn());
  readonly averageRating = computed(() => {
    const list = this.reviews();
    if (list.length === 0) {
      return 0;
    }
    const sum = list.reduce((acc, r) => acc + Number(r.rating), 0);
    return Math.round((sum / list.length) * 10) / 10;
  });
  readonly displayPrices = computed(() => {
    const course = this.course();
    if (!course) {
      return { displayPrice: 0, displayOldPrice: 0, showStrikethrough: false };
    }
    return getCatalogDisplayPricesWithOverride(course, this.discountedPrice());
  });
  readonly formatLabel = computed(() => {
    const raw = this.course()?.courseFormat?.trim().toLowerCase();
    const map: Record<string, string> = {
      video: 'Curso',
      course: 'Curso',
      ebook: 'Ebook',
      podcast: 'Podcast',
      audio: 'Audio',
    };
    return map[raw ?? ''] ?? 'Curso';
  });

  @HostListener('window:scroll')
  onWindowScroll(): void {
    this.sidebarCollapsed.set(window.scrollY > 420);
  }

  ngOnInit(): void {
    const formatParam = this.route.snapshot.paramMap.get('format');
    const slugParam = this.route.snapshot.paramMap.get('slug');
    const slug = slugFromRouteParams(formatParam, slugParam);
    if (!slug) {
      this.notFound.set(true);
      this.isLoading.set(false);
      return;
    }

    this.courseService.getCourseBySlug(slug, formatParam).subscribe({
      next: (found) => {
        const canonical = normalizeCourseSlugForUrl(found.slug);
        if (courseRouteNeedsRedirect(formatParam, slugParam, canonical, found.courseFormat)) {
          void this.router.navigate(courseLandingRouterLink(canonical, found.courseFormat), {
            replaceUrl: true,
          });
          return;
        }
        this.course.set(found);
        this.notFound.set(false);
        this.isLoading.set(false);
        this.checkEnrollment(canonical);
        this.loadReviews(canonical);
        this.loadRelatedCourses(found);
      },
      error: () => {
        this.notFound.set(true);
        this.isLoading.set(false);
      },
    });
  }

  learnPath(): string {
    const course = this.course();
    return course?.slug ? courseLearnPath(course.slug, course.courseFormat) : '/mis-cursos';
  }

  professorName(): string {
    const course = this.course();
    return course ? professorDisplayName(course) : 'Infoproductor';
  }

  professorProfileLink(professor?: ProfessorSummary | null): string[] {
    const username = professor?.username?.trim();
    return username ? ['/infoproductor', username] : ['/marketplace'];
  }

  professorAvatarUrl(professor?: ProfessorSummary | null): string {
    const name = encodeURIComponent(this.professorName());
    return `https://ui-avatars.com/api/?name=${name}&background=89CEFF&color=032b60&bold=true`;
  }

  formatPrice(amount: number): string {
    return formatCatalogPrice(amount, this.course()?.currency ?? 'USD');
  }

  purchaseCta(): string {
    return this.isEnrolled() ? 'Continuar aprendiendo' : 'Comprar ahora';
  }

  levelLabel(level: string): string {
    const map: Record<string, string> = {
      beginner: 'Principiante',
      intermediate: 'Intermedio',
      advanced: 'Avanzado',
    };
    return map[level] ?? level;
  }

  reviewerName(review: CourseReview): string {
    const user = review.user;
    if (!user) {
      return 'Estudiante';
    }
    const full = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
    return full || user.username;
  }

  reviewerAvatar(review: CourseReview): string {
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(this.reviewerName(review))}&background=random`;
  }

  toggleModule(moduleId: number | string): void {
    this.expandedModuleId.update((current) => (current === moduleId ? null : moduleId));
  }

  openIntroPreview(): void {
    const url = this.introVideoUrl();
    if (url) {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  }

  onCouponCodeInput(value: string): void {
    this.couponCode.set(value);
    this.couponStatus.set(null);
  }

  checkCoupon(): void {
    const course = this.course();
    const code = this.couponCode().trim();
    if (!course || !code) {
      this.couponStatus.set({ type: 'error', message: 'Por favor, ingresa un código de cupón.' });
      this.discountedPrice.set(null);
      return;
    }
    this.couponChecking.set(true);
    this.checkoutService.quote({ courseIds: [course.id], couponCode: code }).subscribe({
      next: (res) => {
        this.couponChecking.set(false);
        if (res.couponValid) {
          this.couponStatus.set({ type: 'success', message: '¡Cupón aplicado!' });
          this.discountedPrice.set(res.total);
        } else {
          this.couponStatus.set({
            type: 'error',
            message: res.message ?? 'El código no es válido o ha expirado.',
          });
          this.discountedPrice.set(null);
        }
      },
      error: () => {
        this.couponChecking.set(false);
        this.couponStatus.set({ type: 'error', message: 'Código de cupón inválido o expirado.' });
        this.discountedPrice.set(null);
      },
    });
  }

  buyNow(): void {
    const course = this.course();
    if (!course) {
      return;
    }
    if (this.isEnrolled()) {
      void this.router.navigateByUrl(this.learnPath());
      return;
    }
    this.cartService.clearCart();
    this.cartService.addToCart(courseToMarketplace(course));
    void this.router.navigate(['/checkout'], {
      state: {
        couponCode:
          this.couponStatus()?.type === 'success' ? this.couponCode().trim().toUpperCase() : null,
      },
    });
  }

  addToCart(): void {
    const course = this.course();
    if (!course) {
      return;
    }
    this.cartService.addToCart(courseToMarketplace(course));
    this.toast.success('Agregado al carrito');
    void this.router.navigate(['/checkout']);
  }

  scrollCarousel(target: 'instructor' | 'related', direction: 'left' | 'right'): void {
    const ref =
      target === 'instructor' ? this.instructorCarousel() : this.relatedCarousel();
    const el = ref?.nativeElement;
    if (!el) {
      return;
    }
    el.scrollBy({ left: direction === 'left' ? -600 : 600, behavior: 'smooth' });
  }

  submitReview(): void {
    const slug = this.course()?.slug;
    if (!slug || this.reviewForm.invalid) {
      this.reviewForm.markAllAsTouched();
      return;
    }
    this.reviewSubmitting.set(true);
    this.reviewError.set(null);
    const { rating, comment } = this.reviewForm.getRawValue();
    this.reviewService.addReview(slug, { rating, comment: comment || undefined }, this.course()?.courseFormat).subscribe({
      next: (created) => {
        this.reviews.update((list) => [created, ...list]);
        this.reviewForm.patchValue({ comment: '' });
        this.reviewSubmitting.set(false);
        this.toast.success('Reseña publicada');
      },
      error: (err) => {
        this.reviewError.set(messageFromHttpError(err, 'No se pudo publicar la reseña.'));
        this.reviewSubmitting.set(false);
      },
    });
  }

  starFilled(star: number, rating: number): boolean {
    return star <= Math.round(rating);
  }

  private loadReviews(slug: string): void {
    this.reviewsLoading.set(true);
    this.reviewService.getReviews(slug, this.course()?.courseFormat).subscribe({
      next: (rows) => {
        this.reviews.set(rows);
        this.reviewsLoading.set(false);
      },
      error: () => this.reviewsLoading.set(false),
    });
  }

  private checkEnrollment(slug: string): void {
    if (!this.authService.isLoggedIn()) {
      this.isEnrolled.set(false);
      return;
    }
    this.studentService.getMyCourses().subscribe({
      next: (courses) => {
        const enrolled = courses.some(
          (c) => normalizeCourseSlugForUrl(c.slug) === normalizeCourseSlugForUrl(slug),
        );
        this.isEnrolled.set(enrolled);
      },
      error: () => this.isEnrolled.set(false),
    });
  }

  private loadRelatedCourses(course: CourseResponse): void {
    if (course.professorId != null) {
      this.courseService.getCoursesByProfessor(course.professorId).subscribe({
        next: (rows) => {
          this.instructorCourses.set(
            rows
              .filter((c) => c.id !== course.id && c.status === 'published')
              .slice(0, 10)
              .map((c) => courseToMarketplace(c)),
          );
        },
      });
    }
    if (course.category?.trim()) {
      this.courseService.getCoursesByCategory(course.category).subscribe({
        next: (rows) => {
          this.relatedCourses.set(
            rows
              .filter((c) => c.id !== course.id && c.status === 'published')
              .slice(0, 10)
              .map((c) => courseToMarketplace(c)),
          );
        },
      });
    }
  }
}
