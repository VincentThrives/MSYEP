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
import { IdName, StudentSelfRegisterRequest, StudentSelfRegisterResult } from '../../core/models';

@Component({
  selector: 'app-student-register',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatProgressBarModule, MatIconModule,
    SearchSelectComponent, LocationPickerComponent,
  ],
  templateUrl: './student-register.component.html',
  styleUrl: './login.component.scss',
})
export class StudentRegisterComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  model: StudentSelfRegisterRequest = {
    name: '', phone: '', email: '', gender: '', dateOfBirth: '', educationalQualification: '',
    zoneId: '', centerId: '', district: '', taluk: '', gramPanchayat: '',
  };

  zones = signal<IdName[]>([]);
  centers = signal<IdName[]>([]);

  loading = signal(false);
  error = signal('');
  done = signal<StudentSelfRegisterResult | null>(null);

  constructor() {
    this.auth.publicZones().subscribe((z) => this.zones.set(z));
  }

  onZone(zoneId: string): void {
    this.model.zoneId = zoneId;
    this.model.centerId = '';
    this.centers.set([]);
    if (zoneId) this.auth.publicCenters(zoneId).subscribe((c) => this.centers.set(c));
  }

  submit(): void {
    if (!this.model.name.trim() || !/^\d{10}$/.test(this.model.phone.trim())) {
      this.error.set('Name and a valid 10-digit mobile number are required.');
      return;
    }
    this.loading.set(true);
    this.error.set('');
    const payload: StudentSelfRegisterRequest = {
      name: this.model.name.trim(),
      phone: this.model.phone.trim(),
      email: this.model.email?.trim() || undefined,
      gender: this.model.gender || undefined,
      dateOfBirth: this.model.dateOfBirth || undefined,
      educationalQualification: this.model.educationalQualification || undefined,
      zoneId: this.model.zoneId || undefined,
      centerId: this.model.centerId || undefined,
      district: this.model.district || undefined,
      taluk: this.model.taluk || undefined,
      gramPanchayat: this.model.gramPanchayat || undefined,
    };
    this.auth.registerStudent(payload).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.done.set(res);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message || 'Registration failed');
      },
    });
  }

  goToLogin(): void {
    const id = this.done()?.loginId || this.model.phone.trim();
    this.router.navigate(['/login'], { queryParams: { id } });
  }
}
