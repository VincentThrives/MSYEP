import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { forkJoin } from 'rxjs';

import { DataService } from '../../core/data.service';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatIconModule],
  template: `
    <div class="hero">
      <div>
        <div class="hi">{{ greeting() }} 👋</div>
        <div class="sub">MSYEP · Yukta Kaushalya Tarabethi — here's your overview</div>
      </div>
    </div>
    <div class="grid">
      <div class="stat card-green" routerLink="/app/zones">
        <mat-icon>public</mat-icon>
        <div class="num">{{ counts().zones }}</div>
        <div class="lbl">Zones · Universities</div>
      </div>
      <div class="stat card-gold" routerLink="/app/centers">
        <mat-icon>school</mat-icon>
        <div class="num">{{ counts().centers }}</div>
        <div class="lbl">Centers · Colleges</div>
      </div>
      <div class="stat card-orange" routerLink="/app/students">
        <mat-icon>groups</mat-icon>
        <div class="num">{{ counts().students }}</div>
        <div class="lbl">Students</div>
      </div>
      <div class="stat card-teal" routerLink="/app/finance">
        <mat-icon>account_balance</mat-icon>
        <div class="num">FW</div>
        <div class="lbl">Finance Wing</div>
      </div>
    </div>
  `,
  styles: [`
    .hero { display: flex; align-items: center; justify-content: space-between;
      padding: 22px 26px; margin-bottom: 22px; border-radius: 18px; color: #fff;
      background: linear-gradient(120deg, #0E5132 0%, #1E7A46 55%, #C9A227 130%);
      box-shadow: 0 10px 30px rgba(14,81,50,.25); }
    .hi { font-size: 24px; font-weight: 800; }
    .sub { opacity: .9; margin-top: 4px; font-size: 13px; }
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); gap: 18px; }
    .stat { padding: 24px; border-radius: 18px; cursor: pointer; color: #fff;
      transition: transform .15s, box-shadow .15s; position: relative; overflow: hidden;
      box-shadow: 0 8px 22px rgba(0,0,0,.12); }
    .stat:hover { transform: translateY(-5px); box-shadow: 0 14px 30px rgba(0,0,0,.20); }
    .stat mat-icon { font-size: 40px; height: 40px; width: 40px; opacity: .9; }
    .num { font-size: 40px; font-weight: 800; margin-top: 6px; line-height: 1; }
    .lbl { margin-top: 6px; font-weight: 600; opacity: .95; }
    .card-green  { background: linear-gradient(135deg, #1E7A46, #0E5132); }
    .card-gold   { background: linear-gradient(135deg, #E8C547, #C9A227); }
    .card-orange { background: linear-gradient(135deg, #FF9F43, #ED8B1F); }
    .card-teal   { background: linear-gradient(135deg, #2BB3A3, #11806f); }
  `],
})
export class DashboardComponent {
  private data = inject(DataService);
  private auth = inject(AuthService);

  counts = signal({ zones: 0, centers: 0, students: 0 });

  constructor() {
    forkJoin({
      zones: this.data.zones(),
      centers: this.data.centers(),
      students: this.data.students(),
    }).subscribe({
      next: (r) =>
        this.counts.set({ zones: r.zones.length, centers: r.centers.length, students: r.students.length }),
      error: () => {},
    });
  }

  greeting(): string {
    return `Welcome, ${this.auth.user()?.name || 'User'}`;
  }
}
