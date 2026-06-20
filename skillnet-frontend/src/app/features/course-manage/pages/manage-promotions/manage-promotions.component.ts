import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, OnDestroy, OnInit, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { CourseManageContextService } from '../../../../core/services/course-manage-context.service';
import { CourseService } from '../../../../core/services/course.service';
import { ManageLayoutSaveService } from '../../../../core/services/manage-layout-save.service';
import { ProducerCoursesService } from '../../../../core/services/producer-courses.service';
import { ToastService } from '../../../../core/services/toast.service';
import { CourseCouponResponse } from '../../../../shared/models/producer-course.model';
import { ManageCouponModalComponent } from './manage-coupon-modal.component';
import { ReferralService } from '../../../../core/services/referral.service';

@Component({
  selector: 'app-manage-promotions',
  standalone: true,
  imports: [FormsModule, DatePipe, ManageCouponModalComponent],
  templateUrl: './manage-promotions.component.html',
  styleUrl: './manage-promotions.component.scss',
})
export class ManagePromotionsComponent implements OnInit, OnDestroy {
  private readonly manageContext = inject(CourseManageContextService);
  private readonly courseService = inject(CourseService);
  private readonly producerCourses = inject(ProducerCoursesService);
  private readonly manageSave = inject(ManageLayoutSaveService);
  private readonly toast = inject(ToastService);

  private readonly referralService = inject(ReferralService);

  readonly loading = signal(true);
  readonly coupons = signal<CourseCouponResponse[]>([]);
  readonly searchQuery = signal('');
  readonly copied = signal(false);
  readonly modalOpen = signal(false);
  readonly savingCoupon = signal(false);
  readonly referralLink = signal('');

  readonly courseSlug = signal('');

  readonly filteredCoupons = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    if (!q) {
      return this.coupons();
    }
    return this.coupons().filter((c) => c.code.toLowerCase().includes(q));
  });

  private loadedCourseId: number | null = null;

  constructor() {
    effect(() => {
      const id = this.manageContext.courseId();
      if (id != null && id !== this.loadedCourseId) {
        this.loadedCourseId = id;
        untracked(() => void this.load(id));
      }
    });
  }

  ngOnInit(): void {
    this.manageSave.registerSaveHandler(async () => {
      this.toast.info('Los cupones se guardan al crearlos o eliminarlos.');
    });
  }

  ngOnDestroy(): void {
    this.manageSave.unregisterSaveHandler();
  }

  async copyReferralLink(): Promise<void> {
    const link = this.referralLink();
    if (!link) {
      return;
    }
    try {
      await navigator.clipboard.writeText(link);
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    } catch {
      this.toast.error('No se pudo copiar el enlace.');
    }
  }

  openModal(): void {
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
  }

  async createCoupon(payload: { code: string; percentOff: number }): Promise<void> {
    const courseId = this.manageContext.courseId();
    if (!courseId) {
      return;
    }
    this.savingCoupon.set(true);
    try {
      const created = await firstValueFrom(
        this.producerCourses.createCoupon(courseId, {
          code: payload.code,
          percentOff: payload.percentOff,
        }),
      );
      this.coupons.update((list) => [created, ...list]);
      this.modalOpen.set(false);
      this.toast.success('Cupón creado');
    } catch {
      this.toast.error('No se pudo crear el cupón.');
    } finally {
      this.savingCoupon.set(false);
    }
  }

  async deleteCoupon(id: number): Promise<void> {
    const courseId = this.manageContext.courseId();
    if (!courseId || !confirm('¿Estás seguro de eliminar este cupón?')) {
      return;
    }
    try {
      await firstValueFrom(this.producerCourses.deleteCoupon(courseId, id));
      this.coupons.update((list) => list.filter((c) => c.id !== id));
      this.toast.success('Cupón eliminado');
    } catch {
      this.toast.error('No se pudo eliminar el cupón.');
    }
  }

  copyCouponLink(code: string): void {
    const slug = this.courseSlug();
    if (!slug) {
      return;
    }
    const url = `${window.location.origin}/course/${slug}?couponCode=${code}`;
    void navigator.clipboard.writeText(url);
    this.toast.success('Enlace promocional copiado');
  }

  discountLabel(coupon: CourseCouponResponse): string {
    if (coupon.percentOff > 0) {
      return `-${coupon.percentOff}%`;
    }
    return `-$${coupon.amountOff}`;
  }

  private async load(id: number): Promise<void> {
    try {
      const [course, coupons, referral] = await Promise.all([
        firstValueFrom(this.courseService.getCourse(id)),
        firstValueFrom(this.producerCourses.listCoupons(id)),
        firstValueFrom(this.referralService.getReferralLink(id)).catch(() => null),
      ]);
      this.courseSlug.set(course.slug);
      this.coupons.set(coupons);
      this.manageContext.setSectionStatus('promotions', coupons.length > 0);
      this.referralLink.set(referral?.url ?? '');
    } catch {
      this.toast.error('No se pudieron cargar las promociones.');
    } finally {
      this.loading.set(false);
    }
  }
}
