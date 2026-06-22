import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
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
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () =>
          import('./features/zones/zone-list.component').then((m) => m.ZoneListComponent),
      },
      {
        path: 'centers',
        canActivate: [roleGuard('ADMIN', 'ZONE')],
        loadComponent: () =>
          import('./features/centers/center-list.component').then((m) => m.CenterListComponent),
      },
      {
        path: 'students',
        loadComponent: () =>
          import('./features/students/student-list.component').then((m) => m.StudentListComponent),
      },
      {
        path: 'finance',
        canActivate: [roleGuard('ADMIN', 'FINANCE', 'ZONE', 'CENTER')],
        loadComponent: () =>
          import('./features/finance/finance-table.component').then((m) => m.FinanceTableComponent),
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
        data: { title: 'KPMSYEP SOW (Statement of Work)' },
        loadComponent: () =>
          import('./features/placeholder/placeholder.component').then((m) => m.PlaceholderComponent),
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
