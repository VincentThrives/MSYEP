import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiResponse } from './models';
import { environment } from '../../environments/environment';

/** Thin wrapper that unwraps the {success,message,data} envelope. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  readonly base = environment.apiBase;

  get<T>(path: string, params?: Record<string, string | number | undefined>): Observable<T> {
    return this.http
      .get<ApiResponse<T>>(this.base + path, { params: this.toParams(params) })
      .pipe(map((r) => r.data));
  }

  post<T>(path: string, body: unknown): Observable<T> {
    return this.http.post<ApiResponse<T>>(this.base + path, body).pipe(map((r) => r.data));
  }

  put<T>(path: string, body: unknown): Observable<T> {
    return this.http.put<ApiResponse<T>>(this.base + path, body).pipe(map((r) => r.data));
  }

  delete<T>(path: string): Observable<T> {
    return this.http.delete<ApiResponse<T>>(this.base + path).pipe(map((r) => r.data));
  }

  /** Raw blob download (PDF/Excel). */
  blob(path: string): Observable<Blob> {
    return this.http.get(this.base + path, { responseType: 'blob' });
  }

  /** Multipart upload. */
  upload<T>(path: string, file: File): Observable<T> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<T>>(this.base + path, form).pipe(map((r) => r.data));
  }

  private toParams(params?: Record<string, string | number | undefined>): HttpParams {
    let p = new HttpParams();
    if (params) {
      for (const [k, v] of Object.entries(params)) {
        if (v !== undefined && v !== null && v !== '') p = p.set(k, String(v));
      }
    }
    return p;
  }
}
