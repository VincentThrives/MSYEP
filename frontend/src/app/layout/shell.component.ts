import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';

import { AuthService } from '../core/auth.service';
import { portalFor } from '../core/nav';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    CommonModule, RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule, MatIconModule,
    MatButtonModule, MatMenuModule,
  ],
  template: `
    <div class="shell" [class]="portal().cssClass">
      <mat-toolbar class="topbar">
        <button mat-icon-button (click)="opened = !opened"><mat-icon>menu</mat-icon></button>
        <span class="brand">MSYEP</span>
        <span class="portal-name">{{ portal().name }}</span>
        <span class="spacer"></span>
        <button mat-button [matMenuTriggerFor]="menu">
          <mat-icon>account_circle</mat-icon>
          <span class="user">{{ auth.user()?.name || auth.user()?.email }}</span>
        </button>
        <mat-menu #menu="matMenu">
          <div class="menu-role">{{ auth.role() }}</div>
          <button mat-menu-item (click)="logout()"><mat-icon>logout</mat-icon> Logout</button>
        </mat-menu>
      </mat-toolbar>

      <mat-sidenav-container class="container">
        <mat-sidenav [opened]="opened" mode="side" class="sidenav">
          <mat-nav-list>
            <a mat-list-item *ngFor="let item of portal().nav"
               [routerLink]="item.path" routerLinkActive="active-link">
              <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
            </a>
          </mat-nav-list>
        </mat-sidenav>
        <mat-sidenav-content class="content">
          <router-outlet></router-outlet>
        </mat-sidenav-content>
      </mat-sidenav-container>
    </div>
  `,
  styles: [`
    .shell { height: 100vh; display: flex; flex-direction: column; }
    .topbar { color: #fff; gap: 8px; }
    .brand { font-weight: 700; letter-spacing: 1px; }
    .portal-name { opacity: .85; font-size: 14px; margin-left: 8px; }
    .spacer { flex: 1 1 auto; }
    .user { margin-left: 4px; }
    .container { flex: 1; }
    .sidenav { width: 250px; border-right: 1px solid #eee; }
    .content { padding: 24px; background: #fafafa; }
    .menu-role { padding: 6px 16px; font-size: 12px; color: #888; }
    .active-link { font-weight: 700; }
  `],
})
export class ShellComponent {
  auth = inject(AuthService);
  private router = inject(Router);
  opened = true;

  portal = computed(() => portalFor(this.auth.role() ?? 'ADMIN'));

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
