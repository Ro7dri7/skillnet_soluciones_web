import { Component, inject } from '@angular/core';
import { AuthService } from '../../../../core/services/auth.service';
import { StudentDashboardComponent } from '../../components/student-dashboard/student-dashboard.component';

@Component({
  selector: 'app-student-dashboard-page',
  standalone: true,
  imports: [StudentDashboardComponent],
  template: `@if (authService.currentUser(); as user) {
    <app-student-dashboard [user]="user" />
  }`,
})
export class StudentDashboardPageComponent {
  readonly authService = inject(AuthService);
}
