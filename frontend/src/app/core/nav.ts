import { Role } from './models';

export interface NavItem {
  label: string;
  path: string;
  icon: string;
}

/** Portal theme + nav per role. Theme drives the toolbar/active colors. */
export interface PortalTheme {
  name: string;
  cssClass: 'theme-admin' | 'theme-zone' | 'theme-center' | 'theme-student' | 'theme-finance';
  nav: NavItem[];
}

const ADMIN_NAV: NavItem[] = [
  { label: 'Dashboard', path: '/app/dashboard', icon: 'dashboard' },
  { label: 'Zones', path: '/app/zones', icon: 'public' },
  { label: 'Centers', path: '/app/centers', icon: 'school' },
  { label: 'Students', path: '/app/students', icon: 'groups' },
  { label: 'View Students', path: '/app/students/view', icon: 'list_alt' },
  { label: 'Download Documents', path: '/app/students/download-docs', icon: 'download' },
  { label: 'Entrance Test', path: '/app/entrance-test', icon: 'quiz' },
  { label: 'Staff Management', path: '/app/staff', icon: 'badge' },
  { label: 'Finance', path: '/app/finance', icon: 'account_balance' },
  { label: 'Logins', path: '/app/users', icon: 'manage_accounts' },
];

const ZONE_NAV: NavItem[] = [
  { label: 'Dashboard', path: '/app/dashboard', icon: 'dashboard' },
  { label: 'Centers', path: '/app/centers', icon: 'school' },
  { label: 'Students', path: '/app/students', icon: 'groups' },
  { label: 'KPMSYEP Kit Details', path: '/app/kit', icon: 'inventory_2' },
  { label: 'Staff', path: '/app/staff', icon: 'badge' },
];

const CENTER_NAV: NavItem[] = [
  { label: 'Dashboard', path: '/app/dashboard', icon: 'dashboard' },
  { label: 'Students', path: '/app/students', icon: 'groups' },
  { label: 'View Students', path: '/app/students/view', icon: 'list_alt' },
  { label: 'Download Documents', path: '/app/students/download-docs', icon: 'download' },
  { label: 'Entrance Test', path: '/app/entrance-test', icon: 'quiz' },
  { label: 'KPMSYEP SOW', path: '/app/sow', icon: 'description' },
  { label: 'VBSOW', path: '/app/vbsow', icon: 'view_module' },
  { label: 'SFOW Submissions', path: '/app/sfow', icon: 'assignment_turned_in' },
  { label: 'Finance', path: '/app/finance', icon: 'account_balance' },
];

const STUDENT_NAV: NavItem[] = [
  { label: 'My Profile', path: '/app/students', icon: 'person' },
  { label: 'Entrance Test', path: '/app/entrance-test', icon: 'quiz' },
];

const FINANCE_NAV: NavItem[] = [
  { label: 'Finance Wing', path: '/app/finance', icon: 'account_balance' },
  { label: 'Students', path: '/app/students', icon: 'groups' },
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
    default:
      return { name: 'Admin Console', cssClass: 'theme-admin', nav: ADMIN_NAV };
  }
}
