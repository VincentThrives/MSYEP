import { Component, ElementRef, OnDestroy, ViewChild, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { DataService } from '../../core/data.service';
import { AuthService } from '../../core/auth.service';
import { EntranceResult, EntranceStart, Student } from '../../core/models';
import { SearchSelectComponent } from '../../shared/search-select.component';

@Component({
  selector: 'app-entrance-test',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatButtonModule, MatIconModule, MatCardModule,
    MatRadioModule, MatProgressBarModule, MatSnackBarModule, SearchSelectComponent,
  ],
  templateUrl: './entrance-test.component.html',
  styleUrl: './entrance-test.component.scss',
})
export class EntranceTestComponent implements OnDestroy {
  private data = inject(DataService);
  private auth = inject(AuthService);
  private snack = inject(MatSnackBar);

  @ViewChild('video') video?: ElementRef<HTMLVideoElement>;
  @ViewChild('canvas') canvas?: ElementRef<HTMLCanvasElement>;

  state = signal<'setup' | 'test' | 'result'>('setup');
  students = signal<Student[]>([]);
  studentId?: string;
  exam = signal<EntranceStart | null>(null);
  result = signal<EntranceResult | null>(null);
  /** True when the shown result is a previously-completed attempt (test is one-time only). */
  alreadyTaken = signal(false);
  answers: Record<string, string> = {};

  cameraOn = signal(false);
  selfiePreview = signal<string | null>(null);
  selfieReady = signal(false);
  camError = signal<string | null>(null);
  busy = signal(false);
  remaining = signal(0);

  private stream?: MediaStream;
  private timerId?: any;
  private selfieFile?: File;

  lockedStudent = computed(() => this.auth.role() === 'STUDENT' && !!this.auth.user()?.studentId);

  // Plain methods (not computed): they read non-signal fields (studentId, selfieFile, answers),
  // so they must be re-evaluated on every change-detection pass, not memoized by a computed.
  answeredCount(): number {
    return Object.values(this.answers).filter(Boolean).length;
  }
  canStart(): boolean {
    // selfieReady() is a signal, so the button re-evaluates as soon as the selfie blob is ready.
    return !!this.studentId && this.selfieReady();
  }

  constructor() {
    this.data.students().subscribe((s) => this.students.set(s));
    if (this.lockedStudent()) {
      this.studentId = this.auth.user()!.studentId;
      this.checkExisting();
    }
  }

  /** One attempt only: if this student already took the test, show their result and block a retake. */
  private checkExisting(): void {
    if (!this.studentId) return;
    this.data.entranceResult(this.studentId).subscribe((r) => {
      if (r) {
        this.result.set(r);
        this.alreadyTaken.set(true);
        this.state.set('result');
      }
    });
  }

  async startCamera(): Promise<void> {
    this.camError.set(null);
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' }, audio: false });
      this.cameraOn.set(true);
      setTimeout(() => { if (this.video) this.video.nativeElement.srcObject = this.stream!; }, 0);
    } catch (e: any) {
      this.camError.set('Camera access denied or unavailable. Please allow camera to continue.');
    }
  }

  capture(): void {
    const v = this.video?.nativeElement, c = this.canvas?.nativeElement;
    if (!v || !c) return;
    c.width = v.videoWidth || 320;
    c.height = v.videoHeight || 240;
    c.getContext('2d')!.drawImage(v, 0, 0, c.width, c.height);
    this.selfiePreview.set(c.toDataURL('image/jpeg', 0.8));
    c.toBlob((blob) => {
      if (blob) {
        this.selfieFile = new File([blob], 'selfie.jpg', { type: 'image/jpeg' });
        this.selfieReady.set(true); // signal write → re-enables the Start Test button
      }
    }, 'image/jpeg', 0.8);
    this.stopCamera();
  }

  retakeSelfie(): void {
    this.selfiePreview.set(null);
    this.selfieReady.set(false);
    this.selfieFile = undefined;
    this.startCamera();
  }

  begin(): void {
    if (!this.studentId || !this.selfieFile) return;
    this.busy.set(true);
    this.data.entranceStart(this.studentId, this.selfieFile).subscribe({
      next: (ex) => {
        this.busy.set(false);
        this.exam.set(ex);
        this.answers = {};
        this.remaining.set(ex.durationMinutes * 60);
        this.state.set('test');
        this.startTimer();
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(e?.error?.message || 'Could not start test', 'OK', { duration: 3000 });
        // If the server blocked a second attempt, surface the existing result.
        this.checkExisting();
      },
    });
  }

  private startTimer(): void {
    this.stopTimer();
    this.timerId = setInterval(() => {
      this.remaining.update((r) => r - 1);
      if (this.remaining() <= 0) {
        this.stopTimer();
        this.submit(true);
      }
    }, 1000);
  }

  submit(auto: boolean): void {
    const ex = this.exam();
    if (!ex || this.busy()) return;
    this.stopTimer();
    this.busy.set(true);
    this.data.entranceSubmit(ex.attemptId, this.answers).subscribe({
      next: (r) => {
        this.busy.set(false);
        this.result.set(r);
        this.state.set('result');
        this.alreadyTaken.set(true); // one attempt used — no retake
        if (auto) this.snack.open('Time up — test auto-submitted', 'OK', { duration: 3000 });
        this.snack.open('Your result sheet will be sent to your WhatsApp number.', 'OK', { duration: 4000 });
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(e?.error?.message || 'Submit failed', 'OK', { duration: 3000 });
      },
    });
  }

  downloadResult(r: EntranceResult): void {
    this.data.entranceResultPdf(r.attemptId).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = `entrance-result.pdf`; a.click();
      URL.revokeObjectURL(url);
    });
  }

  reset(): void {
    this.state.set('setup');
    this.exam.set(null);
    this.result.set(null);
    this.answers = {};
    this.selfiePreview.set(null);
    this.selfieReady.set(false);
    this.selfieFile = undefined;
    if (!this.lockedStudent()) this.studentId = undefined;
  }

  mmss(sec: number): string {
    const s = Math.max(0, sec);
    return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
  }

  private stopCamera(): void {
    this.stream?.getTracks().forEach((t) => t.stop());
    this.stream = undefined;
    this.cameraOn.set(false);
  }

  private stopTimer(): void {
    if (this.timerId) clearInterval(this.timerId);
    this.timerId = undefined;
  }

  ngOnDestroy(): void {
    this.stopCamera();
    this.stopTimer();
  }
}
