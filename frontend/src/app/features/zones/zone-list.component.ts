import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { forkJoin, of, Observable } from 'rxjs';

import { DataService } from '../../core/data.service';
import { SearchSelectComponent } from '../../shared/search-select.component';
import { LocationPickerComponent } from '../../shared/location-picker.component';
import { TermsComponent } from '../../shared/terms.component';
import {
  GENDERS, INVESTMENT_CAPACITY, MEMBERSHIP_TIERS, MEMBERSHIP_TERRITORY, OWN_RENT, START_TIMELINE,
  Zone, ZoneRegistrationResult, ZONE_DOC_SLOTS,
} from '../../core/models';

@Component({
  selector: 'app-zone-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatCheckboxModule, MatTabsModule, MatSnackBarModule,
    MatTooltipModule, SearchSelectComponent, LocationPickerComponent, TermsComponent,
  ],
  templateUrl: './zone-list.component.html',
  styleUrl: './zone-list.component.scss',
})
export class ZoneListComponent {
  private data = inject(DataService);
  private snack = inject(MatSnackBar);
  private route = inject(ActivatedRoute);
  private destroyRef = inject(DestroyRef);

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
  hidePassword = true;   // password show/hide (eye) toggle
  private files: Record<string, File> = {};

  // ---- Wizard ----
  readonly sections = [
    { key: 'login', label: 'Login & Organization' },
    { key: 'owner', label: 'Owner Details' },
    { key: 'kyc', label: 'KYC & Documents' },
    { key: 'fit', label: 'Business Fit' },
    { key: 'certificate', label: 'Franchise Certificate' },
    { key: 'membership', label: 'Membership & Submit' },
  ];
  tabIndex = signal(0);

  // ---- View filters ----
  fDistrict = signal('');
  fTaluk = signal('');
  fGp = signal('');
  fSearch = signal('');

  filtered = computed(() => {
    const d = this.fDistrict(), t = this.fTaluk(), g = this.fGp();
    const q = this.fSearch().trim().toLowerCase();
    return this.zones().filter((z) =>
      (!d || z.district === d) &&
      (!t || z.taluk === t) &&
      (!g || z.gramPanchayat === g) &&
      (!q || [z.organizationName, z.name, z.ownerName, z.code].some((v) => (v || '').toLowerCase().includes(q))));
  });
  districtOptions = computed(() => this.distinct(this.zones().map((z) => z.district)));
  talukOptions = computed(() =>
    this.distinct(this.zones().filter((z) => !this.fDistrict() || z.district === this.fDistrict()).map((z) => z.taluk)));
  gpOptions = computed(() =>
    this.distinct(this.zones().filter((z) =>
      (!this.fDistrict() || z.district === this.fDistrict()) &&
      (!this.fTaluk() || z.taluk === this.fTaluk())).map((z) => z.gramPanchayat)));

  constructor() {
    this.load();
    // Side-nav "Create Zone" opens the form via ?new=1; "View Zones" shows the list.
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((p) => (p.get('new') !== null ? this.newZone() : this.editing.set(false)));
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
    this.tabIndex.set(0);
    this.editing.set(true);
  }

  edit(z: Zone): void {
    this.form = { ...z };
    this.files = {};
    this.tabIndex.set(0);
    this.editing.set(true);
  }

  // ---- Wizard helpers ----
  private has(v: unknown): boolean {
    return v !== undefined && v !== null && String(v).trim() !== '';
  }
  private sectionFields(key: string): boolean[] {
    const f = this.form;
    switch (key) {
      case 'login':
        return f.id
          ? [this.has(f.organizationName) || this.has(f.name), this.has(f.buildingOwnership)]
          : [this.has(f.userId), this.has(f.password), this.has(f.organizationName) || this.has(f.name), this.has(f.buildingOwnership)];
      case 'owner':
        return [this.has(f.ownerName), this.has(f.contactNumber), this.has(f.email),
          this.has(f.fullAddress), this.has(f.district), this.has(f.taluk), this.has(f.gramPanchayat)];
      case 'kyc':
        // Logo + signature are mandatory — they brand every certificate & MOU.
        return [this.has(f.aadhaarNumber), this.has(f.panNumber), this.has(f.bankAccountDetails),
          this.hasDoc('logo'), this.hasDoc('authorisedSignatorySignature')];
      case 'fit':
        return [this.has(f.investmentCapacity), this.has(f.preferredLocation),
          this.has(f.spaceOwnership), this.has(f.spaceSqft), this.has(f.startTimeline)];
      case 'certificate':
        return [this.has(f.franchiseeName), this.has(f.registrationNo), this.has(f.issueDate)];
      case 'membership':
        return [this.has(f.membershipTier), f.tcAccepted === true];
      default:
        return [];
    }
  }

  /** Live "valid till" preview = issue date + 2 years (backend recomputes authoritatively). */
  validTillPreview(): string | null {
    const d = this.form.issueDate;
    if (!d) return null;
    const dt = new Date(d);
    if (isNaN(dt.getTime())) return null;
    dt.setFullYear(dt.getFullYear() + 2);
    return dt.toISOString().slice(0, 10);
  }

  /** Territory granted for the selected tier. */
  territoryPreview(): string | null {
    return this.form.membershipTier ? (MEMBERSHIP_TERRITORY[this.form.membershipTier] ?? null) : null;
  }

  /** Territory granted for a given tier name (shown on the tier cards). */
  territoryFor(tier: string): string {
    return MEMBERSHIP_TERRITORY[tier] ?? '';
  }

  downloading = signal(false);

  /** Download the personalised franchise MOU after the zone is submitted. */
  downloadMou(id: string): void {
    this.grab(this.data.zoneMou(id), `Franchise-MOU-${id}.pdf`);
  }

  /** Download the franchise certificate after the zone is submitted. */
  downloadCertificate(id: string): void {
    this.grab(this.data.zoneCertificate(id), `Franchise-Certificate-${id}.pdf`);
  }

  /** Generate + email both documents to the franchise's contact email. */
  emailDocuments(z: Zone): void {
    if (!z.id) return;
    this.snack.open('Sending certificate & MOU…', undefined, { duration: 1500 });
    this.data.sendZoneDocuments(z.id).subscribe({
      next: (r) => this.snack.open(r?.note || 'Documents sent', 'OK', { duration: 4000 }),
      error: (e) => this.snack.open(e?.error?.message || 'Send failed', 'OK', { duration: 3000 }),
    });
  }

  private grab(obs: Observable<Blob>, filename: string): void {
    this.downloading.set(true);
    obs.subscribe({
      next: (blob) => {
        this.downloading.set(false);
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = filename; a.click();
        URL.revokeObjectURL(url);
      },
      error: (e) => {
        this.downloading.set(false);
        this.snack.open(e?.error?.message || 'Download failed', 'OK', { duration: 3000 });
      },
    });
  }
  progress(key: string): number {
    const a = this.sectionFields(key);
    return a.length ? Math.round((a.filter(Boolean).length / a.length) * 100) : 0;
  }
  isComplete(key: string): boolean { return this.progress(key) === 100; }
  overall(): number {
    const a = this.sections.flatMap((s) => this.sectionFields(s.key));
    return a.length ? Math.round((a.filter(Boolean).length / a.length) * 100) : 0;
  }
  goTo(i: number): void { if (i >= 0 && i < this.sections.length) this.tabIndex.set(i); }
  get isLastTab(): boolean { return this.tabIndex() === this.sections.length - 1; }

  // ---- Filter helpers ----
  private distinct(vals: (string | undefined)[]): string[] {
    return [...new Set(vals.filter((v): v is string => !!v && v.trim() !== ''))].sort();
  }
  clearFilters(): void {
    this.fDistrict.set(''); this.fTaluk.set(''); this.fGp.set(''); this.fSearch.set('');
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

  /** Documents that are mandatory to save a zone (branding for the certificate & MOU). */
  readonly requiredDocs = ['logo', 'authorisedSignatorySignature'];
  isRequiredDoc(type: string): boolean {
    return this.requiredDocs.includes(type);
  }

  /** True if a document is provided — either uploaded in this session or already saved on the zone. */
  hasDoc(type: string): boolean {
    return !!this.files[type] || !!this.existingDocName(type);
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
    // The Authorised Signatory (Zone Head) signature is REQUIRED — it signs the MOU annexures.
    if (!this.form.id && !this.hasDoc('authorisedSignatorySignature')) {
      this.snack.open('Authorised Signatory Signature (Zone Head) is required — upload it in the Documents step.',
        'OK', { duration: 4500 });
      return;
    }
    // Logo is strongly recommended (it brands the certificate & MOU) but not mandatory — it can be
    // uploaded later; the MOU just leaves that spot blank until then.
    if (!this.form.id && !this.hasDoc('logo')) {
      this.snack.open('Tip: add the Organization Logo (Documents tab) so the certificate & MOU are fully branded.',
        'OK', { duration: 4000 });
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
        // On a new sign-up, generate + email the certificate and MOU to the franchise.
        if (res) {
          this.data.sendZoneDocuments(id).subscribe({
            next: (r) => this.snack.open(r?.note || 'Documents sent', 'OK', { duration: 4000 }),
            error: () => {},
          });
        }
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
