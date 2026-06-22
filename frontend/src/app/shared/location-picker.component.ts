import { Component, OnInit, inject, input, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { LocationService } from '../core/location.service';
import { SearchSelectComponent } from './search-select.component';

/**
 * Cascading Karnataka location picker: District → Taluk → Gram Panchayat.
 * Two-way binds the three values; GP falls back to a free-text input
 * if no GP master is loaded for the selected taluk.
 *
 * <app-location-picker [(district)]="x.district" [(taluk)]="x.taluk"
 *   [(gramPanchayat)]="x.gramPanchayat"></app-location-picker>
 */
@Component({
  selector: 'app-location-picker',
  standalone: true,
  imports: [CommonModule, MatFormFieldModule, MatInputModule, SearchSelectComponent],
  templateUrl: './location-picker.component.html',
  styleUrl: './location-picker.component.scss',
})
export class LocationPickerComponent implements OnInit {
  private loc = inject(LocationService);

  district = model<string | undefined>(undefined);
  taluk = model<string | undefined>(undefined);
  gramPanchayat = model<string | undefined>(undefined);

  districtLabel = input('District');
  talukLabel = input('Taluk / Town');
  gpLabel = input('Village / Gram Panchayat');

  districts = signal<string[]>([]);
  taluks = signal<string[]>([]);
  gps = signal<string[]>([]);

  ngOnInit(): void {
    this.loc.districts().subscribe((d) => this.districts.set(d));
    if (this.district()) {
      this.loc.taluks(this.district()!).subscribe((t) => this.taluks.set(t));
    }
    if (this.district() && this.taluk()) {
      this.loc.gramPanchayats(this.district()!, this.taluk()!).subscribe((g) => this.gps.set(g));
    }
  }

  onDistrict(v: string): void {
    this.district.set(v);
    this.taluk.set(undefined);
    this.gramPanchayat.set(undefined);
    this.taluks.set([]);
    this.gps.set([]);
    if (v) this.loc.taluks(v).subscribe((t) => this.taluks.set(t));
  }

  onTaluk(v: string): void {
    this.taluk.set(v);
    this.gramPanchayat.set(undefined);
    this.gps.set([]);
    if (v && this.district()) {
      this.loc.gramPanchayats(this.district()!, v).subscribe((g) => this.gps.set(g));
    }
  }

  onGp(v: string): void {
    this.gramPanchayat.set(v);
  }
}
