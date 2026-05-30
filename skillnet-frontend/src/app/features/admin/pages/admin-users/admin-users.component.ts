import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { AdminService } from '../../../../core/services/admin.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

interface AdminUserRow {
  id: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role?: string;
  activeRole?: string;
  active?: boolean;
  student?: boolean;
  infoproductor?: boolean;
  superUser?: boolean;
  dateJoined?: string;
}

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss',
})
export class AdminUsersComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly users = signal<AdminUserRow[]>([]);

  ngOnInit(): void {
    this.adminService.getUsers().subscribe({
      next: (rows) => {
        this.users.set(rows as AdminUserRow[]);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudieron cargar los usuarios.'));
        this.loading.set(false);
      },
    });
  }

  displayName(user: AdminUserRow): string {
    const full = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
    return full || user.username;
  }

  roleLabel(user: AdminUserRow): string {
    if (user.superUser) return 'Super Admin';
    return user.role ?? user.activeRole ?? '—';
  }
}
