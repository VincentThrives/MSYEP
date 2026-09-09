import { Component, computed, inject, signal, HostListener } from '@angular/core';
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

/** Below this width we switch the sidenav to an overlay drawer (mobile / tablet-portrait). */
const MOBILE_BREAKPOINT = 900;

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

  /** Viewport width tracked reactively so the drawer mode follows the screen. */
  private width = signal(typeof window !== 'undefined' ? window.innerWidth : 1280);
  isMobile = computed(() => this.width() < MOBILE_BREAKPOINT);
  /** On desktop the drawer is docked open; on mobile it starts closed and toggles as an overlay. */
  opened = signal(typeof window !== 'undefined' ? window.innerWidth >= MOBILE_BREAKPOINT : true);

  portal = computed(() => portalFor(this.auth.role() ?? 'ADMIN'));

  /** Group labels the user has collapsed; groups are expanded by default. */
  private collapsed = signal<Set<string>>(new Set());

  @HostListener('window:resize')
  onResize(): void {
    const w = window.innerWidth;
    this.width.set(w);
    // Keep desktop docked-open; leave the mobile drawer in whatever state the user set.
    if (w >= MOBILE_BREAKPOINT) this.opened.set(true);
    else this.opened.set(false);
  }

  toggleNav(): void {
    this.opened.update((v) => !v);
  }

  /** Tapping a link on mobile should close the overlay so the page is visible. */
  onNavigate(): void {
    if (this.isMobile()) this.opened.set(false);
  }

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
