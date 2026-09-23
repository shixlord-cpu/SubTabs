import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
  standalone: true,
})
export class HeaderComponent implements OnInit, OnDestroy {
  readonly entityName = 'Header';
  readonly pageSize = 24;
  loading = false;
  saving = false;
  errorMessage = '';
  query = '';
  sortColumn = 'title';
  sortDirection: 'asc' | 'desc' = 'asc';
  pageIndex = 0;
  selectedId: string | null = null;
  draftTitle = '';
  draftSummary = '';
  draftPriceCents = 0;
  form: FormGroup;
  rows: Array<{ id: string; sku: string; title: string; summary: string; priceCents: number; stock: number; status: string; updatedAt: string }> = [];
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly formBuilder: FormBuilder) {
    this.form = this.formBuilder.group({
      title: ['', Validators.required],
      summary: [''],
      priceCents: [0, Validators.min(0)],
      status: ['draft'],
    });
  }

  ngOnInit(): void {
    this.rows = [this.createEmptyRow('seed')];
    this.selectedId = this.rows[0].id;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Load the current snapshot from the remote catalog.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  refresh(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Refresh stale values without resetting the open view.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  save(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Persist the draft and keep the previous revision.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  validate(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    // +SUB-Checks
    if (!nextTitle.trim()) {
      this.errorMessage = 'A title is required.';
      this.loading = false;
      return;
    }
    // +SUBEND
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Check required fields before the next transition.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  applyDiscount(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Apply a percentage discount and round to cents.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  clearDiscount(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Remove the active discount and restore list prices.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  select(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Remember the row the operator last focused.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  deselect(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Drop the current selection and return to the list.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  filter(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Narrow the visible rows by the active query.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  sort(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Order rows by the requested column and direction.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  page(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Move the window to another slice of the result set.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  retry(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Repeat the last failed request with the same payload.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  cancel(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Abort the in-flight request and restore the idle flag.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  archive(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Move a finished record out of the working set.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  restore(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Bring an archived record back into the working set.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  duplicate(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Copy a record and assign a fresh identifier.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  merge(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Fold incoming changes into the local draft.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  split(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Separate a combined line into independent entries.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  assign(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Attach the record to the current operator.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  release(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Detach the record so another operator can take it.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  notify(): void {
    this.loading = true;
    this.errorMessage = '';
    const stamp = new Date().toISOString();
    const nextTitle = this.draftTitle || this.entityName;
    this.rows = this.rows.map((row) => ({
      ...row,
      title: row.id === this.selectedId ? nextTitle : row.title,
      summary: 'Queue a status message for the surrounding shell.',
      updatedAt: stamp,
    }));
    this.loading = false;
  }

  private createEmptyRow(suffix: string) {
    return {
      id: `${suffix}-${Date.now()}`,
      sku: suffix.toUpperCase(),
      title: 'Header',
      summary: '',
      priceCents: 0,
      stock: 0,
      status: 'draft',
      updatedAt: new Date().toISOString(),
    };
  }
}
