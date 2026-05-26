import { Component, inject, input } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-course-builder-toolbar',
  standalone: true,
  templateUrl: './course-builder-toolbar.component.html',
})
export class CourseBuilderToolbarComponent {
  private readonly router = inject(Router);

  readonly stepLabel = input('');
  readonly progressPercent = input(0);
  readonly backRoute = input('/courses');

  onBack(): void {
    void this.router.navigateByUrl(this.backRoute());
  }

  onSaveAndExit(): void {
    void this.router.navigate(['/courses']);
  }
}
