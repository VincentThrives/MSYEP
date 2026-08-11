import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Center, CenterRegistrationResult, EntranceResult, EntranceStart, FinanceRow, GramPanchayat, MailLog, Staff, Student, StudentRegistrationResult, Zone, ZoneRegistrationResult } from './models';

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
  zoneMou = (id: string) => this.api.blob(`/zones/${id}/mou`);
  zoneCertificate = (id: string) => this.api.blob(`/zones/${id}/certificate`);
  sendZoneDocuments = (id: string) => this.api.post<{ note: string }>(`/zones/${id}/send-documents`, {});
  zoneSendMail = (body: { zoneIds: string[]; subject?: string; body?: string }) =>
    this.api.post<Record<string, string>>('/zones/send-mail', body);
  zoneMailHistory = () => this.api.get<MailLog[]>('/zones/mail-history');

  // Franchise settings (one-time admin assets)
  franchiseSettings = () =>
    this.api.get<{ giverSignature: boolean; approvalSignature: boolean }>('/franchise-settings');
  uploadGiverSignature = (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return this.api.post<{ giverSignature: boolean }>('/franchise-settings/giver-signature', form);
  };
  removeGiverSignature = () => this.api.delete<{ giverSignature: boolean }>('/franchise-settings/giver-signature');
  uploadApprovalSignature = (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return this.api.post<{ approvalSignature: boolean }>('/franchise-settings/approval-signature', form);
  };
  removeApprovalSignature = () =>
    this.api.delete<{ approvalSignature: boolean }>('/franchise-settings/approval-signature');
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
  centerBatchApprovalPdf = (id: string) => this.api.blob(`/centers/${id}/batch-approval-pdf`);
  centerSendMail = (body: { centerIds: string[]; subject?: string; body?: string }) =>
    this.api.post<Record<string, string>>('/centers/send-mail', body);
  centerMailHistory = () => this.api.get<MailLog[]>('/centers/mail-history');
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
  getStudent = (id: string): Observable<Student> => this.api.get<Student>(`/students/${id}`);
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
  studentsFilter = (f: Record<string, string | undefined>) => this.api.get<Student[]>('/students/filter', f);
  studentsExportExcel = (f: Record<string, string | undefined>) => this.api.blob('/students/export', f);
  studentDocumentsPdf = (studentIds: string[], docType?: string) =>
    this.api.blobPost('/students/documents-pdf', { studentIds, docType });
  studentDocumentsZip = (studentIds: string[], docType?: string) =>
    this.api.blobPost('/students/documents-zip', { studentIds, docType });

  // Finance
  financeStudents = (f: {
    district?: string; taluk?: string; gramPanchayat?: string; centerId?: string;
  }): Observable<FinanceRow[]> => this.api.get<FinanceRow[]>('/finance/students', f);
  financeFilters = () =>
    this.api.get<{ districts: string[]; taluks: string[]; gramPanchayats: string[] }>('/finance/filters');
  gpEmail = (gramPanchayat: string) =>
    this.api.get<{ email: string }>('/finance/gp-email', { gramPanchayat });
  sendMail = (body: {
    studentIds: string[]; overrideEmail?: string; recipientEmails?: string[]; subject?: string; body?: string;
    gramPanchayat?: string; taluk?: string; district?: string;
  }) => this.api.post<Record<string, string>>('/finance/send-mail', body);
  /** GP Blue Print financial packet (cover + Kannada requisition letter + invoice) for a GP. */
  gpBlueprintPdf = (f: { gramPanchayat?: string; taluk?: string; district?: string }) =>
    this.api.blob('/finance/gp-blueprint-pdf', f);
  gramPanchayats = () => this.api.get<GramPanchayat[]>('/finance/gram-panchayats');
  financeMailHistory = () => this.api.get<MailLog[]>('/finance/mail-history');
  saveGramPanchayat = (gp: GramPanchayat) => this.api.post<GramPanchayat>('/finance/gram-panchayats', gp);
  deleteGramPanchayat = (id: string) => this.api.delete<void>(`/finance/gram-panchayats/${id}`);

  // Student CV / Resume (₹90)
  cvStatus = () => this.api.get<any>('/cv/status');
  cvOrder = () => this.api.post<any>('/cv/order', {});
  cvVerify = (body: { orderId: string; paymentId: string; signature: string }) =>
    this.api.post<boolean>('/cv/verify', body);
  cvDownload = () => this.api.blob('/cv/download');
  /** Admin: fetch any student's resume (no payment gate). inline=true for in-browser view. */
  cvBlobFor = (studentId: string, inline = false) =>
    this.api.blob('/cv/download', { studentId, inline: inline ? 'true' : 'false' });

  /** Append ?centerId= when an admin/zone acts on a specific center (blank for a center's own). */
  private cq = (centerId?: string) => (centerId ? `?centerId=${encodeURIComponent(centerId)}` : '');

  // KP-MSYEP SOW
  sowList = (centerId?: string) => this.api.get<any[]>('/sow/list', { centerId });
  sowGet = (programIndex: number, centerId?: string) =>
    this.api.get<any>(`/sow/${programIndex}`, { centerId });
  sowSave = (programIndex: number, body: { fields: Record<string, string>; photos: Record<string, string> }, centerId?: string) =>
    this.api.post<any>(`/sow/${programIndex}${this.cq(centerId)}`, body);
  sowDownload = (programIndex: number, centerId?: string) =>
    this.api.blobPost(`/sow/${programIndex}/download${this.cq(centerId)}`, {});
  sowDownloadAll = (centerId?: string) =>
    this.api.blobPost(`/sow/download-all${this.cq(centerId)}`, {});

  // Resource persons (center)
  resourcePersonsGet = (centerId?: string) => this.api.get<any>('/resource-persons', { centerId });
  resourcePersonsSave = (body: { countRequired: number; persons: any[] }, centerId?: string) =>
    this.api.post<any>(`/resource-persons${this.cq(centerId)}`, body);
  resourcePersonsLetter = (centerId?: string) =>
    this.api.blobPost(`/resource-persons/letter${this.cq(centerId)}`, {});

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
  /** The student's completed result, or null if not taken yet. */
  entranceResult = (studentId: string) =>
    this.api.get<EntranceResult | null>('/entrance-test/result', { studentId });

  // Users
  users = () => this.api.get<any[]>('/users');
  createUser = (u: any) => this.api.post<any>('/users', u);
  deleteUser = (id: string) => this.api.delete<void>(`/users/${id}`);
}
