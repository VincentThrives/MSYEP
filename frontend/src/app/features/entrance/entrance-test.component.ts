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
  template: `
    <h1>Entrance Test</h1>

    <!-- SETUP -->
    <mat-card class="card" *ngIf="state() === 'setup'">
      <p class="lead">10 questions · 10 minutes · pass mark 5/10. A selfie is required to start.</p>
      <div class="row" *ngIf="!lockedStudent()">
        <app-search-select label="Select Candidate (Student)" [options]="students()" valueKey="id"
          labelKey="name" [(value)]="studentId"></app-search-select>
      </div>
      <div class="selfie-wrap">
        <div class="cam">
          <video #video autoplay playsinline [hidden]="!!selfiePreview()"></video>
          <img *ngIf="selfiePreview() as p" [src]="p" alt="selfie" />
          <canvas #canvas hidden></canvas>
        </div>
        <div class="cam-actions">
          <button mat-stroked-button (click)="startCamera()" *ngIf="!cameraOn() && !selfiePreview()">
            <mat-icon>photo_camera</mat-icon> Enable camera
          </button>
          <button mat-flat-button color="primary" (click)="capture()" *ngIf="cameraOn() && !selfiePreview()">
            <mat-icon>camera</mat-icon> Capture selfie
          </button>
          <button mat-stroked-button (click)="retakeSelfie()" *ngIf="selfiePreview()">
            <mat-icon>refresh</mat-icon> Retake selfie
          </button>
        </div>
      </div>
      <p class="err" *ngIf="camError()">{{ camError() }}</p>
      <div class="actions">
        <button mat-flat-button color="primary" [disabled]="!canStart() || busy()" (click)="begin()">
          <mat-icon>play_arrow</mat-icon> Start Test
        </button>
      </div>
    </mat-card>

    <!-- TEST -->
    <div *ngIf="state() === 'test' && exam() as ex">
      <div class="timer" [class.warn]="remaining() <= 60">
        <mat-icon>timer</mat-icon> {{ mmss(remaining()) }}
        <span class="spacer"></span>
        <span>{{ answeredCount() }} / {{ ex.questions.length }} answered</span>
      </div>
      <mat-progress-bar mode="determinate" [value]="100 * remaining() / (ex.durationMinutes * 60)"></mat-progress-bar>
      <mat-card class="q" *ngFor="let q of ex.questions; let i = index">
        <div class="q-text"><b>Q{{ i + 1 }}.</b> {{ q.question }}</div>
        <mat-radio-group [(ngModel)]="answers[q.questionId]" class="opts">
          <mat-radio-button *ngFor="let o of q.options" [value]="o">{{ o }}</mat-radio-button>
        </mat-radio-group>
      </mat-card>
      <div class="actions">
        <button mat-flat-button color="primary" [disabled]="busy()" (click)="submit(false)">
          <mat-icon>done</mat-icon> Submit Test
        </button>
      </div>
    </div>

    <!-- RESULT -->
    <mat-card class="card" *ngIf="state() === 'result' && result() as r">
      <div class="result" [class.pass]="r.passed" [class.fail]="!r.passed">
        <mat-icon>{{ r.passed ? 'verified' : 'cancel' }}</mat-icon>
        <div>
          <div class="big">{{ r.score }} / {{ r.total }}</div>
          <div class="badge">{{ r.passed ? 'PASS' : 'FAIL' }}</div>
        </div>
      </div>
      <div class="review">
        <div class="rev-item" *ngFor="let it of r.items; let i = index" [class.ok]="it.correct">
          <mat-icon>{{ it.correct ? 'check_circle' : 'cancel' }}</mat-icon>
          <div>
            <div class="rev-q"><b>Q{{ i + 1 }}.</b> {{ it.question }}</div>
            <div class="rev-a">Your answer: <b>{{ it.selectedAnswer || '—' }}</b>
              <span *ngIf="!it.correct"> · Correct: <b>{{ it.correctAnswer }}</b></span>
            </div>
          </div>
        </div>
      </div>
      <div class="actions">
        <button mat-stroked-button (click)="downloadResult(r)"><mat-icon>picture_as_pdf</mat-icon> Download Result</button>
        <button mat-flat-button color="primary" (click)="reset()"><mat-icon>replay</mat-icon> Take Again</button>
      </div>
    </mat-card>
  `,
  styles: [`
    h1 { color: #0E5132; }
    .card { padding: 20px; max-width: 720px; }
    .lead { color: #555; }
    .row { max-width: 420px; }
    .selfie-wrap { display: flex; gap: 16px; align-items: center; margin: 12px 0; flex-wrap: wrap; }
    .cam { width: 240px; height: 180px; border-radius: 12px; overflow: hidden; background: #eef2ef;
      display: grid; place-items: center; border: 1px solid #dfe7e1; }
    .cam video, .cam img { width: 100%; height: 100%; object-fit: cover; }
    .cam-actions { display: flex; flex-direction: column; gap: 8px; }
    .err { color: #d32f2f; font-size: 13px; }
    .actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 12px; }
    .timer { position: sticky; top: 0; z-index: 5; display: flex; align-items: center; gap: 8px;
      background: linear-gradient(120deg,#1E7A46,#0E5132); color: #fff; font-weight: 700;
      padding: 10px 16px; border-radius: 10px; font-size: 18px; }
    .timer.warn { background: linear-gradient(120deg,#e53935,#b71c1c); }
    .timer .spacer { flex: 1; } .timer span:last-child { font-size: 13px; font-weight: 500; }
    .q { padding: 16px; margin: 12px 0; }
    .q-text { margin-bottom: 10px; }
    .opts { display: flex; flex-direction: column; gap: 6px; }
    .result { display: flex; align-items: center; gap: 16px; padding: 12px 0; }
    .result mat-icon { font-size: 48px; height: 48px; width: 48px; }
    .result.pass { color: #1E7A46; } .result.fail { color: #c62828; }
    .big { font-size: 32px; font-weight: 800; }
    .badge { font-weight: 700; letter-spacing: 1px; }
    .review { margin-top: 8px; border-top: 1px solid #eee; padding-top: 8px; }
    .rev-item { display: flex; gap: 10px; padding: 8px 0; border-bottom: 1px solid #f3f3f3; }
    .rev-item mat-icon { color: #c62828; } .rev-item.ok mat-icon { color: #1E7A46; }
    .rev-q { font-size: 14px; } .rev-a { font-size: 13px; color: #666; }
  `],
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
