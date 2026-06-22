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
  answers: Record<string, string> = {};

  cameraOn = signal(false);
  selfiePreview = signal<string | null>(null);
  camError = signal<string | null>(null);
  busy = signal(false);
  remaining = signal(0);

  private stream?: MediaStream;
  private timerId?: any;
  private selfieFile?: File;

  lockedStudent = computed(() => this.auth.role() === 'STUDENT' && !!this.auth.user()?.studentId);
  answeredCount = computed(() => Object.values(this.answers).filter(Boolean).length);
  canStart = computed(() => !!this.studentId && !!this.selfieFile);

  constructor() {
    this.data.students().subscribe((s) => this.students.set(s));
    if (this.lockedStudent()) this.studentId = this.auth.user()!.studentId;
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
      if (blob) this.selfieFile = new File([blob], 'selfie.jpg', { type: 'image/jpeg' });
    }, 'image/jpeg', 0.8);
    this.stopCamera();
  }

  retakeSelfie(): void {
    this.selfiePreview.set(null);
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
        if (auto) this.snack.open('Time up — test auto-submitted', 'OK', { duration: 3000 });
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
