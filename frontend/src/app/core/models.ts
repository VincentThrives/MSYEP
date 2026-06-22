export type Role =
  | 'SUPER_ADMIN'
  | 'ADMIN'
  | 'ZONE'
  | 'CENTER'
  | 'STAFF'
  | 'FINANCE'
  | 'STUDENT';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface AuthResponse {
  token: string;
  userId: string;
  name: string;
  email: string;
  role: Role;
  zoneId?: string;
  centerId?: string;
  studentId?: string;
}

export interface Zone {
  id?: string;
  name: string;
  code?: string;
  district?: string;
  taluk?: string;
  gramPanchayat?: string;
  contactPerson?: string;
  contactEmail?: string;
  contactPhone?: string;
  courses?: string[];
  kitDetails?: string[];
  active?: boolean;
}

export interface CenterDocument {
  type: string;
  label: string;
  filename: string;
  size: number;
  path: string;
}

export interface Staff {
  id?: string;
  name: string;
  designation?: string;
  phone?: string;
  email?: string;
  zoneId?: string;
  centerId?: string;
  district?: string;
  taluk?: string;
  gramPanchayat?: string;
  active?: boolean;
}

export interface Center {
  id?: string;
  // Academic & type
  academicYear?: string;
  academicStartMonth?: string;
  academicEndMonth?: string;
  name: string;
  centerType?: string;
  userId?: string;
  password?: string;
  centerHeadUserId?: string;
  // Allotments (auto)
  code?: string;
  enrollmentNumber?: string;
  batchCode?: string;
  batchYear?: string;
  registrationDate?: string;
  // Location
  zoneId?: string;
  address?: string;
  locality?: string;
  pincode?: string;
  district?: string;
  taluk?: string;
  gramPanchayat?: string;
  // Contacts
  contactNumber?: string;
  email?: string;
  principalName?: string;
  principalNumber?: string;
  uucmsCoordinatorName?: string;
  uucmsCoordinatorNumber?: string;
  scstCoordinatorName?: string;
  scstCoordinatorNumber?: string;
  placementCoordinatorName?: string;
  placementCoordinatorPhone?: string;
  officeNumber?: string;
  hasWebsite?: boolean;
  websiteLink?: string;
  // Courses
  courses?: string[];
  totalStrength?: number;
  strengthTotal?: number;
  strengthSC?: number;
  strengthST?: number;
  strengthGeneral?: number;
  // MOU
  dateOfMou?: string;
  mouEndDate?: string;
  contractDuration?: string;
  documents?: CenterDocument[];
  active?: boolean;
}

export interface CenterRegistrationResult {
  center: Center;
  centerCode: string;
  enrollmentNumber: string;
  batchCode: string;
  headLoginId: string;
  emailSent: boolean;
  emailNote: string;
}

// ----- Entrance test -----
export interface EntranceQuestion {
  questionId: string;
  question: string;
  options: string[];
}
export interface EntranceStart {
  attemptId: string;
  durationMinutes: number;
  startedAt: string;
  questions: EntranceQuestion[];
}
export interface EntranceResultItem {
  question: string;
  options: string[];
  correctAnswer: string;
  selectedAnswer: string;
  correct: boolean;
}
export interface EntranceResult {
  attemptId: string;
  score: number;
  total: number;
  passed: boolean;
  items: EntranceResultItem[];
}

export const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

/** Rolling academic-year range like "2025-26", centered near the current year. */
export function academicYears(): string[] {
  const now = new Date().getFullYear();
  const out: string[] = [];
  for (let y = now - 2; y <= now + 3; y++) {
    out.push(`${y}-${String((y + 1) % 100).padStart(2, '0')}`);
  }
  return out;
}

/** Center type / college category options. */
export const CENTER_TYPES = [
  'University College', 'PUC College', 'ITI / Diploma College',
  'VTU College', 'Social Welfare Hostel', 'GTTC College',
];

export interface CenterDocSlot {
  type: string;
  label: string;
  group: string;
}

/** Document upload section headings, in display order. */
export const CENTER_DOC_GROUPS: { key: string; title: string }[] = [
  { key: 'documents', title: 'Center Documents Uploads (separate files · max 500 KB per file)' },
  { key: 'program', title: 'In Center MSYEP Program Photos' },
];

/** Document upload slots shown on the center form. */
export const CENTER_DOC_SLOTS: CenterDocSlot[] = [
  // --- Center Documents Uploads ---
  { group: 'documents', type: 'buildingBoard', label: 'Center Building Name Board photo / College Logo' },
  { group: 'documents', type: 'kitReceived', label: 'KP MSYEP Kit Received Photo' },
  { group: 'documents', type: 'requisitionSigned', label: 'Requisition received signed copy' },
  { group: 'documents', type: 'semInfoCopy', label: '3rd/4th sem · PUC · ITI · GTTC · hostels (Principal Signature) info copy' },
  { group: 'documents', type: 'mouSigned', label: 'Center MOU Signatured Copy' },
  { group: 'documents', type: 'entranceTest', label: 'Students Entrance test Writing Photo & Wrote copies' },
  // --- In Center MSYEP Program Photos ---
  { group: 'program', type: 'residentialProof', label: 'KP MSYEP selected students residentials proof / Enrollment application proof (with GP/MC/CMC/TMC/PP) Signed Copies (PDF)' },
  { group: 'program', type: 'theoryClassPhoto', label: 'KP MSYEP Theory class photo (08 Student preparation info) — attach PDF' },
  { group: 'program', type: 'practicalClassPhoto', label: 'KP MSYEP practical class photo (08 dept / resource person attend photo) — attach PDF' },
  { group: 'program', type: 'sowFilledCopy', label: 'KP MSYEP SOW Filled Copy (attach PDF)' },
  { group: 'program', type: 'studentOpinion', label: 'KP MSYEP Student Opinion Copy' },
  { group: 'program', type: 'guestOpinion', label: 'KP MSYEP Guest Opinion Copy' },
  { group: 'program', type: 'batchOpinion', label: 'KP MSYEP Batch Opinion Copy (Principal / Warden)' },
];

export interface StudentDocument {
  label: string;
  url: string;
  type?: string;
}

export interface Student {
  id?: string;
  name: string;
  phone?: string;
  email?: string;
  course?: string;
  centerId?: string;
  zoneId?: string;
  district?: string;
  taluk?: string;
  gramPanchayat?: string;
  documents?: StudentDocument[];
  active?: boolean;
}

export interface FinanceRow {
  serialNo: number;
  studentId: string;
  studentName: string;
  district: string;
  taluk: string;
  gramPanchayat: string;
  centerId: string;
  centerName: string;
  gramPanchayatEmail: string;
  documentCount: number;
}

export interface GramPanchayat {
  id?: string;
  name: string;
  taluk?: string;
  district?: string;
  email?: string;
  contactPerson?: string;
}

/** Degree/course options under a zone. */
export const COURSES = ['PU', 'SSLC', 'ITI', 'DIPLOMA', 'DEGREE'];
