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
  template: `
    <mat-card class="ph">
      <mat-icon>construction</mat-icon>
      <h2>{{ title }}</h2>
      <p>This module is scaffolded and waiting on its spec
        (form fields / 16 VBSOW programs / GP mapping). Once you share the details
        it'll be wired up here.</p>
    </mat-card>
  `,
  styles: [`
    .ph { max-width: 560px; margin: 40px auto; padding: 32px; text-align: center; }
    .ph mat-icon { font-size: 48px; height: 48px; width: 48px; color: #bbb; }
    h2 { margin: 12px 0; }
    p { color: #777; }
  `],
})
export class PlaceholderComponent {
  private route = inject(ActivatedRoute);
  title = this.route.snapshot.data['title'] || 'Coming soon';
}
