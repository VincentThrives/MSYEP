import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { forkJoin, of, Observable } from 'rxjs';

import { DataService } from '../../core/data.service';
import { SearchSelectComponent } from '../../shared/search-select.component';
import { LocationPickerComponent } from '../../shared/location-picker.component';
import { TermsComponent } from '../../shared/terms.component';
import {
  GENDERS, INVESTMENT_CAPACITY, MEMBERSHIP_TIERS, OWN_RENT, START_TIMELINE,
  Zone, ZoneRegistrationResult, ZONE_DOC_SLOTS,
} from '../../core/models';

@Component({
  selector: 'app-zone-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatCheckboxModule, MatTabsModule, MatSnackBarModule,
    SearchSelectComponent, LocationPickerComponent, TermsComponent,
  ],
  template: `
    <div class="head">
      <h1>Zones / Franchise</h1>
      <button mat-flat-button color="primary" (click)="newZone()">
        <mat-icon>add_business</mat-icon> Franchise Sign-up
      </button>
    </div>

    <!-- Success -->
    <div class="reg-panel" *ngIf="result() as r">
      <div class="reg-head">
        <mat-icon>check_circle</mat-icon><span>Franchise registered</span>
        <button mat-icon-button (click)="result.set(null)"><mat-icon>close</mat-icon></button>
      </div>
      <div class="reg-grid">
        <div><span class="k">Zone Code</span><span class="v mono">{{ r.zoneCode }}</span></div>
        <div><span class="k">Login (User ID)</span><span class="v">{{ r.loginId || '—' }}</span></div>
        <div><span class="k">Membership</span><span class="v">{{ r.zone.membershipTier }} · ₹{{ r.membershipAmount | number }}</span></div>
        <div><span class="k">Status</span><span class="v badge">{{ r.status }}</span></div>
      </div>
      <div class="reg-note"><mat-icon>info</mat-icon> {{ r.note }}</div>
    </div>

    <div class="form-panel" *ngIf="editing()">
      <mat-tab-group>
        <!-- 1: Login & Organization -->
        <mat-tab label="Login & Organization">
          <div class="tab">
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>User ID</mat-label>
                <mat-icon matPrefix>person</mat-icon>
                <input matInput [(ngModel)]="form.userId" autocomplete="off" [disabled]="!!form.id" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Password</mat-label>
                <mat-icon matPrefix>lock</mat-icon>
                <input matInput type="password" [(ngModel)]="form.password" autocomplete="new-password" [disabled]="!!form.id" /></mat-form-field>
            </div>
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>Organization Name</mat-label>
                <input matInput [(ngModel)]="form.organizationName" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Zone / University Name</mat-label>
                <input matInput [(ngModel)]="form.name" placeholder="defaults to organization name" /></mat-form-field>
            </div>
            <div class="row">
              <app-search-select label="Building (Own / Rent)" [options]="ownRent"
                [(value)]="form.buildingOwnership"></app-search-select>
              <mat-checkbox [(ngModel)]="form.hasWebsite" class="chk">Organization has a website?</mat-checkbox>
            </div>
            <div class="row" *ngIf="form.hasWebsite">
              <mat-form-field appearance="outline" class="wide"><mat-label>Website link</mat-label>
                <input matInput [(ngModel)]="form.websiteLink" placeholder="https://" /></mat-form-field>
            </div>
            <div class="row" *ngIf="!form.hasWebsite">
              <mat-form-field appearance="outline" class="wide"><mat-label>Budget to create the website</mat-label>
                <input matInput [(ngModel)]="form.websiteBudget" /></mat-form-field>
            </div>
            <h4 class="sub">Documents available (upload in the Documents tab)</h4>
            <div class="flags">
              <mat-checkbox [(ngModel)]="form.hasRegisteredCopy">Registered Copy</mat-checkbox>
              <mat-checkbox [(ngModel)]="form.hasMsme">MSME</mat-checkbox>
              <mat-checkbox [(ngModel)]="form.hasGst">GST</mat-checkbox>
              <mat-checkbox [(ngModel)]="form.hasNitiAayog">Niti Aayog</mat-checkbox>
              <mat-checkbox [(ngModel)]="form.hasNgoDarpan">NGO Darpan</mat-checkbox>
              <mat-checkbox [(ngModel)]="form.has12A80G">12A / 80G</mat-checkbox>
            </div>
            <mat-checkbox [(ngModel)]="form.willingToComply" class="chk">
              If documents not available — willing to comply with us?
            </mat-checkbox>
          </div>
        </mat-tab>

        <!-- 2: Owner Details -->
        <mat-tab label="Owner Details">
          <div class="tab">
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>Owner Full Name</mat-label>
                <input matInput [(ngModel)]="form.ownerName" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Date of Birth</mat-label>
                <input matInput type="date" [(ngModel)]="form.ownerDob" /></mat-form-field>
              <app-search-select label="Gender" [options]="genders" [(value)]="form.ownerGender"></app-search-select>
            </div>
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>Contact Number</mat-label>
                <input matInput [(ngModel)]="form.contactNumber" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Alternate Number</mat-label>
                <input matInput [(ngModel)]="form.alternateNumber" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Email Address</mat-label>
                <input matInput type="email" [(ngModel)]="form.email" /></mat-form-field>
            </div>
            <div class="row">
              <mat-form-field appearance="outline" class="wide"><mat-label>Full Address</mat-label>
                <textarea matInput rows="2" [(ngModel)]="form.fullAddress"></textarea></mat-form-field>
            </div>
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>City</mat-label>
                <input matInput [(ngModel)]="form.city" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>State</mat-label>
                <input matInput [(ngModel)]="form.state" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Pincode</mat-label>
                <input matInput [(ngModel)]="form.pincode" /></mat-form-field>
            </div>
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>Occupation / Business Background</mat-label>
                <input matInput [(ngModel)]="form.occupation" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>Educational Qualification</mat-label>
                <input matInput [(ngModel)]="form.educationalQualification" /></mat-form-field>
            </div>
            <h4 class="sub">Zone location</h4>
            <div class="row">
              <app-location-picker [(district)]="form.district" [(taluk)]="form.taluk"
                [(gramPanchayat)]="form.gramPanchayat"></app-location-picker>
            </div>
          </div>
        </mat-tab>

        <!-- 3: KYC & Documents -->
        <mat-tab label="KYC & Documents">
          <div class="tab">
            <div class="row">
              <mat-form-field appearance="outline"><mat-label>Aadhaar Card Number</mat-label>
                <input matInput [(ngModel)]="form.aadhaarNumber" /></mat-form-field>
              <mat-form-field appearance="outline"><mat-label>PAN Number</mat-label>
                <input matInput [(ngModel)]="form.panNumber" /></mat-form-field>
            </div>
            <div class="row">
              <mat-form-field appearance="outline" class="wide"><mat-label>Bank Account Details</mat-label>
                <input matInput [(ngModel)]="form.bankAccountDetails" placeholder="A/c no · IFSC · Bank · Branch" /></mat-form-field>
            </div>
            <h4 class="sub">Upload documents (photos / copies, max 2 MB each)</h4>
            <div class="docs">
              <div class="doc" *ngFor="let slot of docSlots">
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
          </div>
        </mat-tab>

        <!-- 4: Business Fit -->
        <mat-tab label="Business Fit">
          <div class="tab">
            <div class="row">
              <app-search-select label="Investment Capacity (₹)" [options]="investments"
                [(value)]="form.investmentCapacity"></app-search-select>
              <mat-form-field appearance="outline"><mat-label>Preferred Location for Franchise</mat-label>
                <input matInput [(ngModel)]="form.preferredLocation" /></mat-form-field>
            </div>
            <div class="row">
              <app-search-select label="Own or Rent the proposed space?" [options]="ownRent"
                [(value)]="form.spaceOwnership"></app-search-select>
              <mat-form-field appearance="outline"><mat-label>Space Available (sq.ft)</mat-label>
                <input matInput [(ngModel)]="form.spaceSqft" /></mat-form-field>
              <app-search-select label="How soon do you plan to start?" [options]="timelines"
                [(value)]="form.startTimeline"></app-search-select>
            </div>
          </div>
        </mat-tab>

        <!-- 5: Membership & Submit -->
        <mat-tab label="Membership & Submit">
          <div class="tab">
            <h4 class="sub">Select a membership tier</h4>
            <div class="tiers">
              <div class="tier" *ngFor="let t of tiers" [class.sel]="form.membershipTier === t.name"
                (click)="form.membershipTier = t.name">
                <div class="tier-name">{{ t.name }}</div>
                <div class="tier-amt">₹{{ t.amount | number }}</div>
                <mat-icon *ngIf="form.membershipTier === t.name">check_circle</mat-icon>
              </div>
            </div>
            <button mat-stroked-button type="button" (click)="showTerms.set(!showTerms())">
              <mat-icon>description</mat-icon>
              {{ showTerms() ? 'Hide' : 'View' }} Terms &amp; Conditions
            </button>
            <div class="terms-box" *ngIf="showTerms()"><app-terms></app-terms></div>
            <mat-checkbox [(ngModel)]="form.tcAccepted" class="chk">
              I have read and accept the Terms &amp; Conditions of the KP-MSYEP franchise programme.
            </mat-checkbox>
            <p class="note-pay"><mat-icon>info</mat-icon>
              Signup payment &amp; WhatsApp/activation email are pending gateway integration —
              for now the franchise is saved as <b>PENDING</b>.</p>
          </div>
        </mat-tab>
      </mat-tab-group>

      <div class="form-actions">
        <button mat-button (click)="editing.set(false)">Cancel</button>
        <button mat-flat-button color="primary" (click)="save()" [disabled]="saving()">
          {{ form.id ? 'Save' : 'Submit Sign-up' }}
        </button>
      </div>
    </div>

    <table mat-table [dataSource]="zones()" class="mat-elevation-z1 full">
      <ng-container matColumnDef="code">
        <th mat-header-cell *matHeaderCellDef>Zone Code</th>
        <td mat-cell *matCellDef="let z"><span class="mono small">{{ z.code }}</span></td>
      </ng-container>
      <ng-container matColumnDef="org">
        <th mat-header-cell *matHeaderCellDef>Organization</th>
        <td mat-cell *matCellDef="let z">{{ z.organizationName || z.name }}</td>
      </ng-container>
      <ng-container matColumnDef="owner">
        <th mat-header-cell *matHeaderCellDef>Owner</th>
        <td mat-cell *matCellDef="let z">{{ z.ownerName }}</td>
      </ng-container>
      <ng-container matColumnDef="tier">
        <th mat-header-cell *matHeaderCellDef>Membership</th>
        <td mat-cell *matCellDef="let z">{{ z.membershipTier }}</td>
      </ng-container>
      <ng-container matColumnDef="status">
        <th mat-header-cell *matHeaderCellDef>Status</th>
        <td mat-cell *matCellDef="let z"><span class="status" [class.active]="z.status==='ACTIVE'">{{ z.status }}</span></td>
      </ng-container>
      <ng-container matColumnDef="actions">
        <th mat-header-cell *matHeaderCellDef>Actions</th>
        <td mat-cell *matCellDef="let z">
          <button mat-icon-button (click)="edit(z)"><mat-icon>edit</mat-icon></button>
          <button mat-icon-button color="warn" (click)="remove(z)"><mat-icon>delete</mat-icon></button>
        </td>
      </ng-container>
      <tr mat-header-row *matHeaderRowDef="cols"></tr>
      <tr mat-row *matRowDef="let row; columns: cols"></tr>
    </table>
    <p *ngIf="!zones().length" class="empty">No franchises yet.</p>
  `,
  styles: [`
    .head { display: flex; justify-content: space-between; align-items: center; }
    .form-panel, .reg-panel { background: #fff; border: 1px solid #eee; border-radius: 14px; padding: 18px; margin: 16px 0; }
    .reg-panel { border: 1px solid #cfe9d8; background: linear-gradient(180deg,#f3faf5,#ffffff); }
    .reg-head { display: flex; align-items: center; gap: 8px; font-weight: 700; color: #0E5132; }
    .reg-head mat-icon { color: #1E7A46; } .reg-head button { margin-left: auto; }
    .reg-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(210px,1fr)); gap: 10px; margin: 12px 0; }
    .reg-grid .k { display: block; font-size: 12px; color: #6b7d72; } .reg-grid .v { font-weight: 600; }
    .reg-grid .badge { background: #fff3cd; color: #8a6d1a; padding: 2px 8px; border-radius: 6px; }
    .reg-note, .note-pay { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #8a6d1a;
      background: #fbf3d6; padding: 8px 12px; border-radius: 8px; }
    .tab { padding: 18px 4px 4px; }
    .sub { color: #0E5132; margin: 14px 0 8px; }
    .row { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
    .row mat-form-field { flex: 1; min-width: 200px; } .row .wide { flex: 1 1 100%; }
    .chk { margin: 8px 0; } .flags { display: flex; gap: 18px; flex-wrap: wrap; margin: 6px 0 10px; }
    .docs { display: grid; grid-template-columns: repeat(auto-fit,minmax(260px,1fr)); gap: 14px; }
    .doc { border: 1px solid #eee; border-radius: 10px; padding: 12px; }
    .doc-label { font-size: 13px; color: #444; margin-bottom: 8px; }
    .doc-pick { display: flex; align-items: center; gap: 10px; }
    .doc-name { font-size: 12px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .doc-name.has { color: #1E7A46; font-weight: 600; }
    .tiers { display: grid; grid-template-columns: repeat(auto-fit,minmax(150px,1fr)); gap: 14px; margin-bottom: 12px; }
    .tier { border: 2px solid #e6e6e6; border-radius: 14px; padding: 18px; text-align: center; cursor: pointer;
      transition: .12s; position: relative; }
    .tier:hover { border-color: #C9A227; }
    .tier.sel { border-color: #1E7A46; background: #f3faf5; }
    .tier-name { font-weight: 800; color: #0E5132; letter-spacing: 1px; }
    .tier-amt { font-size: 22px; font-weight: 700; margin-top: 6px; }
    .tier mat-icon { position: absolute; top: 8px; right: 8px; color: #1E7A46; }
    .terms-box { max-height: 320px; overflow-y: auto; border: 1px solid #e6e6e6; border-radius: 10px;
      padding: 16px; margin: 12px 0; background: #fcfcfb; }
    .form-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
    .full { width: 100%; margin-top: 12px; } .empty { color: #999; margin-top: 16px; }
    .mono { font-family: monospace; } .small { font-size: 12px; color: #555; }
    .status { background: #fff3cd; color: #8a6d1a; padding: 2px 8px; border-radius: 6px; font-size: 12px; }
    .status.active { background: #e7f3ec; color: #0E5132; }
  `],
})
export class ZoneListComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);

  cols = ['code', 'org', 'owner', 'tier', 'status', 'actions'];
  genders = GENDERS;
  ownRent = OWN_RENT;
  investments = INVESTMENT_CAPACITY;
  timelines = START_TIMELINE;
  tiers = MEMBERSHIP_TIERS;
  docSlots = ZONE_DOC_SLOTS;

  zones = signal<Zone[]>([]);
  editing = signal(false);
  saving = signal(false);
  showTerms = signal(false);
  result = signal<ZoneRegistrationResult | null>(null);
  form: Zone = this.blank();
  private files: Record<string, File> = {};

  constructor() {
    this.load();
  }

  load(): void {
    this.data.zones().subscribe((z) => this.zones.set(z));
  }

  blank(): Zone {
    return { name: '', hasWebsite: false, tcAccepted: false };
  }

  newZone(): void {
    this.form = this.blank();
    this.files = {};
    this.result.set(null);
    this.editing.set(true);
  }

  edit(z: Zone): void {
    this.form = { ...z };
    this.files = {};
    this.editing.set(true);
  }

  onFile(type: string, ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      this.snack.open('File exceeds 2 MB limit', 'OK', { duration: 3000 });
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
    if (!this.form.name && !this.form.organizationName) {
      this.snack.open('Organization / Zone name is required', 'OK', { duration: 2500 });
      return;
    }
    if (!this.form.id && (!this.form.userId || !this.form.password)) {
      this.snack.open('User ID and Password are required for the franchise login', 'OK', { duration: 3500 });
      return;
    }
    if (!this.form.id && !this.form.tcAccepted) {
      this.snack.open('Please accept the Terms & Conditions', 'OK', { duration: 3000 });
      return;
    }
    this.saving.set(true);
    if (this.form.id) {
      this.data.updateZone(this.form.id, this.form).subscribe({
        next: (z) => this.afterSave(z.id!, null),
        error: (e) => this.fail(e),
      });
    } else {
      this.data.createZone(this.form).subscribe({
        next: (res) => this.afterSave(res.zone.id!, res),
        error: (e) => this.fail(e),
      });
    }
  }

  private afterSave(id: string, res: ZoneRegistrationResult | null): void {
    const slots = Object.entries(this.files);
    const uploads: Observable<unknown> = slots.length
      ? forkJoin(slots.map(([type, file]) =>
          this.data.uploadZoneDocument(id, type, this.labelFor(type), file)))
      : of(null);
    uploads.subscribe({
      next: () => {
        this.saving.set(false);
        this.editing.set(false);
        if (res) this.result.set(res);
        this.snack.open(res ? res.note : 'Zone saved', 'OK', { duration: 4000 });
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
    this.snack.open(e?.error?.message || 'Sign-up failed', 'OK', { duration: 3000 });
  }

  private labelFor(type: string): string {
    return this.docSlots.find((s) => s.type === type)?.label ?? type;
  }

  remove(z: Zone): void {
    if (!z.id || !confirm(`Delete franchise "${z.organizationName || z.name}"?`)) return;
    this.data.deleteZone(z.id).subscribe(() => {
      this.snack.open('Deleted', 'OK', { duration: 2000 });
      this.load();
    });
  }
}
