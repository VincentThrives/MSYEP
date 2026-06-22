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
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
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
