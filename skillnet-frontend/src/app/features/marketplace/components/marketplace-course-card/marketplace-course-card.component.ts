import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MarketplaceCourse } from '../../models/marketplace-course.model';

@Component({
  selector: 'app-marketplace-course-card',
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe, RouterLink],
  templateUrl: './marketplace-course-card.component.html',
})
export class MarketplaceCourseCardComponent {
  readonly course = input.required<MarketplaceCourse>();

  readonly addToCart = output<MarketplaceCourse>();
  readonly addToWishlist = output<MarketplaceCourse>();

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
    this.addToCart.emit(this.course());
  }

  onWishlist(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.addToWishlist.emit(this.course());
  }
}
