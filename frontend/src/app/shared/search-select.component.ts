import { Component, computed, inject, input, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DestroyRef } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { NgxMatSelectSearchModule } from 'ngx-mat-select-search';

/**
 * A Material select with a built-in type-to-search box.
 *
 *   <app-search-select label="District" [options]="districts()" [(value)]="x.district"></app-search-select>
 *   <app-search-select label="Center" [options]="centers()" valueKey="id" labelKey="name"
 *     [(value)]="x.centerId" [emptyOption]="true" emptyLabel="All"></app-search-select>
 */
@Component({
  selector: 'app-search-select',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatFormFieldModule, MatSelectModule,
    MatInputModule, NgxMatSelectSearchModule,
  ],
  templateUrl: './search-select.component.html',
  styleUrl: './search-select.component.scss',
})
export class SearchSelectComponent {
  private destroyRef = inject(DestroyRef);

  value = model<any>(undefined);
  label = input('Select');
  options = input<any[]>([]);
  valueKey = input<string | null>(null);
  labelKey = input<string | null>(null);
  labelFn = input<((o: any) => string) | null>(null);
  disabled = input(false);
  emptyOption = input(false);
  emptyLabel = input('— none —');

  filterCtrl = new FormControl('');
  private term = signal('');

  filtered = computed(() => {
    const t = this.term().trim().toLowerCase();
    const opts = this.options() ?? [];
    if (!t) return opts;
    return opts.filter((o) => this.optLabel(o).toLowerCase().includes(t));
  });

  constructor() {
    this.filterCtrl.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((v) => this.term.set(v ?? ''));
  }

  optVal(o: any): any {
    const k = this.valueKey();
    return k && o != null && typeof o === 'object' ? o[k] : o;
  }

  optLabel(o: any): string {
    const fn = this.labelFn();
    if (fn) return fn(o);
    const k = this.labelKey();
    if (k && o != null && typeof o === 'object') return String(o[k] ?? '');
    return String(o ?? '');
  }

  displayLabel(): string {
    const v = this.value();
    if (v === undefined || v === null || v === '') return '';
    const found = (this.options() ?? []).find((o) => this.optVal(o) === v);
    return found ? this.optLabel(found) : String(v);
  }
}
