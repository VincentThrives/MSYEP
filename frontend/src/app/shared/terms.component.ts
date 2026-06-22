import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * KP-MSYEP / Yuktha Kaushalya Kar job-portal Terms & Conditions.
 * Reused at every "accept the T&C" step (franchise sign-up, employer/candidate sign-up).
 */
@Component({
  selector: 'app-terms',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './terms.component.html',
  styleUrl: './terms.component.scss',
})
export class TermsComponent {}
