import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { interval, switchMap, takeWhile, tap, catchError, of, startWith } from 'rxjs';
import { PaymentService, PaymentStatus } from '../../../../core/services/payment.service';
import { OwnedCoursesService } from '../../../../core/services/owned-courses.service';
import { messageFromHttpError } from '../../../../shared/utils/http-error.util';

@Component({
  selector: 'app-payment-success',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './payment-success.component.html',
  styleUrl: './payment-success.component.scss',
})
export class PaymentSuccessComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly paymentService = inject(PaymentService);
  private readonly ownedCourses = inject(OwnedCoursesService);
  private readonly destroyRef = inject(DestroyRef);

  readonly paymentId = signal<number | null>(null);
  readonly receipt = signal<PaymentStatus | null>(null);
  readonly polling = signal(false);
  readonly error = signal<string | null>(null);
  readonly genericSuccess = signal(false);

  ngOnInit(): void {
    const raw = this.route.snapshot.queryParamMap.get('paymentId');
    if (!raw) {
      this.genericSuccess.set(true);
      return;
    }
    const id = Number(raw);
    if (!Number.isFinite(id)) {
      this.genericSuccess.set(true);
      return;
    }
    this.paymentId.set(id);
    this.polling.set(true);
    interval(2000)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.paymentService.paymentStatus(id).pipe(
            catchError((err) => {
              this.error.set(messageFromHttpError(err, 'No se pudo verificar el pago.'));
              return of(null);
            }),
          ),
        ),
        tap((result) => {
          if (result) {
            this.receipt.set(result);
            const status = (result.status ?? '').toLowerCase();
            if (['completed', 'succeeded', 'paid', 'failed', 'cancelled'].includes(status)) {
              this.polling.set(false);
              if (['completed', 'succeeded', 'paid'].includes(status)) {
                this.ownedCourses.refresh();
              }
            }
          }
        }),
        takeWhile(
          (result) =>
            result == null ||
            !['completed', 'succeeded', 'paid', 'failed', 'cancelled'].includes(
              (result.status ?? '').toLowerCase(),
            ),
          true,
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        complete: () => this.polling.set(false),
      });
  }

  isSuccessStatus(): boolean {
    const s = (this.receipt()?.status ?? '').toLowerCase();
    return ['completed', 'succeeded', 'paid', 'success'].includes(s);
  }

  receiptItems(receipt: PaymentStatus) {
    if (receipt.items?.length) {
      return receipt.items;
    }
    if (receipt.courseTitle) {
      return [{ courseTitle: receipt.courseTitle, courseId: receipt.courseId }];
    }
    return [];
  }
}
