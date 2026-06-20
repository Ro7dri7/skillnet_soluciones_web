import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../services/auth.service';
import { User } from '../../../shared/models/auth.model';
import {
  canSwitchDashboardRole,
  dashboardPathForRole,
  isAdminAccount,
} from '../../../shared/utils/user-role.util';
import { HttpErrorResponse } from '@angular/common/http';

export interface SidebarNavItem {
  id: string;
  label: string;
  route?: string;
  href?: string;
}

export interface SidebarSection {
  id: string;
  label: string;
  icon: string;
  items: SidebarNavItem[];
}

@Component({
  selector: 'app-dashboard-sidebar',
  standalone: true,
  imports: [RouterLink],
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

  readonly expanded = signal(false);
  readonly mobileOpenSection = signal<string | null>(null);
  readonly openSections = signal<Record<string, boolean>>({
    cursos: true,
    metrics: false,
    aprendizaje: true,
    perfil: false,
  });

  readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(() => this.router.url),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );

  readonly activeRole = computed(() => this.authService.currentUser()?.role);

  readonly isAdminRoute = computed(() => {
    const path = this.currentUrl().split('?')[0];
    return path === '/admin' || path.startsWith('/admin/');
  });

  readonly showRoleSwitcher = computed(() =>
    canSwitchDashboardRole(this.authService.getCurrentUser()),
  );

  readonly roleSwitchError = signal<string | null>(null);

  readonly roleLabel = computed(() => {
    const role = this.activeRole();
    if (role === 'admin' || this.isAdminRoute()) return 'Administrador';
    return role === 'infoproductor' ? 'Infoproductor' : 'Estudiante';
  });

  readonly isAdminUser = computed(() =>
    isAdminAccount(this.authService.getCurrentUser(), this.authService.getToken()),
  );

  readonly adminNavItems: SidebarNavItem[] = [
    { id: 'admin-panel', label: 'Panel Admin', route: '/admin' },
    { id: 'admin-users', label: 'Usuarios', route: '/admin/users' },
    { id: 'admin-courses', label: 'Cursos', route: '/admin/courses' },
    { id: 'admin-enrollments', label: 'Inscripciones', route: '/admin/enrollments' },
    { id: 'admin-services', label: 'Servicios', route: '/admin/service-offerings' },
    { id: 'admin-audit', label: 'Auditoría', route: '/admin/audit-log' },
    { id: 'admin-marketplace', label: 'Marketplace', route: '/marketplace' },
  ];

  readonly infoproductorSections: SidebarSection[] = [
    {
      id: 'cursos',
      label: 'Mis Infoproductos',
      icon: 'ri-store-2-line',
      items: [
        { id: 'new-course', label: 'Crear un nuevo Infoproducto', route: '/infoproductor/courses/new/type' },
        { id: 'all-courses', label: 'Ver todos mis Infoproductos', route: '/courses' },
      ],
    },
    {
      id: 'metrics',
      label: 'Métricas',
      icon: 'ri-bar-chart-box-line',
      items: [
        { id: 'metrics-hub', label: 'Progreso y exámenes', route: '/infoproductor/student-progress' },
        { id: 'traffic', label: 'Tráfico', route: '/infoproductor/traffic' },
        { id: 'plans', label: 'Planes IA', route: '/infoproductor/plans' },
      ],
    },
    {
      id: 'perfil',
      label: 'Mi perfil',
      icon: 'ri-user-line',
      items: [
        { id: 'profile', label: 'Detalles de la cuenta', route: '/profile' },
        { id: 'dashboard', label: 'Panel Infoproductor', route: '/dashboard/infoproductor' },
      ],
    },
  ];

  readonly studentSections: SidebarSection[] = [
    {
      id: 'aprendizaje',
      label: 'Aprendizaje',
      icon: 'ri-book-open-line',
      items: [
        { id: 'dashboard', label: 'Panel de Control', route: '/dashboard/estudiante' },
        { id: 'my-courses', label: 'Mis Infoproductos', route: '/mis-cursos' },
        { id: 'explore', label: 'Explorar Catálogo', route: '/marketplace' },
        { id: 'certificates', label: 'Certificados', route: '/certificates' },
      ],
    },
    {
      id: 'perfil',
      label: 'Mi perfil',
      icon: 'ri-user-line',
      items: [{ id: 'profile', label: 'Detalles de la cuenta', route: '/profile' }],
    },
  ];

  readonly sidebarSections = computed<SidebarSection[]>(() => {
    const role = this.authService.currentUser()?.role;
    if (this.isAdminRoute() || role === 'admin') {
      return [
        {
          id: 'sistema',
          label: 'Sistema',
          icon: 'ri-settings-4-line',
          items: this.adminNavItems,
        },
      ];
    }
    if (role === 'infoproductor') {
      return this.infoproductorSections;
    }
    if (role === 'student') {
      return this.studentSections;
    }
    return [];
  });

  readonly useSectionLayout = computed(() => {
    const role = this.authService.currentUser()?.role;
    return role === 'infoproductor' || role === 'student';
  });

  constructor() {
    effect(() => {
      if (this.forceExpanded()) {
        this.expanded.set(true);
      }
    });
  }

  toggleSection(sectionId: string): void {
    this.openSections.update((current) => ({
      ...current,
      [sectionId]: !current[sectionId],
    }));
  }

  toggleMobileSection(sectionId: string): void {
    this.mobileOpenSection.update((current) => (current === sectionId ? null : sectionId));
  }

  closeMobileMenu(): void {
    this.mobileOpenSection.set(null);
  }

  isRouteActive(route: string): boolean {
    const path = this.currentUrl().split('?')[0];
    if (route.includes('/dashboard/')) {
      return path === route;
    }
    return path === route || path.startsWith(route + '/');
  }

  isItemActive(item: SidebarNavItem): boolean {
    if (!item.route) {
      return false;
    }
    const path = this.currentUrl().split('?')[0];
    if (item.route === '/courses') {
      return path === '/courses';
    }
    if (item.route === '/infoproductor/student-progress') {
      return (
        path === '/infoproductor/student-progress' ||
        path === '/infoproductor/students' ||
        path.startsWith('/infoproductor/quiz-review')
      );
    }
    return this.isRouteActive(item.route);
  }

  isSectionActive(section: SidebarSection): boolean {
    return section.items.some((item) => this.isItemActive(item));
  }

  isSectionOpen(sectionId: string): boolean {
    return this.openSections()[sectionId] ?? false;
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
    this.closeMobileMenu();

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
    if (active === 'infoproductor') {
      void this.router.navigateByUrl('/courses');
      return;
    }
    if (active === 'student') {
      void this.router.navigateByUrl('/mis-cursos');
      return;
    }
    const path = dashboardPathForRole(active);
    void this.router.navigateByUrl(path);
  }
}
