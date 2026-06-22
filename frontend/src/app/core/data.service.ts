import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Center, CenterRegistrationResult, FinanceRow, GramPanchayat, Staff, Student, Zone } from './models';

@Injectable({ providedIn: 'root' })
export class DataService {
  private api = inject(ApiService);

  // Zones
  zones = (): Observable<Zone[]> => this.api.get<Zone[]>('/zones');
  zone = (id: string) => this.api.get<Zone>(`/zones/${id}`);
  createZone = (z: Zone) => this.api.post<Zone>('/zones', z);
  updateZone = (id: string, z: Zone) => this.api.put<Zone>(`/zones/${id}`, z);
  deleteZone = (id: string) => this.api.delete<void>(`/zones/${id}`);
  importZones = (file: File) => this.api.upload<{ imported: number }>('/zones/import', file);
  zonePdf = (id: string) => this.api.blob(`/zones/${id}/pdf`);

  // Centers
  centers = (zoneId?: string): Observable<Center[]> =>
    this.api.get<Center[]>('/centers', { zoneId });
  createCenter = (c: Center) => this.api.post<CenterRegistrationResult>('/centers', c);
  updateCenter = (id: string, c: Center) => this.api.put<Center>(`/centers/${id}`, c);
  deleteCenter = (id: string) => this.api.delete<void>(`/centers/${id}`);
  centerRegistrationPdf = (id: string) => this.api.blob(`/centers/${id}/registration-pdf`);
  uploadCenterDocument = (id: string, type: string, label: string, file: File) => {
    const form = new FormData();
    form.append('type', type);
    form.append('label', label);
    form.append('file', file);
    return this.api.post<Center>(`/centers/${id}/documents`, form);
  };

  // Students
  students = (centerId?: string): Observable<Student[]> =>
    this.api.get<Student[]>('/students', { centerId });
  createStudent = (s: Student) => this.api.post<Student>('/students', s);
  updateStudent = (id: string, s: Student) => this.api.put<Student>(`/students/${id}`, s);
  deleteStudent = (id: string) => this.api.delete<void>(`/students/${id}`);

  // Finance
  financeStudents = (f: {
    district?: string; taluk?: string; gramPanchayat?: string; centerId?: string;
  }): Observable<FinanceRow[]> => this.api.get<FinanceRow[]>('/finance/students', f);
  financeFilters = () =>
    this.api.get<{ districts: string[]; taluks: string[]; gramPanchayats: string[] }>('/finance/filters');
  gpEmail = (gramPanchayat: string) =>
    this.api.get<{ email: string }>('/finance/gp-email', { gramPanchayat });
  sendMail = (body: { studentIds: string[]; overrideEmail?: string; subject?: string; body?: string }) =>
    this.api.post<Record<string, string>>('/finance/send-mail', body);
  gramPanchayats = () => this.api.get<GramPanchayat[]>('/finance/gram-panchayats');
  saveGramPanchayat = (gp: GramPanchayat) => this.api.post<GramPanchayat>('/finance/gram-panchayats', gp);
  deleteGramPanchayat = (id: string) => this.api.delete<void>(`/finance/gram-panchayats/${id}`);

  // Staff
  staff = () => this.api.get<Staff[]>('/staff');
  createStaff = (s: Staff) => this.api.post<Staff>('/staff', s);
  updateStaff = (id: string, s: Staff) => this.api.put<Staff>(`/staff/${id}`, s);
  deleteStaff = (id: string) => this.api.delete<void>(`/staff/${id}`);

  // Users
  users = () => this.api.get<any[]>('/users');
  createUser = (u: any) => this.api.post<any>('/users', u);
  deleteUser = (id: string) => this.api.delete<void>(`/users/${id}`);
}
