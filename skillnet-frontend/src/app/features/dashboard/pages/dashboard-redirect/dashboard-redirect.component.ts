import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import {
  dashboardPathForRole,
  resolveUserRole,
} from '../../../../shared/utils/user-role.util';

@Component({
  selector: 'app-dashboard-redirect',
  standalone: true,
  template: '',
})
export class DashboardRedirectComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  ngOnInit(): void {
    const role = resolveUserRole(
      this.authService.getCurrentUser(),
      this.authService.getToken(),
    );
    void this.router.navigateByUrl(dashboardPathForRole(role), { replaceUrl: true });
  }
}
