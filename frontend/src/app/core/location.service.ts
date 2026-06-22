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
}
