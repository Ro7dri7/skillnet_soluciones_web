import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
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

const ROLE_OPTIONS = ['student', 'infoproductor', 'admin'] as const;

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss',
})
export class AdminUsersComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly roleOptions = ROLE_OPTIONS;
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly users = signal<AdminUserRow[]>([]);
  readonly savingUserId = signal<number | null>(null);
  readonly roleDrafts = signal<Record<number, string>>({});

  ngOnInit(): void {
    this.adminService.getUsers().subscribe({
      next: (rows) => {
        const list = rows as AdminUserRow[];
        this.users.set(list);
        const drafts: Record<number, string> = {};
        for (const user of list) {
          drafts[user.id] = user.role ?? user.activeRole ?? 'student';
        }
        this.roleDrafts.set(drafts);
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

  roleDraft(userId: number): string {
    return this.roleDrafts()[userId] ?? 'student';
  }

  setRoleDraft(userId: number, role: string): void {
    this.roleDrafts.update((drafts) => ({ ...drafts, [userId]: role }));
  }

  saveRole(user: AdminUserRow): void {
    const role = this.roleDraft(user.id);
    this.savingUserId.set(user.id);
    this.error.set(null);
    this.adminService.updateUserRole(user.id, role).subscribe({
      next: () => {
        this.users.update((list) =>
          list.map((row) => (row.id === user.id ? { ...row, role, activeRole: role } : row)),
        );
        this.savingUserId.set(null);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo actualizar el rol.'));
        this.savingUserId.set(null);
      },
    });
  }
}
