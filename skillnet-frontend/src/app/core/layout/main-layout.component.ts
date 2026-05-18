import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../services/auth.service';
import { User } from '../../shared/models/auth.model';
import { isStudentRole, resolveUserRole } from '../../shared/utils/user-role.util';
import { DashboardNavbarComponent } from './dashboard-navbar/dashboard-navbar.component';
import { DashboardSidebarComponent } from './dashboard-sidebar/dashboard-sidebar.component';

const MARKETPLACE_PREFIXES = ['/marketplace', '/catalog'];

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, AsyncPipe, DashboardNavbarComponent, DashboardSidebarComponent],
  templateUrl: './main-layout.component.html',
})
export class MainLayoutComponent {
  readonly authService = inject(AuthService);
  readonly currentUser$ = this.authService.currentUser$;

  private readonly router = inject(Router);

  readonly hideSidebar = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.isMarketplaceRoute()),
      startWith(this.isMarketplaceRoute()),
    ),
    { initialValue: false },
  );

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }

  displayName(user: User): string {
    const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
    return fullName || user.username;
  }

  avatarSrc(user: User): string {
    return user.profilePicture ?? 'assets/images/avatar-placeholder.png';
  }

  isStudent(user: User): boolean {
    return isStudentRole(user, this.authService.getToken());
  }

  isInfoproductor(user: User): boolean {
    return resolveUserRole(user, this.authService.getToken()) === 'infoproductor';
  }

  isAdmin(user: User): boolean {
    return resolveUserRole(user, this.authService.getToken()) === 'admin';
  }

  private isMarketplaceRoute(): boolean {
    const path = this.router.url.split('?')[0];
    return MARKETPLACE_PREFIXES.some((p) => path === p || path.startsWith(p + '/'));
  }
}
