import { Component, ElementRef, input, viewChild } from '@angular/core';
import { MarketplaceCourse } from '../../models/marketplace-course.model';
import { MarketplaceCourseCardComponent } from '../marketplace-course-card/marketplace-course-card.component';

@Component({
  selector: 'app-marketplace-carousel',
  standalone: true,
  imports: [MarketplaceCourseCardComponent],
  templateUrl: './marketplace-carousel.component.html',
})
export class MarketplaceCarouselComponent {
  readonly title = input.required<string>();
  readonly courses = input.required<MarketplaceCourse[]>();
  readonly sectionId = input<string>('');

  readonly scrollRef = viewChild<ElementRef<HTMLElement>>('track');

  scroll(direction: 'left' | 'right'): void {
    const el = this.scrollRef()?.nativeElement;
    if (!el) {
      return;
    }
    el.scrollBy({ left: direction === 'left' ? -400 : 400, behavior: 'smooth' });
  }
}
