import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../services/auth.service';
import { User } from '../../../shared/models/auth.model';
import { canSwitchDashboardRole, dashboardPathForRole, isAdminAccount } from '../../../shared/utils/user-role.util';
import { HttpErrorResponse } from '@angular/common/http';
export interface SidebarNavItem {
  label: string;
  icon: string;
  route?: string;
  href?: string;
}

@Component({
  selector: 'app-dashboard-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './dashboard-sidebar.component.html',
})
export class DashboardSidebarComponent {
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly user = input.required<User>();
  readonly isStudent = input(false);
  readonly isInfoproductor = input(false);
  readonly isAdmin = input(false);

  readonly logout = output<void>();

  readonly forceExpanded = input(false);

  /** Comprimido al cargar; se expande solo con hover (salvo forceExpanded). */
  readonly expanded = signal(false);

  readonly activeRole = computed(() => this.authService.currentUser()?.role);

  readonly isAdminRoute = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.onAdminRoute()),
      startWith(this.onAdminRoute()),
    ),
    { initialValue: false },
  );

  readonly showRoleSwitcher = computed(() =>
    canSwitchDashboardRole(this.authService.getCurrentUser()),
  );

  readonly roleSwitchError = signal<string | null>(null);

  constructor() {
    effect(() => {
      if (this.forceExpanded()) {
        this.expanded.set(true);
      }
    });
  }

  readonly roleLabel = computed(() => {
    const role = this.activeRole();
    if (role === 'admin' || this.isAdminRoute()) return 'Administrador';
    return role === 'infoproductor' ? 'Infoproductor' : 'Estudiante';
  });

  readonly isAdminUser = computed(() =>
    isAdminAccount(this.authService.getCurrentUser(), this.authService.getToken()),
  );

  readonly adminNavItems: SidebarNavItem[] = [
    { label: 'Panel Admin', icon: 'ri-shield-star-line', route: '/admin' },
    { label: 'Usuarios', icon: 'ri-user-settings-line', route: '/admin/users' },
    { label: 'Cursos', icon: 'ri-book-2-line', route: '/admin/courses' },
    { label: 'Ventas', icon: 'ri-coin-line', href: '#' },
    { label: 'Reportes', icon: 'ri-bar-chart-box-line', href: '#' },
    { label: 'Marketplace', icon: 'ri-store-2-line', route: '/marketplace' },
  ];

  readonly navItems = computed<SidebarNavItem[]>(() => {
    const role = this.authService.currentUser()?.role;
    if (this.isAdminRoute() || role === 'admin') {
      return this.adminNavItems;
    }
    if (role === 'student') {
      return [
        { label: 'Panel de Control', icon: 'ri-dashboard-line', route: '/dashboard/estudiante' },
        { label: 'Mis Cursos', icon: 'ri-book-open-line', route: '/mis-cursos' },
        { label: 'Explorar Catálogo', icon: 'ri-compass-3-line', route: '/marketplace' },
        { label: 'Certificados', icon: 'ri-award-line', href: '#' },
      ];
    }
    if (role === 'infoproductor') {
      return [
        {
          label: 'Panel Infoproductor',
          icon: 'ri-dashboard-line',
          route: '/dashboard/infoproductor',
        },
        { label: 'Mis Infoproductos', icon: 'ri-book-open-line', route: '/courses' },
        { label: 'Crear Curso', icon: 'ri-add-circle-line', route: '/infoproductor/courses/new/type' },
        { label: 'Mis Ventas', icon: 'ri-line-chart-line', href: '#' },
        { label: 'Alumnos', icon: 'ri-group-line', href: '#' },
      ];
    }
    return [{ label: 'Inicio', icon: 'ri-home-line', route: '/dashboard/estudiante' }];
  });

  private onAdminRoute(): boolean {
    const path = this.router.url.split('?')[0];
    return path === '/admin' || path.startsWith('/admin/');
  }

  onMouseEnter(): void {
    this.expanded.set(true);
  }

  onMouseLeave(): void {
    if (!this.forceExpanded()) {
      this.expanded.set(false);
    }
  }

  onLogout(): void {
    this.logout.emit();
  }

  switchToRole(role: 'student' | 'infoproductor' | 'admin'): void {
    const user = this.authService.getCurrentUser();
    this.roleSwitchError.set(null);

    if (!user || user.role === role) {
      this.navigateAfterRoleSwitch(role);
      return;
    }

    this.authService.switchRole(role).subscribe({
      next: () => {
        this.roleSwitchError.set(null);
        this.navigateAfterRoleSwitch(role);
      },
      error: (err: unknown) => {
        const message =
          err instanceof HttpErrorResponse && err.error?.message
            ? String(err.error.message)
            : 'No se pudo cambiar de rol. Cierra sesión e inicia de nuevo.';
        this.roleSwitchError.set(message);
      },
    });
  }

  navigateAfterRoleSwitch(role?: 'student' | 'infoproductor' | 'admin'): void {
    const active = role ?? this.authService.getCurrentUser()?.role;
    const path = dashboardPathForRole(active);
    void this.router.navigateByUrl(path);
  }
}
