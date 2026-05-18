import {
  Component,
  ElementRef,
  HostListener,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';

interface NavNotification {
  id: number;
  title: string;
  message: string;
  isRead: boolean;
}

@Component({
  selector: 'app-dashboard-navbar',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard-navbar.component.html',
})
export class DashboardNavbarComponent {
  readonly displayName = input.required<string>();
  readonly avatarSrc = input.required<string>();
  readonly isInfoproductor = input(false);

  readonly logout = output<void>();

  private readonly hostRef = inject(ElementRef<HTMLElement>);

  readonly notificationsPanelRef = viewChild<ElementRef<HTMLElement>>('notificationsPanel');
  readonly profilePanelRef = viewChild<ElementRef<HTMLElement>>('profilePanel');
  readonly categoriesPanelRef = viewChild<ElementRef<HTMLElement>>('categoriesPanel');

  readonly showNotifications = signal(false);
  readonly showProfile = signal(false);
  readonly showCategories = signal(false);

  readonly cartCount = signal(2);
  readonly wishlistCount = signal(1);
  readonly unreadCount = signal(2);

  readonly categories = ['Tecnología', 'Finanzas', 'Marketing', 'Diseño', 'Negocios'];

  readonly notifications: NavNotification[] = [
    {
      id: 1,
      title: 'Nuevo módulo disponible',
      message: 'Maestría en Ciencia de Datos — Módulo 3',
      isRead: false,
    },
    {
      id: 2,
      title: 'Certificado listo',
      message: 'Descarga tu certificado de Python',
      isRead: false,
    },
    {
      id: 3,
      title: 'Recordatorio',
      message: 'Continúa tu racha de aprendizaje hoy',
      isRead: true,
    },
  ];

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as Node;
    if (!this.hostRef.nativeElement.contains(target)) {
      this.closeAllPanels();
    }
  }

  toggleNotifications(event: MouseEvent): void {
    event.stopPropagation();
    const next = !this.showNotifications();
    this.showNotifications.set(next);
    this.showProfile.set(false);
    this.showCategories.set(false);
  }

  toggleProfile(event: MouseEvent): void {
    event.stopPropagation();
    const next = !this.showProfile();
    this.showProfile.set(next);
    this.showNotifications.set(false);
    this.showCategories.set(false);
  }

  toggleCategories(event: MouseEvent): void {
    event.stopPropagation();
    const next = !this.showCategories();
    this.showCategories.set(next);
    this.showNotifications.set(false);
    this.showProfile.set(false);
  }

  markAllNotificationsRead(): void {
    this.notifications.forEach((n) => (n.isRead = true));
    this.unreadCount.set(0);
  }

  onLogout(): void {
    this.closeAllPanels();
    this.logout.emit();
  }

  iconBtnClass(): string {
    return 'relative flex h-9 w-[42px] cursor-pointer items-center justify-center rounded-[10px] border border-white/60 text-white transition-colors hover:bg-white/10';
  }

  badgeClass(): string {
    return 'absolute -right-2 -top-2 flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-skillnet-sky px-1 text-[10px] font-bold text-skillnet-dark';
  }

  private closeAllPanels(): void {
    this.showNotifications.set(false);
    this.showProfile.set(false);
    this.showCategories.set(false);
  }
}
