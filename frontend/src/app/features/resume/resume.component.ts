import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { DataService } from '../../core/data.service';
import { CvOrder, CvStatus } from '../../core/models';

declare const Razorpay: any;

@Component({
  selector: 'app-resume',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatProgressBarModule, MatSnackBarModule],
  templateUrl: './resume.component.html',
  styleUrl: './resume.component.scss',
})
export class ResumeComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);
  private router = inject(Router);

  readonly price = 90;
  status = signal<CvStatus | null>(null);
  busy = signal(false);

  constructor() {
    this.loadStatus();
  }

  private loadStatus(): void {
    this.data.cvStatus().subscribe({
      next: (s) => this.status.set(s),
      error: (e) => this.fail(e),
    });
  }

  goToField(tab: number): void {
    this.router.navigate(['/app/students'], { queryParams: { tab } });
  }

  /** Complete → not paid: create the order and take payment; paid: download straight away. */
  payAndDownload(): void {
    const st = this.status();
    if (!st) return;
    if (st.paid) { this.download(); return; }
    this.busy.set(true);
    this.data.cvOrder().subscribe({
      next: (order: CvOrder) => {
        if (order.alreadyPaid) { this.afterPaid(); return; }
        if (order.stub) {
          // No Razorpay keys yet → confirm the (test) payment directly.
          this.confirm(order.orderId!, 'stub_payment', 'stub_signature');
        } else {
          this.openRazorpay(order);
        }
      },
      error: (e) => this.fail(e),
    });
  }

  private openRazorpay(order: CvOrder): void {
    const rzp = new Razorpay({
      key: order.keyId,
      order_id: order.orderId,
      amount: order.amountPaise,
      currency: order.currency,
      name: 'MSYEP',
      description: 'Student CV Generation',
      handler: (res: any) =>
        this.confirm(res.razorpay_order_id, res.razorpay_payment_id, res.razorpay_signature),
      modal: { ondismiss: () => this.busy.set(false) },
    });
    rzp.open();
  }

  private confirm(orderId: string, paymentId: string, signature: string): void {
    this.data.cvVerify({ orderId, paymentId, signature }).subscribe({
      next: () => this.afterPaid(),
      error: (e) => this.fail(e),
    });
  }

  private afterPaid(): void {
    this.snack.open('Payment successful — downloading your CV.', 'OK', { duration: 3000 });
    this.status.update((s) => (s ? { ...s, paid: true } : s));
    this.download();
  }

  download(): void {
    this.busy.set(true);
    this.data.cvDownload().subscribe({
      next: (blob) => {
        this.busy.set(false);
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'MSYEP-Resume.pdf';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (e) => this.fail(e),
    });
  }

  private fail(e: any): void {
    this.busy.set(false);
    this.snack.open(e?.error?.message || 'Something went wrong', 'OK', { duration: 3500 });
  }
}
