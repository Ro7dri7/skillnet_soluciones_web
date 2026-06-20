import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CertificateItem, CertificateService } from '../../../../core/services/certificate.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

@Component({
  selector: 'app-certificates',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './certificates.component.html',
  styleUrl: './certificates.component.scss',
})
export class CertificatesComponent implements OnInit {
  private readonly certificateService = inject(CertificateService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly certificates = signal<CertificateItem[]>([]);
  readonly total = signal(0);

  ngOnInit(): void {
    this.certificateService.overview().subscribe({
      next: (data) => {
        this.certificates.set(data.certificates ?? []);
        this.total.set(data.totalCertificates ?? 0);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(messageFromHttpError(err, 'No se pudieron cargar los certificados.'));
        this.loading.set(false);
      },
    });
  }
}
