import { Component, computed, inject, signal } from '@angular/core';
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
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  auth = inject(AuthService);
  private router = inject(Router);
  opened = true;

  portal = computed(() => portalFor(this.auth.role() ?? 'ADMIN'));

  /** Group labels the user has collapsed; groups are expanded by default. */
  private collapsed = signal<Set<string>>(new Set());

  isExpanded(label: string): boolean {
    return !this.collapsed().has(label);
  }

  toggleGroup(label: string): void {
    const next = new Set(this.collapsed());
    next.has(label) ? next.delete(label) : next.add(label);
    this.collapsed.set(next);
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
