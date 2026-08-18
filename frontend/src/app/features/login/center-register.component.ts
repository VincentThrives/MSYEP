import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';

import { AuthService } from '../../core/auth.service';
import { LocationPickerComponent } from '../../shared/location-picker.component';
import { SearchSelectComponent } from '../../shared/search-select.component';
import { Center, IdName } from '../../core/models';

/** Public center self-registration: fills the center form and creates the center + its CENTER login. */
@Component({
  selector: 'app-center-register',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatProgressBarModule, MatIconModule, SearchSelectComponent, LocationPickerComponent,
  ],
  templateUrl: './center-register.component.html',
  styleUrl: './login.component.scss',
})
export class CenterRegisterComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  model: Partial<Center> = {
    name: '', centerType: '', zoneId: '', district: '', taluk: '', gramPanchayat: '',
    address: '', locality: '', pincode: '', contactNumber: '', email: '', officeNumber: '',
    principalName: '', principalNumber: '', academicYear: '',
    academicStartMonth: '', academicEndMonth: '', userId: '', password: '',
  };
  coursesText = '';

  zones = signal<IdName[]>([]);
  loading = signal(false);
  error = signal('');
  done = signal<{ loginId?: string; emailNote?: string } | null>(null);

  readonly centerTypes = ['ITI', 'Diploma', 'PU College', 'Degree College', 'Engineering College', 'Polytechnic', 'Other'];
  readonly months = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December',
  ];

  constructor() {
    this.auth.publicZones().subscribe((z) => this.zones.set(z));
  }

  onZone(id: string): void {
    this.model.zoneId = id || undefined;
  }

  submit(): void {
    if (!this.model.name?.trim()) {
      this.error.set('Center name is required.');
      return;
    }
    if (!this.model.userId?.trim() || !this.model.password?.trim()) {
      this.error.set('Login User ID and Password are required.');
      return;
    }
    this.loading.set(true);
    this.error.set('');
    const courses = this.coursesText.split(',').map((c) => c.trim()).filter(Boolean);
    const payload: Partial<Center> = { ...this.model, courses };
    this.auth.registerCenter(payload).subscribe({
      next: (res) => { this.loading.set(false); this.done.set(res); },
      error: (err) => { this.loading.set(false); this.error.set(err?.error?.message || 'Registration failed'); },
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login'], { queryParams: { id: this.done()?.loginId || this.model.userId } });
  }
}
