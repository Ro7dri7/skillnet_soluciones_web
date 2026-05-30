import { AsyncPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { User } from '../../../shared/models/auth.model';
import { dashboardPathForRole, isAdminAccount } from '../../../shared/utils/user-role.util';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AsyncPipe],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss',
})
export class AdminLayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser$ = this.authService.currentUser$;

  readonly navItems = [
    { label: 'Dashboard', icon: 'ri-dashboard-line', route: '/admin' },
    { label: 'Usuarios', icon: 'ri-user-settings-line', route: '/admin/users' },
    { label: 'Cursos', icon: 'ri-book-2-line', route: '/admin/courses' },
  ];

  displayName(user: User): string {
    const full = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
    return full || user.username || 'Admin';
  }

  initials(user: User): string {
    const name = this.displayName(user);
    const parts = name.split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

  switchToStudent(): void {
    this.switchRole('student');
  }

  switchToInfoproductor(): void {
    this.switchRole('infoproductor');
  }

  private switchRole(role: 'student' | 'infoproductor'): void {
    this.authService.switchRole(role).subscribe({
      next: () => void this.router.navigateByUrl(dashboardPathForRole(role)),
    });
  }
}
