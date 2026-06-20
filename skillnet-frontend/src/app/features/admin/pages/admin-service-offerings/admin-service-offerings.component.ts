import { CurrencyPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  AdminService,
  ServiceOffering,
  ServiceOfferingPayload,
} from '../../../../core/services/admin.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

@Component({
  selector: 'app-admin-service-offerings',
  standalone: true,
  imports: [ReactiveFormsModule, CurrencyPipe],
  templateUrl: './admin-service-offerings.component.html',
  styleUrl: './admin-service-offerings.component.scss',
})
export class AdminServiceOfferingsComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly offerings = signal<ServiceOffering[]>([]);
  readonly editingId = signal<number | null>(null);

  readonly form = this.fb.nonNullable.group({
    section: ['', Validators.required],
    title: ['', Validators.required],
    description: [''],
    priceUsd: [0, [Validators.required, Validators.min(0)]],
    active: [true],
    featured: [false],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.adminService.getServiceOfferings().subscribe({
      next: (rows) => {
        this.offerings.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudieron cargar los servicios.'));
        this.loading.set(false);
      },
    });
  }

  startEdit(row: ServiceOffering): void {
    this.editingId.set(row.id);
    this.form.patchValue({
      section: row.section,
      title: row.title,
      description: row.description ?? '',
      priceUsd: row.priceUsd,
      active: row.active !== false,
      featured: row.featured === true,
    });
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.form.reset({ section: '', title: '', description: '', priceUsd: 0, active: true, featured: false });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload = this.form.getRawValue() as ServiceOfferingPayload;
    const id = this.editingId();
    this.saving.set(true);
    const request$ =
      id != null
        ? this.adminService.updateServiceOffering(id, payload)
        : this.adminService.createServiceOffering(payload);

    request$.subscribe({
      next: () => {
        this.cancelEdit();
        this.load();
        this.saving.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudo guardar el servicio.'));
        this.saving.set(false);
      },
    });
  }

  delete(id: number): void {
    if (!confirm('¿Eliminar este servicio?')) {
      return;
    }
    this.adminService.deleteServiceOffering(id).subscribe({
      next: () => this.load(),
      error: (err) => this.error.set(messageFromHttpError(err, 'No se pudo eliminar.')),
    });
  }
}
