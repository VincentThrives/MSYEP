import { Component, DestroyRef, computed, inject, input, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';

/**
 * A Material multi-select with a plain type-to-search box and a "Select all" toggle.
 * `value` is an array of the selected option values.
 *
 * The FormControl is the single source of truth: selection flows control → `value` output only,
 * so there is no `value ↔ control` sync effect that could loop. It also uses a plain search
 * <input> (not ngx-mat-select-search), avoiding that library's render-loop freeze.
 */
@Component({
  selector: 'app-multi-search-select',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatFormFieldModule, MatSelectModule,
    MatInputModule, MatCheckboxModule,
  ],
  template: `
    <mat-form-field appearance="outline">
      <mat-label>{{ label() }}</mat-label>
      <mat-select multiple [formControl]="selectCtrl" (selectionChange)="onSelection($event.value)">
        <mat-select-trigger>{{ triggerLabel() }}</mat-select-trigger>
        <div class="mss-search" (click)="$event.stopPropagation()">
          <input class="mss-input" type="text" [placeholder]="'Search ' + label().toLowerCase() + '…'"
            [value]="term()" (input)="term.set($any($event.target).value)"
            (keydown)="$event.stopPropagation()" />
        </div>
        <div class="mss-all">
          <mat-checkbox [checked]="allSelected()" [indeterminate]="someSelected()"
            (change)="toggleAll($event.checked)">Select all</mat-checkbox>
        </div>
        <mat-option *ngFor="let o of filtered()" [value]="optVal(o)">{{ optLabel(o) }}</mat-option>
        <div class="mss-empty" *ngIf="!filtered().length">No match</div>
      </mat-select>
    </mat-form-field>
  `,
  styles: [`
    :host { display: block; }
    mat-form-field { width: 100%; }
    .mss-search { padding: 8px 12px 4px; }
    .mss-input { width: 100%; box-sizing: border-box; padding: 6px 8px; font: inherit;
      border: 1px solid rgba(0,0,0,.2); border-radius: 6px; outline: none; }
    .mss-input:focus { border-color: #0E5132; }
    .mss-all { padding: 4px 16px; border-bottom: 1px solid rgba(0,0,0,.08); }
    .mss-empty { padding: 10px 16px; color: rgba(0,0,0,.5); font-size: 13px; }
  `],
})
export class MultiSearchSelectComponent {
  private destroyRef = inject(DestroyRef);

  value = model<any[]>([]);
  label = input('Select');
  options = input<any[]>([]);
  valueKey = input<string | null>(null);
  labelKey = input<string | null>(null);
  labelFn = input<((o: any) => string) | null>(null);

  /** mat-select needs a formControl in multiple mode; this control is the single source of truth. */
  selectCtrl = new FormControl<any[]>([], { nonNullable: true });
  term = signal('');
  /** Mirror of the current selection for the computeds/labels (kept in sync from the control). */
  private selected = signal<any[]>([]);

  filtered = computed(() => {
    const t = this.term().trim().toLowerCase();
    const opts = this.options() ?? [];
    if (!t) return opts;
    return opts.filter((o) => this.optLabel(o).toLowerCase().includes(t));
  });

  private allValues = computed(() => (this.options() ?? []).map((o) => this.optVal(o)));
  allSelected = computed(() => {
    const all = this.allValues();
    const sel = this.selected();
    return all.length > 0 && all.every((v) => sel.includes(v));
  });
  someSelected = computed(() => this.selected().length > 0 && !this.allSelected());

  constructor() {
    // Keep the mirror + `value` output in sync whenever the control changes (typed pick or Select all).
    this.selectCtrl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((v) => {
      const arr = v ?? [];
      this.selected.set([...arr]);
      this.value.set([...arr]);
    });
  }

  onSelection(v: any[]): void {
    this.selected.set([...(v ?? [])]);
    this.value.set([...(v ?? [])]);
  }

  toggleAll(checked: boolean): void {
    this.selectCtrl.setValue(checked ? [...this.allValues()] : []);
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

  triggerLabel(): string {
    const v = this.selected();
    if (!v.length) return '';
    if (v.length === 1) {
      const found = (this.options() ?? []).find((o) => this.optVal(o) === v[0]);
      return found ? this.optLabel(found) : String(v[0]);
    }
    return `${v.length} selected`;
  }
}
