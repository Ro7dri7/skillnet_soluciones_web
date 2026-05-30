import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../services/auth.service';
import { User } from '../../shared/models/auth.model';
import { isAdminAccount, isStudentRole } from '../../shared/utils/user-role.util';
import { DashboardNavbarComponent } from './dashboard-navbar/dashboard-navbar.component';
import { DashboardSidebarComponent } from './dashboard-sidebar/dashboard-sidebar.component';
import { MarketplaceCategoryBarComponent } from '../../features/marketplace/components/marketplace-category-bar/marketplace-category-bar.component';
import { isWizardOnlyStep, isWizardStepWithoutMainSidebar } from '../../features/course-builder/data/builder-steps.data';

const FULL_WIDTH_PREFIXES = ['/marketplace', '/catalog', '/checkout'];
const CATEGORY_BAR_PREFIXES = ['/marketplace', '/catalog'];
const ADMIN_ROUTE = /^\/admin(\/|$)/;
const LEARN_ROUTE = /\/marketplace\/course\/[^/]+\/learn$/;

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    AsyncPipe,
    DashboardNavbarComponent,
    DashboardSidebarComponent,
    MarketplaceCategoryBarComponent,
  ],
  templateUrl: './main-layout.component.html',
})
export class MainLayoutComponent {
  readonly authService = inject(AuthService);
  readonly currentUser$ = this.authService.currentUser$;

  private readonly router = inject(Router);

  readonly isWizardRoute = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.isCourseWizardRoute()),
      startWith(this.isCourseWizardRoute()),
    ),
    { initialValue: false },
  );

  readonly hideMainSidebarOnWizard = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.isWizardWithoutMainSidebar()),
      startWith(this.isWizardWithoutMainSidebar()),
    ),
    { initialValue: false },
  );

  readonly isLearnRoute = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.isCourseLearnRoute()),
      startWith(this.isCourseLearnRoute()),
    ),
    { initialValue: false },
  );

  readonly isAdminRoute = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.isAdminPanelRoute()),
      startWith(this.isAdminPanelRoute()),
    ),
    { initialValue: false },
  );

  readonly hideSidebar = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.isFullWidthRoute()),
      startWith(this.isFullWidthRoute()),
    ),
    { initialValue: false },
  );

  readonly showCategoryBar = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.isCategoryBarRoute()),
      startWith(this.isCategoryBarRoute()),
    ),
    { initialValue: false },
  );

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login'], { replaceUrl: true });
  }

  displayName(user: User): string {
    const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
    return fullName || user.username;
  }

  avatarSrc(user: User): string {
    return user.profilePicture ?? 'assets/images/avatar-placeholder.png';
  }

  isStudent(user: User): boolean {
    return user.role === 'student' || isStudentRole(user, this.authService.getToken());
  }

  isInfoproductor(user: User): boolean {
    return user.role === 'infoproductor';
  }

  isAdmin(user: User): boolean {
    return isAdminAccount(user, this.authService.getToken()) || this.isAdminPanelRoute();
  }

  private isAdminPanelRoute(): boolean {
    return ADMIN_ROUTE.test(this.router.url.split('?')[0]);
  }

  private isWizardWithoutMainSidebar(): boolean {
    return isWizardStepWithoutMainSidebar(this.router.url.split('?')[0]);
  }

  private isCourseWizardRoute(): boolean {
    return isWizardOnlyStep(this.router.url.split('?')[0]);
  }

  private isFullWidthRoute(): boolean {
    const path = this.router.url.split('?')[0];
    if (this.isCourseLearnRoute()) {
      return true;
    }
    return FULL_WIDTH_PREFIXES.some((p) => path === p || path.startsWith(p + '/'));
  }

  private isCourseLearnRoute(): boolean {
    return LEARN_ROUTE.test(this.router.url.split('?')[0]);
  }

  private isCategoryBarRoute(): boolean {
    const path = this.router.url.split('?')[0];
    if (this.isCourseLearnRoute() || this.isAdminPanelRoute()) {
      return false;
    }
    return CATEGORY_BAR_PREFIXES.some((p) => path === p || path.startsWith(p + '/'));
  }
}
