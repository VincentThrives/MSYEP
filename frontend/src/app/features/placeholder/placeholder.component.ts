import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

/** Stub page for modules whose spec is still pending (KPMSYEP kit, SOW, VBSOW, SFOW). */
@Component({
  selector: 'app-placeholder',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule],
  templateUrl: './placeholder.component.html',
  styleUrl: './placeholder.component.scss',
})
export class PlaceholderComponent {
  private route = inject(ActivatedRoute);
  title = this.route.snapshot.data['title'] || 'Coming soon';
}
