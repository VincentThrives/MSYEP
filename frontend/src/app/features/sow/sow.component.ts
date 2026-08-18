import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

import { DataService } from '../../core/data.service';
import { AuthService } from '../../core/auth.service';
import { SearchSelectComponent } from '../../shared/search-select.component';
import { Center, Zone } from '../../core/models';

interface PersonField { key: string; label: string; question?: boolean; opinion?: boolean; }

@Component({
  selector: 'app-sow',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatSnackBarModule, MatTooltipModule, MatDialogModule, SearchSelectComponent,
  ],
  templateUrl: './sow.component.html',
  styleUrl: './sow.component.scss',
})
export class SowComponent {
  private data = inject(DataService);
  private auth = inject(AuthService);
  private snack = inject(MatSnackBar);

  /** CENTER acts on its own center; ADMIN picks Zone→Center; ZONE picks one of its own centers. */
  readonly role = this.auth.role();
  readonly isCenter = this.role === 'CENTER';
  readonly isAdmin = this.role === 'SUPER_ADMIN' || this.role === 'ADMIN';
  zones = signal<Zone[]>([]);
  selectedZoneId = signal<string | undefined>(undefined);
  centers = signal<Center[]>([]);
  selectedCenterId = signal<string | undefined>(undefined);
  /** Admin: centers of the chosen zone; Zone: its own centers (already scoped). */
  centersForPicker(): Center[] {
    if (!this.isAdmin) return this.centers();
    const zid = this.selectedZoneId();
    if (!zid) return [];
    // Show centers linked to the chosen zone, plus any not yet linked to a zone,
    // so legacy/unassigned centers stay reachable instead of a dead-end empty list.
    return this.centers().filter((c) => c.zoneId === zid || !c.zoneId);
  }
  private centerId(): string | undefined {
    return this.isCenter ? undefined : this.selectedCenterId();
  }
  get ready(): boolean {
    return this.isCenter || !!this.selectedCenterId();
  }

  readonly programs = [1, 2, 3, 4, 5, 6, 7, 8];
  readonly programOptions = [
    '1st Program', '2nd Program', '3rd Program', '4th Program',
    '5th Program', '6th Program', '7th Program', '8th Program',
  ];
  readonly groupIdx = [1, 2, 3, 4, 5, 6, 7, 8];

  readonly topFields = [
    { key: 'inaugurationDate', label: 'Program Inauguration Date', type: 'date' },
    { key: 'guestName', label: 'Guest Name', type: 'text' },
    { key: 'guestPhone', label: 'Guest Phone Number', type: 'text' },
    { key: 'guestDesignation', label: 'Guest Designation', type: 'text' },
    { key: 'groupsDate', label: 'Groups Date', type: 'date' },
  ];
  readonly letterFields = [
    { key: 'letterGuestName', label: 'Guest Name', type: 'text' },
    { key: 'letterGuestPhone', label: 'Guest Phone Number', type: 'text' },
    { key: 'letterGuestDesignation', label: 'Guest Designation', type: 'text' },
  ];
  readonly personFields: PersonField[] = [
    { key: 'anchoring', label: 'Program Anchoring by' },
    { key: 'welcomeSpeech', label: 'Welcome Speech by' },
    { key: 'preambleReading', label: 'Indian Constitution — Preamble Reading by' },
    { key: 'query1', label: 'Query 1 by', question: true },
    { key: 'query2', label: 'Query 2 by', question: true },
    { key: 'query3', label: 'Query 3 by', question: true },
    { key: 'voteOfThanks', label: 'Vote of Thanks by' },
    { key: 'honouring', label: 'Honouring by' },
    { key: 'guestOpinionBy', label: 'Taking Guest Opinion by' },
    { key: 'guestOpinion', label: 'Guest Opinion', opinion: true },
  ];

  view = signal<'cards' | 'form'>('cards');
  program = signal(0);
  saving = signal(false);
  filledPrograms = signal<Set<number>>(new Set());

  fields: Record<string, string> = {};
  photos: Record<string, string> = {};

  constructor() {
    if (this.isCenter) {
      this.loadStatuses();
    } else {
      this.data.centers().subscribe((c) => this.centers.set(c));
      if (this.isAdmin) this.data.zones().subscribe((z) => this.zones.set(z));
    }
  }

  onZone(id: string): void {
    this.selectedZoneId.set(id || undefined);
    this.selectedCenterId.set(undefined);
    this.view.set('cards');
  }

  onCenter(id: string): void {
    this.selectedCenterId.set(id || undefined);
    this.view.set('cards');
    if (id) this.loadStatuses();
  }

  private loadStatuses(): void {
    this.data.sowList(this.centerId()).subscribe((list) =>
      this.filledPrograms.set(new Set((list || []).map((s) => s.programIndex))));
  }

  openProgram(i: number): void {
    this.program.set(i);
    this.fields = {};
    this.photos = {};
    this.fields['programNumber'] = this.programOptions[i - 1];
    this.view.set('cards'); // reset then load
    this.data.sowGet(i, this.centerId()).subscribe((s) => {
      if (s) {
        this.fields = { ...s.fields };
        this.photos = { ...s.photos };
        if (!this.fields['programNumber']) this.fields['programNumber'] = this.programOptions[i - 1];
      }
      this.view.set('form');
    });
  }

  back(): void {
    this.view.set('cards');
    this.loadStatuses();
  }

  /** Read a captured/selected image and downscale it, keeping the WHOLE image (no cropping) so wide
   *  slides/photos are stored complete and show fully in the SOW PDF. */
  onPhoto(key: string, ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const img = new Image();
      img.onload = () => {
        // Fit within a max dimension, preserving aspect ratio — the entire image is kept.
        const max = 1280;
        const scale = Math.min(1, max / Math.max(img.width, img.height));
        const w = Math.max(1, Math.round(img.width * scale));
        const h = Math.max(1, Math.round(img.height * scale));
        const canvas = document.createElement('canvas');
        canvas.width = w;
        canvas.height = h;
        canvas.getContext('2d')!.drawImage(img, 0, 0, w, h);
        this.photos[key] = canvas.toDataURL('image/jpeg', 0.8);
      };
      img.src = reader.result as string;
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  clearPhoto(key: string): void {
    delete this.photos[key];
  }

  private dialog = inject(MatDialog);
  /** Open a photo full-size in a dialog (renders above the app shell / side nav). */
  openLightbox(src: string | undefined): void {
    if (!src) return;
    this.dialog.open(PhotoDialogComponent, {
      data: src,
      panelClass: 'photo-dialog',
      maxWidth: '98vw',
      maxHeight: '98vh',
      autoFocus: false,
    });
  }

  private payload() {
    return { fields: this.fields, photos: this.photos };
  }

  save(): void {
    this.saving.set(true);
    this.data.sowSave(this.program(), this.payload(), this.centerId()).subscribe({
      next: () => {
        this.saving.set(false);
        this.snack.open(`Program ${this.program()} saved`, 'OK', { duration: 2500 });
        this.loadStatuses();
      },
      error: (e) => this.fail(e),
    });
  }

  /** Save the latest values, then download the PDF (backend also emails the college). */
  download(): void {
    this.saving.set(true);
    this.data.sowSave(this.program(), this.payload(), this.centerId()).subscribe({
      next: () => {
        this.data.sowDownload(this.program(), this.centerId()).subscribe({
          next: (blob) => {
            this.saving.set(false);
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `SOW-Program-${this.program()}.pdf`;
            a.click();
            URL.revokeObjectURL(url);
            this.snack.open('Downloaded — and emailed to the college mail-id (if configured).', 'OK', { duration: 4000 });
            this.loadStatuses();
          },
          error: (e) => this.fail(e),
        });
      },
      error: (e) => this.fail(e),
    });
  }

  /** Download all saved SOW programs as ONE PDF — one program per page, full details + photos. */
  downloadAll(): void {
    this.saving.set(true);
    this.data.sowDownloadAll(this.centerId()).subscribe({
      next: (blob) => {
        this.saving.set(false);
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'KP-MSYEP-SOW-Programs.pdf';
        a.click();
        URL.revokeObjectURL(url);
        this.snack.open('Downloaded all saved SOW programs as one PDF.', 'OK', { duration: 3500 });
      },
      error: (e) => this.fail(e),
    });
  }

  private fail(e: any): void {
    this.saving.set(false);
    this.snack.open(e?.error?.message || 'Could not save', 'OK', { duration: 3500 });
  }
}

/** Full-size photo viewer shown in a MatDialog (CDK overlay → always above the shell + side nav). */
@Component({
  selector: 'app-photo-dialog',
  standalone: true,
  imports: [MatIconModule, MatButtonModule],
  template: `
    <div class="pv">
      <button mat-icon-button class="x" (click)="ref.close()" aria-label="Close">
        <mat-icon>close</mat-icon>
      </button>
      <img [src]="data" alt="Full photo" (click)="ref.close()" />
    </div>
  `,
  styles: [`
    .pv { position: relative; line-height: 0; }
    img { display: block; max-width: min(94vw, 1200px); max-height: 88vh; object-fit: contain; cursor: zoom-out; border-radius: 4px; }
    .x { position: absolute; top: -10px; right: -10px; background: rgba(0,0,0,.65); color: #fff; }
  `],
})
export class PhotoDialogComponent {
  ref = inject(MatDialogRef<PhotoDialogComponent>);
  data = inject<string>(MAT_DIALOG_DATA);
}
