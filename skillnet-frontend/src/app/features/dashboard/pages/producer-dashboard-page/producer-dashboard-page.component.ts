import { Component, inject } from '@angular/core';
import { AuthService } from '../../../../core/services/auth.service';
import { ProducerDashboardComponent } from '../../components/producer-dashboard/producer-dashboard.component';

@Component({
  selector: 'app-producer-dashboard-page',
  standalone: true,
  imports: [ProducerDashboardComponent],
  template: `@if (authService.currentUser(); as user) {
    <app-producer-dashboard [user]="user" />
  }`,
})
export class ProducerDashboardPageComponent {
  readonly authService = inject(AuthService);
}
