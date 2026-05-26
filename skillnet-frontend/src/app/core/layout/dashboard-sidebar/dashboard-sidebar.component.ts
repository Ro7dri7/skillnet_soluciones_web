import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { User } from '../../../shared/models/auth.model';
import { canSwitchDashboardRole, dashboardPathForRole } from '../../../shared/utils/user-role.util';
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

  readonly roleLabel = computed(() =>
    this.activeRole() === 'infoproductor' ? 'Infoproductor' : 'Estudiante',
  );

  readonly navItems = computed<SidebarNavItem[]>(() => {
    const role = this.authService.currentUser()?.role;
    if (role === 'student') {
      return [
        { label: 'Panel de Control', icon: 'ri-dashboard-line', route: '/dashboard/estudiante' },
        { label: 'Mis Cursos', icon: 'ri-book-open-line', href: '#' },
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
        { label: 'Mis Cursos', icon: 'ri-book-open-line', route: '/courses' },
        { label: 'Crear Curso', icon: 'ri-add-circle-line', route: '/infoproductor/courses/new/type' },
        { label: 'Mis Ventas', icon: 'ri-line-chart-line', href: '#' },
        { label: 'Alumnos', icon: 'ri-group-line', href: '#' },
      ];
    }
    if (this.isAdmin()) {
      return [
        { label: 'Panel Global', icon: 'ri-dashboard-line', href: '#' },
        { label: 'Usuarios', icon: 'ri-user-settings-line', href: '#' },
      ];
    }
    return [{ label: 'Inicio', icon: 'ri-home-line', route: '/dashboard/estudiante' }];
  });

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

  switchToRole(role: 'student' | 'infoproductor'): void {
    const user = this.authService.getCurrentUser();
    this.roleSwitchError.set(null);

    if (!user || user.role === role) {
      this.navigateAfterRoleSwitch();
      return;
    }

    this.authService.switchRole(role).subscribe({
      next: () => {
        this.roleSwitchError.set(null);
        this.navigateAfterRoleSwitch();
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

  navigateAfterRoleSwitch(): void {
    const path = dashboardPathForRole(this.authService.getCurrentUser()?.role);
    void this.router.navigateByUrl(path);
  }
}
