import { Component, computed, input, output, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { User } from '../../../shared/models/auth.model';

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
  readonly user = input.required<User>();
  readonly isStudent = input(false);
  readonly isInfoproductor = input(false);
  readonly isAdmin = input(false);

  readonly logout = output<void>();

  /** Comprimido al cargar; se expande solo con hover. */
  readonly expanded = signal(false);

  readonly navItems = computed<SidebarNavItem[]>(() => {
    if (this.isStudent()) {
      return [
        { label: 'Panel de Control', icon: 'ri-dashboard-line', route: '/dashboard' },
        { label: 'Mis Cursos', icon: 'ri-book-open-line', href: '#' },
        { label: 'Explorar Catálogo', icon: 'ri-compass-3-line', href: '#' },
        { label: 'Certificados', icon: 'ri-award-line', href: '#' },
      ];
    }
    if (this.isInfoproductor()) {
      return [
        { label: 'Mis Cursos', icon: 'ri-book-open-line', route: '/courses' },
        { label: 'Crear Curso', icon: 'ri-add-circle-line', route: '/courses/new' },
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
    return [{ label: 'Inicio', icon: 'ri-home-line', route: '/dashboard' }];
  });

  onMouseEnter(): void {
    this.expanded.set(true);
  }

  onMouseLeave(): void {
    this.expanded.set(false);
  }

  onLogout(): void {
    this.logout.emit();
  }
}
