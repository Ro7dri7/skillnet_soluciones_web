import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../../../core/services/cart.service';
import { MarketplaceCourse } from '../../models/marketplace-course.model';

@Component({
  selector: 'app-marketplace-course-card',
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe, RouterLink],
  templateUrl: './marketplace-course-card.component.html',
})
export class MarketplaceCourseCardComponent {
  private readonly router = inject(Router);
  readonly cartService = inject(CartService);

  readonly course = input.required<MarketplaceCourse>();

  readonly inCart = computed(() => this.cartService.isInCart(this.course().id));

  levelLabel(level: string): string {
    const map: Record<string, string> = {
      beginner: 'Principiante',
      intermediate: 'Intermedio',
      advanced: 'Avanzado',
    };
    return map[level] ?? level;
  }

  onBuy(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.cartService.addToCart(this.course());
    void this.router.navigate(['/checkout']);
  }

  onWishlist(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
  }

  onToggleCart(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.cartService.toggleCart(this.course());
  }
}
