import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Center, CenterRegistrationResult, EntranceResult, EntranceStart, FinanceRow, GramPanchayat, Staff, Student, StudentRegistrationResult, Zone, ZoneRegistrationResult } from './models';

@Injectable({ providedIn: 'root' })
export class DataService {
  private api = inject(ApiService);

  // Zones
  zones = (): Observable<Zone[]> => this.api.get<Zone[]>('/zones');
  zone = (id: string) => this.api.get<Zone>(`/zones/${id}`);
  createZone = (z: Zone) => this.api.post<ZoneRegistrationResult>('/zones', z);
  updateZone = (id: string, z: Zone) => this.api.put<Zone>(`/zones/${id}`, z);
  deleteZone = (id: string) => this.api.delete<void>(`/zones/${id}`);
  importZones = (file: File) => this.api.upload<{ imported: number }>('/zones/import', file);
  zonePdf = (id: string) => this.api.blob(`/zones/${id}/pdf`);
  uploadZoneDocument = (id: string, type: string, label: string, file: File) => {
    const form = new FormData();
    form.append('type', type);
    form.append('label', label);
    form.append('file', file);
    return this.api.post<Zone>(`/zones/${id}/documents`, form);
  };

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
  createStudent = (s: Student) => this.api.post<StudentRegistrationResult>('/students', s);
  updateStudent = (id: string, s: Student) => this.api.put<Student>(`/students/${id}`, s);
  deleteStudent = (id: string) => this.api.delete<void>(`/students/${id}`);
  uploadStudentDocument = (id: string, type: string, label: string, file: File) => {
    const form = new FormData();
    form.append('type', type);
    form.append('label', label);
    form.append('file', file);
    return this.api.post<Student>(`/students/${id}/documents`, form);
  };

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

  // Entrance test
  entranceStart = (studentId: string, selfie: File) => {
    const form = new FormData();
    form.append('studentId', studentId);
    form.append('selfie', selfie);
    return this.api.post<EntranceStart>('/entrance-test/start', form);
  };
  entranceSubmit = (attemptId: string, answers: Record<string, string>) =>
    this.api.post<EntranceResult>(`/entrance-test/${attemptId}/submit`, { answers });
  entranceResultPdf = (attemptId: string) =>
    this.api.blob(`/entrance-test/${attemptId}/result-pdf`);
  entranceAttempts = (studentId: string) =>
    this.api.get<any[]>('/entrance-test/attempts', { studentId });

  // Users
  users = () => this.api.get<any[]>('/users');
  createUser = (u: any) => this.api.post<any>('/users', u);
  deleteUser = (id: string) => this.api.delete<void>(`/users/${id}`);
}
