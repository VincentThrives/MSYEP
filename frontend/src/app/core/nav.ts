import { Role } from './models';

export interface NavItem {
  label: string;
  /** Route to navigate to. Omitted for a group header that only expands its children. */
  path?: string;
  icon: string;
  /** Optional query params for the route (e.g. which profile tab to open). */
  queryParams?: Record<string, string | number>;
  /** When present, this item is a collapsible group rendering these sub-items. */
  children?: NavItem[];
}

/** Portal theme + nav per role. Theme drives the toolbar/active colors. */
export interface PortalTheme {
  name: string;
  cssClass: 'theme-admin' | 'theme-zone' | 'theme-center' | 'theme-student' | 'theme-finance' | 'theme-staff';
  nav: NavItem[];
}

// Reusable collapsible groups (Create opens the form via ?new=1; View shows the list).
const ZONES_GROUP: NavItem = {
  label: 'Zones', icon: 'public', children: [
    { label: 'Create Zone', path: '/app/zones', icon: 'add_business', queryParams: { new: 1 } },
    { label: 'View Zones', path: '/app/zones', icon: 'list_alt' },
    { label: 'Mail', path: '/app/zone-mail', icon: 'mail' },
  ],
};
const CENTERS_GROUP: NavItem = {
  label: 'Centers', icon: 'school', children: [
    { label: 'Create Center', path: '/app/centers', icon: 'add_home_work', queryParams: { new: 1 } },
    { label: 'View Centers', path: '/app/centers', icon: 'list_alt' },
    { label: 'Center Map', path: '/app/center-map', icon: 'map' },
    { label: 'Mail', path: '/app/center-mail', icon: 'mail' },
  ],
};
const STAFF_GROUP: NavItem = {
  label: 'Staff Management', icon: 'badge', children: [
    { label: 'Add Staff', path: '/app/staff', icon: 'person_add', queryParams: { new: 1 } },
    { label: 'View Staff', path: '/app/staff', icon: 'list_alt' },
  ],
};
// Finance wing + the GP-mail management page.
const FINANCE_GROUP: NavItem = {
  label: 'Finance', icon: 'account_balance', children: [
    { label: 'Finance Wing', path: '/app/finance', icon: 'request_quote' },
    { label: 'Mail', path: '/app/finance-mail', icon: 'mail' },
  ],
};
const STUDENTS_GROUP: NavItem = {
  label: 'Students', icon: 'groups', children: [
    { label: 'Create Student', path: '/app/students', icon: 'person_add' },
    { label: 'View Students', path: '/app/students/view', icon: 'list_alt' },
    { label: 'Download Documents', path: '/app/students/download-docs', icon: 'download' },
    { label: 'Mail', path: '/app/student-mail', icon: 'mail' },
  ],
};

const ADMIN_NAV: NavItem[] = [
  { label: 'Dashboard', path: '/app/dashboard', icon: 'dashboard' },
  ZONES_GROUP,
  CENTERS_GROUP,
  STUDENTS_GROUP,
  STAFF_GROUP,
  { label: 'KPMSYEP SOW', path: '/app/sow', icon: 'description' },
  { label: 'Resource Person', path: '/app/resource-persons', icon: 'groups_2' },
  { label: 'Location', path: '/app/location', icon: 'place' },
  FINANCE_GROUP,
  { label: 'Franchise Settings', path: '/app/franchise-settings', icon: 'draw' },
  { label: 'Logins', path: '/app/users', icon: 'manage_accounts' },
];

const ZONE_NAV: NavItem[] = [
  { label: 'Dashboard', path: '/app/dashboard', icon: 'dashboard' },
  {
    label: 'Center Management', icon: 'school', children: [
      { label: 'Center Create', path: '/app/centers', icon: 'add_home_work', queryParams: { new: 1 } },
      { label: 'Center View', path: '/app/centers', icon: 'list_alt' },
      { label: 'Center Map', path: '/app/center-map', icon: 'map' },
    ],
  },
  {
    label: 'Students Management', icon: 'groups', children: [
      { label: 'Students Create', path: '/app/students', icon: 'person_add' },
      { label: 'Students View', path: '/app/students/view', icon: 'list_alt' },
    ],
  },
  { label: 'Fund Approval', path: '/app/fund-approval', icon: 'verified' },
  { label: 'KPMSYEP SOW', path: '/app/sow', icon: 'description' },
  { label: 'Resource Person', path: '/app/resource-persons', icon: 'groups_2' },
  { label: 'KPMSYEP Kit Details', path: '/app/kit', icon: 'inventory_2' },
  STAFF_GROUP,
];

const CENTER_NAV: NavItem[] = [
  { label: 'Dashboard', path: '/app/dashboard', icon: 'dashboard' },
  {
    label: 'My Center', icon: 'school', children: [
      { label: 'Create Center', path: '/app/centers', icon: 'add_home_work', queryParams: { new: 1 } },
      { label: 'View Center', path: '/app/centers', icon: 'list_alt' },
      { label: 'Center Map', path: '/app/center-map', icon: 'map' },
    ],
  },
  {
    label: 'Students', icon: 'groups', children: [
      { label: 'Create Student', path: '/app/students', icon: 'person_add' },
      { label: 'View Students', path: '/app/students/view', icon: 'list_alt' },
      { label: 'Download Documents', path: '/app/students/download-docs', icon: 'download' },
    ],
  },
  { label: 'KPMSYEP SOW', path: '/app/sow', icon: 'description' },
  { label: 'Resource Person', path: '/app/resource-persons', icon: 'groups_2' },
];

const STUDENT_NAV: NavItem[] = [
  {
    label: 'My Profile', icon: 'person', children: [
      { label: 'Account', path: '/app/students', icon: 'account_circle', queryParams: { tab: 0 } },
      { label: 'Personal Details', path: '/app/students', icon: 'badge', queryParams: { tab: 1 } },
      { label: 'Education', path: '/app/students', icon: 'school', queryParams: { tab: 2 } },
      { label: 'Job', path: '/app/students', icon: 'work', queryParams: { tab: 3 } },
      { label: 'Address', path: '/app/students', icon: 'home', queryParams: { tab: 4 } },
      { label: 'MSYEP Allotments', path: '/app/students', icon: 'assignment_turned_in', queryParams: { tab: 5 } },
      { label: 'Documents', path: '/app/students', icon: 'folder', queryParams: { tab: 6 } },
    ],
  },
  { label: 'My Resume / CV', path: '/app/resume', icon: 'description' },
  { label: 'Entrance Test', path: '/app/entrance-test', icon: 'quiz' },
];

const STAFF_NAV: NavItem[] = [
  { label: 'Dashboard', path: '/app/dashboard', icon: 'dashboard' },
  ZONES_GROUP,
  CENTERS_GROUP,
  STUDENTS_GROUP,
  { label: 'KPMSYEP SOW', path: '/app/sow', icon: 'description' },
  { label: 'Resource Person', path: '/app/resource-persons', icon: 'groups_2' },
  { label: 'Location', path: '/app/location', icon: 'place' },
  FINANCE_GROUP,
  STAFF_GROUP,
];

const FINANCE_NAV: NavItem[] = [
  FINANCE_GROUP,
  {
    label: 'Students', icon: 'groups', children: [
      { label: 'Create Student', path: '/app/students', icon: 'person_add' },
      { label: 'View Students', path: '/app/students/view', icon: 'list_alt' },
    ],
  },
];

export function portalFor(role: Role): PortalTheme {
  switch (role) {
    case 'ZONE':
      return { name: 'Zone Portal', cssClass: 'theme-zone', nav: ZONE_NAV };
    case 'CENTER':
      return { name: 'Center Portal', cssClass: 'theme-center', nav: CENTER_NAV };
    case 'STUDENT':
      return { name: 'Student Portal', cssClass: 'theme-student', nav: STUDENT_NAV };
    case 'FINANCE':
      return { name: 'Finance Wing', cssClass: 'theme-finance', nav: FINANCE_NAV };
    case 'STAFF':
      return { name: 'Staff Portal', cssClass: 'theme-staff', nav: STAFF_NAV };
    default:
      return { name: 'Admin Console', cssClass: 'theme-admin', nav: ADMIN_NAV };
  }
}
