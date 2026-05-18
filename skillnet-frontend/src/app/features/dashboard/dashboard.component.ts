import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { User } from '../../shared/models/auth.model';
import { isStudentRole } from '../../shared/utils/user-role.util';
import { StudentDashboardComponent } from './components/student-dashboard/student-dashboard.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [AsyncPipe, StudentDashboardComponent],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent {
  readonly authService = inject(AuthService);
  readonly currentUser$ = this.authService.currentUser$;

  isStudent(user: User): boolean {
    return isStudentRole(user, this.authService.getToken());
  }
}
