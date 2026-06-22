import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { forkJoin, of, Observable } from 'rxjs';

import { DataService } from '../../core/data.service';
import { LocationService } from '../../core/location.service';
import { SearchSelectComponent } from '../../shared/search-select.component';
import {
  academicYears, Center, CenterRegistrationResult, CENTER_DOC_GROUPS, CENTER_DOC_SLOTS,
  CENTER_TYPES, MONTHS, Zone,
} from '../../core/models';

@Component({
  selector: 'app-center-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatSnackBarModule, MatTooltipModule,
    MatTabsModule, MatChipsModule, MatCheckboxModule, SearchSelectComponent,
  ],
  template: `
    <div class="head">
      <h1>Centers (Colleges)</h1>
      <div class="head-actions">
        <button mat-stroked-button (click)="locInput.click()"
          matTooltip="Import Karnataka District / Taluk / Gram Panchayat master (.xlsx)">
          <mat-icon>cloud_upload</mat-icon> Import Locations
        </button>
        <input #locInput type="file" hidden accept=".xlsx" (change)="onImportLocations($event)" />
        <button mat-flat-button color="primary" (click)="newCenter()">
          <mat-icon>add</mat-icon> Create Center
        </button>
      </div>
    </div>

    <!-- Registration success -->
    <div class="reg-panel" *ngIf="result() as r">
      <div class="reg-head">
        <mat-icon>check_circle</mat-icon>
        <span>Center registered successfully</span>
        <button mat-icon-button (click)="result.set(null)"><mat-icon>close</mat-icon></button>
      </div>
      <div class="reg-grid">
        <div><span class="k">Center Code</span><span class="v mono">{{ r.centerCode }}</span></div>
        <div><span class="k">Batch Code</span><span class="v mono">{{ r.batchCode }}</span></div>
        <div><span class="k">Enrollment Number</span><span class="v mono">{{ r.enrollmentNumber }}</span></div>
        <div><span class="k">Login (User ID)</span><span class="v">{{ r.headLoginId || '—' }}</span></div>
        <div><span class="k">Registration Date</span><span class="v">{{ r.center.registrationDate }}</span></div>
      </div>
      <div class="reg-note" [class.ok]="r.emailSent">
        <mat-icon>{{ r.emailSent ? 'mark_email_read' : 'info' }}</mat-icon> {{ r.emailNote }}
      </div>
      <button mat-stroked-button (click)="downloadCenterPdf(r.center)">
        <mat-icon>picture_as_pdf</mat-icon> Download registration PDF
      </button>
    </div>

    <div class="form-panel" *ngIf="editing()">
      <mat-tab-group>
        <!-- Tab 1: Academic & Type -->
        <mat-tab label="Academic & Type">
          <div class="tab">
            <div class="row">
              <app-search-select label="Academic Year" [options]="academicYearList"
                [(value)]="form.academicYear"></app-search-select>
              <app-search-select label="Center Academic Start Month" [options]="months"
                [(value)]="form.academicStartMonth"></app-search-select>
              <app-search-select label="Center Academic End Month" [options]="months"
                [(value)]="form.academicEndMonth"></app-search-select>
            </div>
            <div class="row">
              <app-search-select label="Select Center Type" [options]="centerTypes"
                [(value)]="form.centerType"></app-search-select>
              <mat-form-field appearance="outline"><mat-label>College / Center Name</mat-label>
                <input matInput [(ngModel)]="form.name" placeholder="Select or type a new college" /></mat-form-field>
            </div>
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>User ID</mat-label>
                <mat-icon matPrefix>person</mat-icon>
                <input matInput [(ngModel)]="form.userId" autocomplete="off"
                  placeholder="login id / email" [disabled]="!!form.id" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Password</mat-label>
                <mat-icon matPrefix>lock</mat-icon>
                <input matInput [(ngModel)]="form.password" autocomplete="new-password"
                  [disabled]="!!form.id" /></mat-form-field>
            </div>
            <p class="hint" *ngIf="form.id">Login credentials can't be changed here after creation.</p>
          </div>
        </mat-tab>

        <!-- Tab 2: Center Location -->
        <mat-tab label="Center Location">
          <div class="tab">
            <div class="row">
              <mat-form-field appearance="outline">
                <mat-label>Center District</mat-label>
                <mat-select [(ngModel)]="form.district" (selectionChange)="onDistrict()">
                  <mat-option *ngFor="let d of districts()" [value]="d">{{ d }}</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Taluk</mat-label>
                <mat-select [(ngModel)]="form.taluk" (selectionChange)="onTaluk()" [disabled]="!form.district">
                  <mat-option *ngFor="let t of taluks()" [value]="t">{{ t }}</mat-option>
                </mat-select>
              </mat-form-field>
              <ng-container *ngIf="gps().length; else gpFree">
                <mat-form-field appearance="outline">
                  <mat-label>Village / Gram Panchayath</mat-label>
                  <mat-select [(ngModel)]="form.gramPanchayat" [disabled]="!form.taluk">
                    <mat-option *ngFor="let g of gps()" [value]="g">{{ g }}</mat-option>
                  </mat-select>
                </mat-form-field>
              </ng-container>
              <ng-template #gpFree>
                <mat-form-field appearance="outline">
                  <mat-label>Village / Gram Panchayath</mat-label>
                  <input matInput [(ngModel)]="form.gramPanchayat" [disabled]="!form.taluk"
                    placeholder="Type, or import the GP master" />
                </mat-form-field>
              </ng-template>
            </div>
            <div class="row">
              <mat-form-field appearance="outline" class="wide"><mat-label>MSYEP Center Address</mat-label>
                <textarea matInput rows="2" [(ngModel)]="form.address"></textarea></mat-form-field>
            </div>
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>MSYEP Center Locality</mat-label>
                <input matInput [(ngModel)]="form.locality" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Pincode of college</mat-label>
                <input matInput [(ngModel)]="form.pincode" /></mat-form-field>
              <app-search-select label="Zone (University)" [options]="zones()" valueKey="id"
                labelKey="name" [emptyOption]="true" [(value)]="form.zoneId"></app-search-select>
            </div>
          </div>
        </mat-tab>

        <!-- Tab 3: Center Details -->
        <mat-tab label="Center Details">
          <div class="tab">
            <div class="allot">
              <div class="auto-field"><span class="k">Center Code</span>
                <span class="v mono">{{ form.code || 'Auto: CENTER-' + year + '-NNNN' }}</span></div>
              <div class="auto-field"><span class="k">Batch Code</span>
                <span class="v mono">{{ form.batchCode || 'Auto: BATCH-' + year + '-NNNN' }}</span></div>
              <div class="auto-field"><span class="k">Enrollment Number</span>
                <span class="v mono">{{ form.enrollmentNumber || 'Auto: CENENR' + year + 'NNNNNNN' }}</span></div>
            </div>
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>Batch Year</mat-label>
                <input matInput [(ngModel)]="form.batchYear" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Center Mail-ID</mat-label>
                <input matInput type="email" [(ngModel)]="form.email" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Center Office Number</mat-label>
                <input matInput [(ngModel)]="form.officeNumber" /></mat-form-field>
            </div>
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>Principal Name</mat-label>
                <input matInput [(ngModel)]="form.principalName" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Principal Number</mat-label>
                <input matInput [(ngModel)]="form.principalNumber" /></mat-form-field>
            </div>
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>UUCMS / Computer Operator Coordinator Name</mat-label>
                <input matInput [(ngModel)]="form.uucmsCoordinatorName" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>UUCMS / Computer Operator Coordinator Number</mat-label>
                <input matInput [(ngModel)]="form.uucmsCoordinatorNumber" /></mat-form-field>
            </div>
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>SC-ST Cell Coordinator Name</mat-label>
                <input matInput [(ngModel)]="form.scstCoordinatorName" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>SC-ST Cell Coordinator Number</mat-label>
                <input matInput [(ngModel)]="form.scstCoordinatorNumber" /></mat-form-field>
            </div>
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>Placement (Kaushalya Patha) Coordinator Name</mat-label>
                <input matInput [(ngModel)]="form.placementCoordinatorName" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Placement Coordinator Phone</mat-label>
                <input matInput [(ngModel)]="form.placementCoordinatorPhone" /></mat-form-field>
            </div>
            <div class="row toggle-row">
              <mat-checkbox [(ngModel)]="form.hasWebsite">Center has a website?</mat-checkbox>
              <mat-form-field appearance="outline" *ngIf="form.hasWebsite" class="grow">
                <mat-label>Center website link</mat-label>
                <input matInput [(ngModel)]="form.websiteLink" placeholder="https://" /></mat-form-field>
            </div>
            <div class="docs two">
              <div class="doc">
                <div class="doc-label">Center Logo (not mandatory)</div>
                <div class="doc-pick">
                  <button mat-stroked-button type="button" (click)="logoPick.click()">
                    <mat-icon>image</mat-icon> Choose file</button>
                  <input #logoPick type="file" hidden accept="image/*" (change)="onFile('centerLogo', $event)" />
                  <span class="doc-name" [class.has]="selectedName('centerLogo')">
                    {{ selectedName('centerLogo') || existingDocName('centerLogo') || 'No file chosen' }}</span>
                </div>
              </div>
              <div class="doc">
                <div class="doc-label">Center Building photo</div>
                <div class="doc-pick">
                  <button mat-stroked-button type="button" (click)="bldgPick.click()">
                    <mat-icon>image</mat-icon> Choose file</button>
                  <input #bldgPick type="file" hidden accept="image/*" (change)="onFile('centerBuilding', $event)" />
                  <span class="doc-name" [class.has]="selectedName('centerBuilding')">
                    {{ selectedName('centerBuilding') || existingDocName('centerBuilding') || 'No file chosen' }}</span>
                </div>
              </div>
            </div>
          </div>
        </mat-tab>

        <!-- Tab 4: Course Details -->
        <mat-tab label="Course Details">
          <div class="tab">
            <div class="row">
              <mat-form-field appearance="outline" class="wide">
                <mat-label>Enter your Center Courses</mat-label>
                <mat-chip-grid #chipGrid>
                  <mat-chip-row *ngFor="let c of form.courses" (removed)="removeCourse(c)">
                    {{ c }}<button matChipRemove><mat-icon>cancel</mat-icon></button>
                  </mat-chip-row>
                </mat-chip-grid>
                <input placeholder="Type a course and press Enter" [matChipInputFor]="chipGrid"
                  (matChipInputTokenEnd)="addCourse($event)" />
              </mat-form-field>
            </div>
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>Center Total Strength</mat-label>
                <input matInput type="number" [(ngModel)]="form.totalStrength" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>3rd/4th Sem or 1yr — Total</mat-label>
                <input matInput type="number" [(ngModel)]="form.strengthTotal" /></mat-form-field>
            </div>
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>3rd/4th Sem or 1yr — SC</mat-label>
                <input matInput type="number" [(ngModel)]="form.strengthSC" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>3rd/4th Sem or 1yr — ST</mat-label>
                <input matInput type="number" [(ngModel)]="form.strengthST" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>3rd/4th Sem or 1yr — General</mat-label>
                <input matInput type="number" [(ngModel)]="form.strengthGeneral" /></mat-form-field>
            </div>
          </div>
        </mat-tab>

        <!-- Tab 5: MOU Details -->
        <mat-tab label="MOU Details">
          <div class="tab">
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>MOU Date (from)</mat-label>
                <input matInput type="date" [(ngModel)]="form.dateOfMou" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>MOU Date (to)</mat-label>
                <input matInput type="date" [(ngModel)]="form.mouEndDate" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Year of Contract</mat-label>
                <input matInput [(ngModel)]="form.contractDuration" /></mat-form-field>
            </div>
          </div>
        </mat-tab>

        <!-- Tab 6: Documents -->
        <mat-tab label="Documents">
          <div class="tab">
            <ng-container *ngFor="let g of docGroups">
              <h3 class="sec">{{ g.title }}</h3>
              <div class="docs">
                <div class="doc" *ngFor="let slot of slotsIn(g.key)">
                  <div class="doc-label">{{ slot.label }}</div>
                  <div class="doc-pick">
                    <button mat-stroked-button type="button" (click)="picker.click()">
                      <mat-icon>image</mat-icon> Choose file</button>
                    <input #picker type="file" hidden accept="image/*,.pdf" (change)="onFile(slot.type, $event)" />
                    <span class="doc-name" [class.has]="selectedName(slot.type)">
                      {{ selectedName(slot.type) || existingDocName(slot.type) || 'No file chosen' }}</span>
                  </div>
                </div>
              </div>
            </ng-container>
          </div>
        </mat-tab>
      </mat-tab-group>

      <div class="form-actions">
        <button mat-button (click)="editing.set(false)">Cancel</button>
        <button mat-flat-button color="primary" (click)="save()" [disabled]="saving()">
          {{ form.id ? 'Save' : 'Submit' }}
        </button>
      </div>
    </div>

    <table mat-table [dataSource]="centers()" class="mat-elevation-z1 full">
      <ng-container matColumnDef="name">
        <th mat-header-cell *matHeaderCellDef>Center</th>
        <td mat-cell *matCellDef="let c">{{ c.name }}</td>
      </ng-container>
      <ng-container matColumnDef="code">
        <th mat-header-cell *matHeaderCellDef>Center Code</th>
        <td mat-cell *matCellDef="let c"><span class="mono small">{{ c.code }}</span></td>
      </ng-container>
      <ng-container matColumnDef="batch">
        <th mat-header-cell *matHeaderCellDef>Batch Code</th>
        <td mat-cell *matCellDef="let c"><span class="mono small">{{ c.batchCode }}</span></td>
      </ng-container>
      <ng-container matColumnDef="type">
        <th mat-header-cell *matHeaderCellDef>Type</th>
        <td mat-cell *matCellDef="let c">{{ c.centerType }}</td>
      </ng-container>
      <ng-container matColumnDef="district">
        <th mat-header-cell *matHeaderCellDef>District</th>
        <td mat-cell *matCellDef="let c">{{ c.district }}</td>
      </ng-container>
      <ng-container matColumnDef="actions">
        <th mat-header-cell *matHeaderCellDef>Actions</th>
        <td mat-cell *matCellDef="let c">
          <button mat-icon-button (click)="edit(c)"><mat-icon>edit</mat-icon></button>
          <button mat-icon-button matTooltip="Download PDF" (click)="downloadCenterPdf(c)">
            <mat-icon>picture_as_pdf</mat-icon></button>
          <button mat-icon-button color="warn" (click)="remove(c)"><mat-icon>delete</mat-icon></button>
        </td>
      </ng-container>
      <tr mat-header-row *matHeaderRowDef="cols"></tr>
      <tr mat-row *matRowDef="let row; columns: cols"></tr>
    </table>
    <p *ngIf="!centers().length" class="empty">No centers yet.</p>
  `,
  styles: [`
    .head { display: flex; justify-content: space-between; align-items: center; }
    .head-actions { display: flex; gap: 10px; }
    .form-panel, .reg-panel { background: #fff; border: 1px solid #eee; border-radius: 14px; padding: 18px; margin: 16px 0; }
    .reg-panel { border: 1px solid #cfe9d8; background: linear-gradient(180deg,#f3faf5,#ffffff); }
    .reg-head { display: flex; align-items: center; gap: 8px; font-weight: 700; color: #0E5132; }
    .reg-head mat-icon { color: #1E7A46; } .reg-head button { margin-left: auto; }
    .reg-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px,1fr)); gap: 10px; margin: 12px 0; }
    .reg-grid .k, .auto-field .k { display: block; font-size: 12px; color: #6b7d72; }
    .reg-grid .v { font-weight: 600; }
    .reg-note { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #8a6d1a;
      background: #fbf3d6; padding: 8px 12px; border-radius: 8px; margin-bottom: 12px; }
    .reg-note.ok { color: #0E5132; background: #e7f3ec; }
    .tab { padding: 18px 4px 4px; }
    .sec { color: #0E5132; border-left: 4px solid #C9A227; padding-left: 10px; margin: 8px 0 12px; }
    .row { display: flex; gap: 12px; flex-wrap: wrap; }
    .row mat-form-field { flex: 1; min-width: 200px; }
    .row .wide { flex: 1 1 100%; }
    .row .grow { flex: 2; }
    .toggle-row { align-items: center; }
    .hint { color: #999; font-size: 12px; }
    .allot { display: grid; grid-template-columns: repeat(auto-fit,minmax(220px,1fr)); gap: 12px; margin-bottom: 14px; }
    .auto-field { background: #f3faf5; border: 1px dashed #cfe9d8; border-radius: 10px; padding: 10px 12px; }
    .auto-field .v { font-weight: 600; color: #0E5132; }
    .docs { display: grid; grid-template-columns: repeat(auto-fit,minmax(300px,1fr)); gap: 14px; }
    .docs.two { grid-template-columns: repeat(auto-fit,minmax(280px,1fr)); margin-top: 8px; }
    .doc { border: 1px solid #eee; border-radius: 10px; padding: 12px; }
    .doc-label { font-size: 13px; color: #444; margin-bottom: 8px; min-height: 34px; }
    .doc-pick { display: flex; align-items: center; gap: 10px; }
    .doc-name { font-size: 12px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .doc-name.has { color: #1E7A46; font-weight: 600; }
    .mono { font-family: monospace; } .small { font-size: 12px; color: #555; }
    .form-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
    .full { width: 100%; margin-top: 12px; }
    .empty { color: #999; margin-top: 16px; }
  `],
})
export class CenterListComponent {
  private data = inject(DataService);
  private location = inject(LocationService);
  private snack = inject(MatSnackBar);

  districts = signal<string[]>([]);
  taluks = signal<string[]>([]);
  gps = signal<string[]>([]);

  cols = ['name', 'code', 'batch', 'type', 'district', 'actions'];
  centerTypes = CENTER_TYPES;
  months = MONTHS;
  academicYearList = academicYears();
  docSlots = CENTER_DOC_SLOTS;
  docGroups = CENTER_DOC_GROUPS;
  year = new Date().getFullYear();

  slotsIn(group: string) {
    return this.docSlots.filter((s) => s.group === group);
  }

  centers = signal<Center[]>([]);
  zones = signal<Zone[]>([]);
  editing = signal(false);
  saving = signal(false);
  result = signal<CenterRegistrationResult | null>(null);
  form: Center = this.blank();
  private files: Record<string, File> = {};

  constructor() {
    this.load();
    this.data.zones().subscribe((z) => this.zones.set(z));
    this.location.districts().subscribe((d) => this.districts.set(d));
  }

  onDistrict(): void {
    this.form.taluk = undefined;
    this.form.gramPanchayat = undefined;
    this.taluks.set([]);
    this.gps.set([]);
    if (this.form.district) {
      this.location.taluks(this.form.district).subscribe((t) => this.taluks.set(t));
    }
  }

  onTaluk(): void {
    this.form.gramPanchayat = undefined;
    this.gps.set([]);
    if (this.form.district && this.form.taluk) {
      this.location.gramPanchayats(this.form.district, this.form.taluk).subscribe((g) => this.gps.set(g));
    }
  }

  addCourse(ev: MatChipInputEvent): void {
    const v = (ev.value || '').trim();
    if (v) (this.form.courses ??= []).push(v);
    ev.chipInput!.clear();
  }

  removeCourse(c: string): void {
    this.form.courses = (this.form.courses || []).filter((x) => x !== c);
  }

  onImportLocations(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.location.importExcel(file).subscribe({
      next: (r) => {
        this.snack.open(`Imported ${r.added} location rows`, 'OK', { duration: 3000 });
        if (this.form.taluk) this.onTaluk();
      },
      error: (e) => this.snack.open(e?.error?.message || 'Import failed', 'OK', { duration: 3000 }),
    });
    input.value = '';
  }

  load(): void {
    this.data.centers().subscribe((c) => this.centers.set(c));
  }

  blank(): Center {
    return { name: '', courses: [], hasWebsite: false };
  }

  newCenter(): void {
    this.form = this.blank();
    this.files = {};
    this.taluks.set([]);
    this.gps.set([]);
    this.result.set(null);
    this.editing.set(true);
  }

  edit(c: Center): void {
    this.form = { ...c, courses: [...(c.courses || [])] };
    this.files = {};
    this.taluks.set([]);
    this.gps.set([]);
    this.editing.set(true);
    if (c.district) this.location.taluks(c.district).subscribe((t) => this.taluks.set(t));
    if (c.district && c.taluk) {
      this.location.gramPanchayats(c.district, c.taluk).subscribe((g) => this.gps.set(g));
    }
  }

  onFile(type: string, ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (file.size > 500 * 1024) {
      this.snack.open('File exceeds 500 KB limit', 'OK', { duration: 3000 });
      input.value = '';
      return;
    }
    this.files[type] = file;
  }

  selectedName(type: string): string | null {
    return this.files[type]?.name ?? null;
  }

  existingDocName(type: string): string | null {
    return this.form.documents?.find((d) => d.type === type)?.filename ?? null;
  }

  save(): void {
    if (!this.form.name) {
      this.snack.open('College / Center name is required', 'OK', { duration: 2500 });
      return;
    }
    if (!this.form.id && (!this.form.userId || !this.form.password)) {
      this.snack.open('User ID and Password are required to create the center login', 'OK', { duration: 3500 });
      return;
    }
    this.saving.set(true);
    if (this.form.id) {
      this.data.updateCenter(this.form.id, this.form).subscribe({
        next: (c) => this.afterSave(c.id!, null),
        error: (e) => this.fail(e),
      });
    } else {
      this.data.createCenter(this.form).subscribe({
        next: (res) => this.afterSave(res.center.id!, res),
        error: (e) => this.fail(e),
      });
    }
  }

  private afterSave(id: string, res: CenterRegistrationResult | null): void {
    const slots = Object.entries(this.files);
    const uploads: Observable<unknown> = slots.length
      ? forkJoin(slots.map(([type, file]) =>
          this.data.uploadCenterDocument(id, type, this.labelFor(type), file)))
      : of(null);
    uploads.subscribe({
      next: () => {
        this.saving.set(false);
        this.editing.set(false);
        if (res) this.result.set(res);
        this.snack.open(res ? res.emailNote : 'Center saved', 'OK', { duration: 4000 });
        this.load();
      },
      error: (e: any) => {
        this.saving.set(false);
        this.snack.open('Saved, but a document upload failed: ' + (e?.error?.message || ''), 'OK', { duration: 4000 });
        this.editing.set(false);
        this.load();
      },
    });
  }

  private fail(e: any): void {
    this.saving.set(false);
    this.snack.open(e?.error?.message || 'Save failed', 'OK', { duration: 3000 });
  }

  private labelFor(type: string): string {
    return this.docSlots.find((s) => s.type === type)?.label ?? type;
  }

  remove(c: Center): void {
    if (!c.id || !confirm(`Delete center "${c.name}"?`)) return;
    this.data.deleteCenter(c.id).subscribe(() => {
      this.snack.open('Center deleted', 'OK', { duration: 2000 });
      this.load();
    });
  }

  downloadCenterPdf(c: Center): void {
    if (!c.id) return;
    this.data.centerRegistrationPdf(c.id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = `Center-${c.name}.pdf`; a.click();
      URL.revokeObjectURL(url);
    });
  }
}
