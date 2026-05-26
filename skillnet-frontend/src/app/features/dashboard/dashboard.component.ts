import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { ProducerDashboardComponent } from './components/producer-dashboard/producer-dashboard.component';
import { StudentDashboardComponent } from './components/student-dashboard/student-dashboard.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [StudentDashboardComponent, ProducerDashboardComponent],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent {
  readonly authService = inject(AuthService);
}
