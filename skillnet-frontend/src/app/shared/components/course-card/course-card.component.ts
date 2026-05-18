import { CurrencyPipe } from '@angular/common';
import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-course-card',
  standalone: true,
  imports: [CurrencyPipe],
  templateUrl: './course-card.component.html',
})
export class CourseCardComponent {
  readonly title = input.required<string>();
  readonly price = input.required<number>();
  readonly imageUrl = input<string | null>(null);
  readonly category = input<string | null>(null);

  readonly buy = output<void>();

  onBuyClick(event: MouseEvent): void {
    event.stopPropagation();
    this.buy.emit();
  }

  onCardClick(): void {
    this.buy.emit();
  }
}
