import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { ApiService } from './api.service';

/** Cascading Karnataka location lookups (district → taluk → gram panchayat). */
@Injectable({ providedIn: 'root' })
export class LocationService {
  private api = inject(ApiService);
  private districts$?: Observable<string[]>;

  districts(): Observable<string[]> {
    if (!this.districts$) {
      this.districts$ = this.api.get<string[]>('/locations/districts').pipe(shareReplay(1));
    }
    return this.districts$;
  }

  taluks(district: string): Observable<string[]> {
    return this.api.get<string[]>('/locations/taluks', { district });
  }

  gramPanchayats(district: string, taluk: string): Observable<string[]> {
    return this.api.get<string[]>('/locations/gram-panchayats', { district, taluk });
  }

  importExcel(file: File) {
    return this.api.upload<{ added: number }>('/locations/import', file);
  }

  // ---- Admin: extend the master (new entries appear in every dropdown) ----
  addDistrict(name: string) {
    return this.api.post<string>('/locations/districts', { name });
  }
  addTaluk(district: string, taluk: string) {
    return this.api.post<void>('/locations/taluks', { district, taluk });
  }
  addGramPanchayat(district: string, taluk: string, gramPanchayat: string) {
    return this.api.post<void>('/locations/gram-panchayats', { district, taluk, gramPanchayat });
  }

  renameDistrict(oldName: string, newName: string) {
    return this.api.put<void>('/locations/districts', { oldName, newName });
  }
  renameTaluk(district: string, oldTaluk: string, newTaluk: string) {
    return this.api.put<void>('/locations/taluks', { district, oldTaluk, newTaluk });
  }
  renameGramPanchayat(district: string, taluk: string, oldGp: string, newGp: string) {
    return this.api.put<void>('/locations/gram-panchayats', { district, taluk, oldGp, newGp });
  }

  private q(params: Record<string, string>): string {
    return Object.entries(params).map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&');
  }
  deleteDistrict(name: string) {
    return this.api.delete<void>(`/locations/districts?${this.q({ name })}`);
  }
  deleteTaluk(district: string, taluk: string) {
    return this.api.delete<void>(`/locations/taluks?${this.q({ district, taluk })}`);
  }
  deleteGramPanchayat(district: string, taluk: string, gramPanchayat: string) {
    return this.api.delete<void>(`/locations/gram-panchayats?${this.q({ district, taluk, gramPanchayat })}`);
  }

  /** Bust the cached district list so freshly-added districts show up. */
  resetDistricts(): void {
    this.districts$ = undefined;
  }
}
