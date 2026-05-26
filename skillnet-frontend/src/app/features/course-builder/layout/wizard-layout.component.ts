import { Component, computed, inject } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';
import { CourseBuilderToolbarComponent } from '../components/course-builder-toolbar/course-builder-toolbar.component';
import {
  builderPreviousRoute,
  builderProgressPercent,
  builderStepLabel,
  extractBuilderStepFromUrl,
} from '../data/builder-steps.data';

@Component({
  selector: 'app-wizard-layout',
  standalone: true,
  imports: [RouterOutlet, CourseBuilderToolbarComponent],
  templateUrl: './wizard-layout.component.html',
  styles: [
    `
      :host {
        display: flex;
        flex: 1 1 auto;
        flex-direction: column;
        min-height: 0;
        min-width: 0;
      }
    `,
  ],
})
export class WizardLayoutComponent {
  private readonly router = inject(Router);

  private readonly currentPath = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.extractStepPath()),
      startWith(this.extractStepPath()),
    ),
    { initialValue: 'type' },
  );

  readonly showToolbar = computed(() => this.currentPath() !== 'type');

  readonly progressPercent = computed(() => builderProgressPercent(this.currentPath()));

  readonly currentStepLabel = computed(() => builderStepLabel(this.currentPath()));

  readonly previousRoute = computed(() => builderPreviousRoute(this.currentPath()));

  private extractStepPath(): string {
    return extractBuilderStepFromUrl(this.router.url) ?? 'type';
  }
}
