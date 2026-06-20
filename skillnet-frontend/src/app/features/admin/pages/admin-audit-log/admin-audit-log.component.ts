import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import {
  AuditLogEntry,
  AuditLogFilters,
  AuditService,
} from '../../../../core/services/audit.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

const AUDIT_ACTIONS = [
  { value: '', label: 'Todas las acciones' },
  { value: 'REGISTER_USER', label: 'Registro de cuenta' },
  { value: 'UPDATE_PROFILE', label: 'Actualizar perfil' },
  { value: 'CHANGE_PASSWORD', label: 'Cambiar contraseña' },
  { value: 'PASSWORD_RESET', label: 'Restablecer contraseña' },
  { value: 'SWITCH_ROLE', label: 'Cambiar rol activo' },
  { value: 'CREATE_COURSE', label: 'Crear curso / producto' },
  { value: 'UPDATE_COURSE', label: 'Editar curso' },
  { value: 'DELETE_COURSE', label: 'Eliminar curso' },
  { value: 'PUBLISH_COURSE', label: 'Publicar curso' },
  { value: 'UNPUBLISH_COURSE', label: 'Ocultar curso' },
  { value: 'SET_DRAFT', label: 'Pasar a borrador' },
  { value: 'TAKEDOWN_COURSE', label: 'Dar de baja (admin)' },
  { value: 'PURCHASE_COURSE', label: 'Compra de producto' },
  { value: 'PURCHASE_PLAN', label: 'Compra de plan IA' },
  { value: 'ADMIN_ENROLL_USER', label: 'Matrícula admin' },
  { value: 'ADMIN_CHANGE_USER_ROLE', label: 'Cambio rol admin' },
  { value: 'DELETE_USER', label: 'Eliminar cuenta' },
] as const;

@Component({
  selector: 'app-admin-audit-log',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule],
  templateUrl: './admin-audit-log.component.html',
  styleUrl: './admin-audit-log.component.scss',
})
export class AdminAuditLogComponent implements OnInit {
  private readonly auditService = inject(AuditService);
  private readonly fb = inject(FormBuilder);

  readonly actionOptions = AUDIT_ACTIONS;
  readonly loading = signal(true);
  readonly exporting = signal(false);
  readonly error = signal<string | null>(null);
  readonly entries = signal<AuditLogEntry[]>([]);
  readonly totalElements = signal(0);
  readonly page = signal(0);
  readonly pageSize = 50;

  readonly filterForm = this.fb.nonNullable.group({
    email: [''],
    action: [''],
    startDate: [''],
    endDate: [''],
  });

  private activeFilters: AuditLogFilters = {};

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(page = 0): void {
    this.loading.set(true);
    this.error.set(null);
    this.page.set(page);

    this.auditService.getAuditLogs(page, this.pageSize, this.activeFilters).subscribe({
      next: (response) => {
        this.entries.set(response.content ?? []);
        this.totalElements.set(response.totalElements ?? 0);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo cargar el reporte de auditoría.'));
        this.loading.set(false);
      },
    });
  }

  applyFilters(): void {
    const { email, action, startDate, endDate } = this.filterForm.getRawValue();
    this.activeFilters = {
      email: email.trim() || undefined,
      action: action.trim() || undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
    };
    this.loadLogs(0);
  }

  clearFilters(): void {
    this.filterForm.reset({
      email: '',
      action: '',
      startDate: '',
      endDate: '',
    });
    this.activeFilters = {};
    this.loadLogs(0);
  }

  downloadCsv(): void {
    this.exporting.set(true);
    this.auditService.exportAuditLogsToCsv(this.activeFilters).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = 'audit-logs.csv';
        anchor.click();
        window.URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo exportar el CSV de auditoría.'));
        this.exporting.set(false);
      },
    });
  }

  actionLabel(action: string): string {
    const match = AUDIT_ACTIONS.find((item) => item.value === action);
    return match?.label ?? action;
  }

  userLabel(entry: AuditLogEntry): string {
    if (entry.userDisplayName && entry.userEmail) {
      return `${entry.userDisplayName} (${entry.userEmail})`;
    }
    return entry.userEmail || entry.userDisplayName || '—';
  }

  entityLabel(entry: AuditLogEntry): string {
    const id = entry.entityId != null ? ` #${entry.entityId}` : '';
    return `${entry.entityName}${id}`;
  }

  hasPreviousPage(): boolean {
    return this.page() > 0;
  }

  hasNextPage(): boolean {
    return (this.page() + 1) * this.pageSize < this.totalElements();
  }

  previousPage(): void {
    if (this.hasPreviousPage()) {
      this.loadLogs(this.page() - 1);
    }
  }

  nextPage(): void {
    if (this.hasNextPage()) {
      this.loadLogs(this.page() + 1);
    }
  }
}
