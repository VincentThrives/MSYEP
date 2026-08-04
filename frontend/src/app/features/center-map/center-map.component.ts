import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

import { DataService } from '../../core/data.service';
import { AuthService } from '../../core/auth.service';
import { SearchSelectComponent } from '../../shared/search-select.component';
import { Center } from '../../core/models';

@Component({
  selector: 'app-center-map',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, SearchSelectComponent],
  template: `
    <h1>Center Map</h1>
    <p class="lead">Locate a center on the map from the address entered while creating it.</p>

    <div class="pick">
      <app-search-select label="Select Center" [options]="centers()" valueKey="id" labelKey="name"
        [value]="selectedId()" (valueChange)="selectedId.set($event)"></app-search-select>
      <span class="count">{{ centers().length }} center(s)</span>
    </div>

    <div class="empty" *ngIf="!centers().length">
      <mat-icon>location_off</mat-icon> No centers found for your login yet.
    </div>

    <div class="panel" *ngIf="selected() as c">
      <div class="info">
        <h2>{{ c.name }}</h2>
        <p class="addr"><mat-icon>place</mat-icon>{{ fullAddress(c) || 'No address on file' }}</p>
        <a *ngIf="fullAddress(c)" mat-stroked-button color="primary" [href]="externalUrl(c)" target="_blank" rel="noopener">
          <mat-icon>open_in_new</mat-icon> Open in Google Maps
        </a>
      </div>
      <div class="map" *ngIf="fullAddress(c); else noAddr">
        <iframe [src]="mapUrl(c)" title="Center location" loading="lazy"
          referrerpolicy="no-referrer-when-downgrade"></iframe>
      </div>
      <ng-template #noAddr>
        <div class="map noaddr"><mat-icon>map</mat-icon>
          Add a Center Address (Center Location tab) to see it on the map.</div>
      </ng-template>
    </div>
  `,
  styles: [`
    :host { display: block; padding: 8px 4px; }
    h1 { margin: 0 0 4px; }
    .lead { color: #6b6b6b; margin: 0 0 18px; }
    .pick { display: flex; align-items: center; gap: 14px; max-width: 560px; }
    .pick app-search-select { flex: 1; }
    .count { font-size: 12px; color: #888; white-space: nowrap; }
    .empty { display: flex; align-items: center; gap: 8px; color: #888; margin-top: 24px; }
    .panel { margin-top: 18px; border: 1px solid #e6e6e6; border-radius: 14px; overflow: hidden; background: #fff; }
    .info { padding: 16px 20px; border-bottom: 1px solid #eee; }
    .info h2 { margin: 0 0 6px; font-size: 18px; color: #0E5132; }
    .addr { display: flex; align-items: flex-start; gap: 6px; color: #555; margin: 0 0 12px; font-size: 14px; }
    .addr mat-icon { color: #C9A227; font-size: 19px; width: 19px; height: 19px; }
    .map { height: 440px; }
    .map iframe { width: 100%; height: 100%; border: 0; display: block; }
    .map.noaddr { display: flex; align-items: center; justify-content: center; gap: 8px;
      color: #999; background: #fafafa; }
  `],
})
export class CenterMapComponent {
  private data = inject(DataService);
  private auth = inject(AuthService);
  private sanitizer = inject(DomSanitizer);

  centers = signal<Center[]>([]);
  selectedId = signal<string | undefined>(undefined);
  selected = computed(() => this.centers().find((c) => c.id === this.selectedId()));

  constructor() {
    this.data.centers().subscribe((list) => {
      const u = this.auth.user();
      let scoped = list || [];
      // Scope to what the login can see; admin/staff see everything.
      if (u?.role === 'ZONE' && u.zoneId) scoped = scoped.filter((c) => c.zoneId === u.zoneId);
      else if (u?.role === 'CENTER' && u.centerId) scoped = scoped.filter((c) => c.id === u.centerId);
      this.centers.set(scoped);
      if (scoped.length) this.selectedId.set(scoped[0].id);
    });
  }

  /** Build a human-readable address from the parts captured at center creation. */
  fullAddress(c: Center): string {
    return [c.address, c.locality, c.gramPanchayat, c.taluk, c.district, 'Karnataka', c.pincode]
      .filter((p) => p && String(p).trim()).join(', ');
  }

  mapUrl(c: Center): SafeResourceUrl {
    const q = encodeURIComponent(this.fullAddress(c));
    return this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://maps.google.com/maps?q=${q}&z=14&hl=en&output=embed`);
  }

  externalUrl(c: Center): string {
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(this.fullAddress(c))}`;
  }
}
