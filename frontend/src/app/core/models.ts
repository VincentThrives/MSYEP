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

export interface ZoneDocument {
  type: string;
  label: string;
  filename: string;
  size: number;
  path: string;
}

export interface Zone {
  id?: string;
  name: string;
  code?: string;
  // Login
  userId?: string;
  password?: string;
  // Organization
  organizationName?: string;
  hasWebsite?: boolean;
  websiteLink?: string;
  websiteBudget?: string;
  buildingOwnership?: string;
  hasRegisteredCopy?: boolean;
  hasMsme?: boolean;
  hasGst?: boolean;
  hasNitiAayog?: boolean;
  hasNgoDarpan?: boolean;
  has12A80G?: boolean;
  willingToComply?: boolean;
  // Owner
  ownerName?: string;
  ownerDob?: string;
  ownerGender?: string;
  contactNumber?: string;
  alternateNumber?: string;
  email?: string;
  fullAddress?: string;
  city?: string;
  state?: string;
  pincode?: string;
  occupation?: string;
  educationalQualification?: string;
  // KYC
  aadhaarNumber?: string;
  panNumber?: string;
  bankAccountDetails?: string;
  // Business fit
  investmentCapacity?: string;
  preferredLocation?: string;
  spaceOwnership?: string;
  spaceSqft?: string;
  startTimeline?: string;
  // Membership
  membershipTier?: string;
  membershipAmount?: number;
  tcAccepted?: boolean;
  status?: string;
  registrationDate?: string;
  // existing geo/contact
  district?: string;
  taluk?: string;
  gramPanchayat?: string;
  contactPerson?: string;
  contactEmail?: string;
  contactPhone?: string;
  courses?: string[];
  kitDetails?: string[];
  documents?: ZoneDocument[];
  active?: boolean;
}

export interface ZoneRegistrationResult {
  zone: Zone;
  zoneCode: string;
  loginId: string;
  status: string;
  membershipAmount: number;
  note: string;
}

export const GENDERS = ['Male', 'Female', 'Other'];
export const OWN_RENT = ['Own', 'Rent'];
export const INVESTMENT_CAPACITY = ['Below 1 Lakh', '1–5 Lakhs', '5–10 Lakhs', 'Above 10 Lakhs'];
export const START_TIMELINE = ['Immediately', 'Within 3 months', 'Within 6 months', 'Later'];

export const MEMBERSHIP_TIERS: { name: string; amount: number }[] = [
  { name: 'Silver', amount: 50000 },
  { name: 'Gold', amount: 75000 },
  { name: 'Platinum', amount: 100000 },
  { name: 'Diamond', amount: 125000 },
];

/** Franchise document upload slots. */
export const ZONE_DOC_SLOTS: { type: string; label: string }[] = [
  { type: 'aadhaar', label: 'Aadhaar Card Photo' },
  { type: 'pan', label: 'PAN Card Photo' },
  { type: 'bank', label: 'Bank Account Details Photo' },
  { type: 'logo', label: 'Organization Logo' },
  { type: 'registeredCopy', label: 'Organization Registered Copy' },
  { type: 'msme', label: 'MSME Copy' },
  { type: 'gst', label: 'GST Copy' },
  { type: 'nitiAayog', label: 'Niti Aayog Copy' },
  { type: 'ngoDarpan', label: 'NGO Darpan Copy' },
  { type: 'doc12A80G', label: '12A / 80G Copy' },
  { type: 'building', label: 'Building (Own / Rent) Copy' },
];

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
  type: string;
  label: string;
  filename: string;
  size: number;
  path: string;
}

export interface Student {
  id?: string;
  // Account
  name: string;
  email?: string;
  phone?: string;
  userId?: string;
  password?: string;
  course?: string;
  // Personal
  gender?: string;
  dateOfBirth?: string;
  caste?: string;
  // Education & job
  educationalQualification?: string;
  admissionYear?: string;
  interestedInternship?: string;
  technicalSkills?: string;
  hobbies?: string[];
  interestedCourses?: string[];
  careerGoal?: string;
  centerId?: string;
  zoneId?: string;
  // Address
  state?: string;
  district?: string;
  taluk?: string;
  gramPanchayat?: string;
  pincode?: string;
  postalAddress?: string;
  nativePlace?: string;
  hostelName?: string;
  // Allotments
  registerNo?: string;
  batchCode?: string;
  courseJoiningDate?: string;
  collegeName?: string;
  documents?: StudentDocument[];
  active?: boolean;
}

export interface StudentRegistrationResult {
  student: Student;
  registerNo: string;
  batchCode: string;
  loginId: string;
  note: string;
}

export const CASTES = ['General', 'SC', 'ST', 'Category-1', 'Category-2A', 'Category-2B', 'Category-3A', 'Category-3B', 'Others'];
export const HOBBIES = ['Music', 'Classical Dancing', 'Acting', 'No'];
export const YES_NO = ['Yes', 'No'];

export const STUDENT_DOC_SLOTS: { type: string; label: string }[] = [
  { type: 'passportPhoto', label: "Student's Passport Size Photo" },
  { type: 'aadhaar', label: "Student's Aadhar Copy" },
  { type: 'pancard', label: "Student's Pancard Copy" },
  { type: 'sslcMarks', label: "Student's SSLC Marks Card Copy" },
  { type: 'pucDiplomaMarks', label: "Student's PUC / Diploma Marks Card Copy" },
  { type: 'feeReceipt', label: 'If Studying, Present Year College Fee Paid Receipt' },
  { type: 'casteCertificate', label: "Student's Caste Certificate" },
];

/** Interested-courses catalog (grouped), from the KP-MSYEP student form. */
export const COURSE_CATALOG: { group: string; items: string[] }[] = [
  { group: 'Basic Level Technical Computer Courses', items: [
    'Computer Fundamentals (Basics of Hardware & Software)', 'MS Office (Word, Excel, PowerPoint, Access)',
    'Internet & Email Handling', 'Typing & Data Entry', 'Digital Literacy (NIELIT CCC / BCC)'] },
  { group: 'Programming & Development', items: [
    'C, C++ Programming', 'Java / Advanced Java', 'Python Programming',
    '.NET Framework (C#, ASP.NET, VB.NET)', 'PHP & MySQL', 'Full Stack Development (MERN / MEAN)',
    'Mobile App Development (Android / iOS)', 'Web Designing (HTML, CSS, JavaScript, Bootstrap)'] },
  { group: 'Software & Database', items: [
    'Database Management (SQL, Oracle, MongoDB)', 'Software Engineering & Testing (Manual & Automation)',
    'ERP (SAP, Tally Prime with GST, Odoo Basics)', 'Cloud Computing (AWS, Azure, Google Cloud)'] },
  { group: 'Networking & Hardware', items: [
    'Computer Hardware & Maintenance', 'Networking (CCNA, CCNP)', 'Ethical Hacking & Cybersecurity',
    'System Administration (Linux / Windows Server)', 'Cloud Networking'] },
  { group: 'Multimedia & Design', items: [
    'Graphic Design (Photoshop, Illustrator, CorelDRAW)', 'Video Editing (Premiere Pro, Final Cut, DaVinci Resolve)',
    '2D / 3D Animation (Maya, Blender, After Effects)', 'UI/UX Designing', 'AutoCAD (Mechanical / Civil / Electrical)'] },
  { group: 'Specialized / Job-Oriented Courses', items: [
    'Artificial Intelligence (AI) & Machine Learning', 'Data Science & Big Data Analytics', 'Blockchain Technology',
    'Internet of Things (IoT)', 'Cyber Forensics', 'Cloud Security', 'Robotics & Embedded Systems'] },
  { group: 'Short-Term Certificate / Diploma Courses', items: [
    'Diploma in Computer Applications (DCA)'] },
];

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
