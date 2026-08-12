import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/login/student-register.component').then((m) => m.StudentRegisterComponent),
  },
  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'zones',
        canActivate: [roleGuard('ADMIN', 'STAFF')],
        loadComponent: () =>
          import('./features/zones/zone-list.component').then((m) => m.ZoneListComponent),
      },
      {
        path: 'zone-mail',
        canActivate: [roleGuard('ADMIN', 'STAFF')],
        loadComponent: () =>
          import('./features/zones/zone-mail.component').then((m) => m.ZoneMailComponent),
      },
      {
        path: 'center-mail',
        canActivate: [roleGuard('ADMIN', 'STAFF')],
        loadComponent: () =>
          import('./features/centers/center-mail.component').then((m) => m.CenterMailComponent),
      },
      {
        path: 'student-mail',
        canActivate: [roleGuard('ADMIN', 'STAFF')],
        loadComponent: () =>
          import('./features/students/student-mail.component').then((m) => m.StudentMailComponent),
      },
      {
        path: 'centers',
        canActivate: [roleGuard('ADMIN', 'ZONE', 'CENTER', 'STAFF')],
        loadComponent: () =>
          import('./features/centers/center-list.component').then((m) => m.CenterListComponent),
      },
      {
        path: 'center-map',
        canActivate: [roleGuard('ADMIN', 'ZONE', 'CENTER', 'STAFF')],
        loadComponent: () =>
          import('./features/center-map/center-map.component').then((m) => m.CenterMapComponent),
      },
      {
        path: 'students',
        loadComponent: () =>
          import('./features/students/student-list.component').then((m) => m.StudentListComponent),
      },
      {
        path: 'students/view',
        loadComponent: () =>
          import('./features/students/view-students.component').then((m) => m.ViewStudentsComponent),
      },
      {
        path: 'students/download-docs',
        loadComponent: () =>
          import('./features/students/download-documents.component').then((m) => m.DownloadDocumentsComponent),
      },
      {
        path: 'fund-approval',
        canActivate: [roleGuard('ADMIN', 'ZONE')],
        data: { title: 'Fund Approval' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
      },
      {
        path: 'finance',
        canActivate: [roleGuard('ADMIN', 'FINANCE', 'STAFF')],
        loadComponent: () =>
          import('./features/finance/finance-table.component').then((m) => m.FinanceTableComponent),
      },
      {
        path: 'finance-mail',
        canActivate: [roleGuard('ADMIN', 'FINANCE', 'STAFF')],
        loadComponent: () =>
          import('./features/finance/finance-mail.component').then((m) => m.FinanceMailComponent),
      },
      {
        path: 'users',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () =>
          import('./features/users/users.component').then((m) => m.UsersComponent),
      },
      {
        path: 'staff',
        canActivate: [roleGuard('ADMIN', 'STAFF', 'ZONE', 'CENTER')],
        loadComponent: () =>
          import('./features/staff/staff-list.component').then((m) => m.StaffListComponent),
      },
      {
        path: 'location',
        canActivate: [roleGuard('ADMIN', 'STAFF')],
        loadComponent: () =>
          import('./features/location/location-management.component').then((m) => m.LocationManagementComponent),
      },
      {
        path: 'entrance-test',
        loadComponent: () =>
          import('./features/entrance/entrance-test.component').then((m) => m.EntranceTestComponent),
      },
      {
        path: 'kit',
        data: { title: 'KPMSYEP Kit Details' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
      },
      {
        path: 'sow',
        canActivate: [roleGuard('ADMIN', 'ZONE', 'CENTER', 'STAFF')],
        loadComponent: () =>
          import('./features/sow/sow.component').then((m) => m.SowComponent),
      },
      {
        path: 'resume',
        canActivate: [roleGuard('STUDENT', 'ADMIN')],
        loadComponent: () =>
          import('./features/resume/resume.component').then((m) => m.ResumeComponent),
      },
      {
        path: 'resource-persons',
        canActivate: [roleGuard('ADMIN', 'ZONE', 'CENTER', 'STAFF')],
        loadComponent: () =>
          import('./features/resource-person/resource-person.component').then((m) => m.ResourcePersonComponent),
      },
      {
        path: 'franchise-settings',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () =>
          import('./features/franchise-settings/franchise-settings.component').then((m) => m.FranchiseSettingsComponent),
      },
      {
        path: 'vbsow',
        data: { title: 'VBSOW — 16 Programs' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
      },
      {
        path: 'sfow',
        data: { title: 'SFOW Form Submissions' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
