import { createAction, props } from '@ngrx/store';

interface CatalogItem {
  id: string;
  sku: string;
  title: string;
  quantity: number;
  priceCents: number;
}

interface CatalogDraft {
  title: string;
  summary: string;
  priceCents: number;
  status: 'draft' | 'active' | 'archived';
}

export const loadCatalog = createAction(
  '[Catalog] Load',
  props<{ draft: CatalogDraft; note: string }>()
);

export const loadCatalogSuccess = createAction(
  '[Catalog] Load Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const loadCatalogFailure = createAction(
  '[Catalog] Load Failure',
  props<{ message: string }>()
);

export const refreshCatalog = createAction(
  '[Catalog] Refresh',
  props<{ draft: CatalogDraft; note: string }>()
);

export const refreshCatalogSuccess = createAction(
  '[Catalog] Refresh Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const refreshCatalogFailure = createAction(
  '[Catalog] Refresh Failure',
  props<{ message: string }>()
);

export const saveCatalog = createAction(
  '[Catalog] Save',
  props<{ draft: CatalogDraft; note: string }>()
);

export const saveCatalogSuccess = createAction(
  '[Catalog] Save Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const saveCatalogFailure = createAction(
  '[Catalog] Save Failure',
  props<{ message: string }>()
);

export const validateCatalog = createAction(
  '[Catalog] Validate',
  props<{ draft: CatalogDraft; note: string }>()
);

export const validateCatalogSuccess = createAction(
  '[Catalog] Validate Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const validateCatalogFailure = createAction(
  '[Catalog] Validate Failure',
  props<{ message: string }>()
);

export const applyDiscountCatalog = createAction(
  '[Catalog] Apply Discount',
  props<{ draft: CatalogDraft; note: string }>()
);

export const applyDiscountCatalogSuccess = createAction(
  '[Catalog] Apply Discount Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const applyDiscountCatalogFailure = createAction(
  '[Catalog] Apply Discount Failure',
  props<{ message: string }>()
);

export const clearDiscountCatalog = createAction(
  '[Catalog] Clear Discount',
  props<{ draft: CatalogDraft; note: string }>()
);

export const clearDiscountCatalogSuccess = createAction(
  '[Catalog] Clear Discount Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const clearDiscountCatalogFailure = createAction(
  '[Catalog] Clear Discount Failure',
  props<{ message: string }>()
);

export const selectCatalog = createAction(
  '[Catalog] Select',
  props<{ draft: CatalogDraft; note: string }>()
);

export const selectCatalogSuccess = createAction(
  '[Catalog] Select Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const selectCatalogFailure = createAction(
  '[Catalog] Select Failure',
  props<{ message: string }>()
);

export const deselectCatalog = createAction(
  '[Catalog] Deselect',
  props<{ draft: CatalogDraft; note: string }>()
);

export const deselectCatalogSuccess = createAction(
  '[Catalog] Deselect Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const deselectCatalogFailure = createAction(
  '[Catalog] Deselect Failure',
  props<{ message: string }>()
);

export const filterCatalog = createAction(
  '[Catalog] Filter',
  props<{ draft: CatalogDraft; note: string }>()
);

export const filterCatalogSuccess = createAction(
  '[Catalog] Filter Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const filterCatalogFailure = createAction(
  '[Catalog] Filter Failure',
  props<{ message: string }>()
);

export const sortCatalog = createAction(
  '[Catalog] Sort',
  props<{ draft: CatalogDraft; note: string }>()
);

export const sortCatalogSuccess = createAction(
  '[Catalog] Sort Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const sortCatalogFailure = createAction(
  '[Catalog] Sort Failure',
  props<{ message: string }>()
);

export const pageCatalog = createAction(
  '[Catalog] Page',
  props<{ draft: CatalogDraft; note: string }>()
);

export const pageCatalogSuccess = createAction(
  '[Catalog] Page Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const pageCatalogFailure = createAction(
  '[Catalog] Page Failure',
  props<{ message: string }>()
);

export const retryCatalog = createAction(
  '[Catalog] Retry',
  props<{ draft: CatalogDraft; note: string }>()
);

export const retryCatalogSuccess = createAction(
  '[Catalog] Retry Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const retryCatalogFailure = createAction(
  '[Catalog] Retry Failure',
  props<{ message: string }>()
);

export const cancelCatalog = createAction(
  '[Catalog] Cancel',
  props<{ draft: CatalogDraft; note: string }>()
);

export const cancelCatalogSuccess = createAction(
  '[Catalog] Cancel Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const cancelCatalogFailure = createAction(
  '[Catalog] Cancel Failure',
  props<{ message: string }>()
);

export const archiveCatalog = createAction(
  '[Catalog] Archive',
  props<{ draft: CatalogDraft; note: string }>()
);

export const archiveCatalogSuccess = createAction(
  '[Catalog] Archive Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const archiveCatalogFailure = createAction(
  '[Catalog] Archive Failure',
  props<{ message: string }>()
);

export const restoreCatalog = createAction(
  '[Catalog] Restore',
  props<{ draft: CatalogDraft; note: string }>()
);

export const restoreCatalogSuccess = createAction(
  '[Catalog] Restore Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const restoreCatalogFailure = createAction(
  '[Catalog] Restore Failure',
  props<{ message: string }>()
);

export const duplicateCatalog = createAction(
  '[Catalog] Duplicate',
  props<{ draft: CatalogDraft; note: string }>()
);

export const duplicateCatalogSuccess = createAction(
  '[Catalog] Duplicate Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const duplicateCatalogFailure = createAction(
  '[Catalog] Duplicate Failure',
  props<{ message: string }>()
);

export const mergeCatalog = createAction(
  '[Catalog] Merge',
  props<{ draft: CatalogDraft; note: string }>()
);

export const mergeCatalogSuccess = createAction(
  '[Catalog] Merge Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const mergeCatalogFailure = createAction(
  '[Catalog] Merge Failure',
  props<{ message: string }>()
);

export const splitCatalog = createAction(
  '[Catalog] Split',
  props<{ draft: CatalogDraft; note: string }>()
);

export const splitCatalogSuccess = createAction(
  '[Catalog] Split Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const splitCatalogFailure = createAction(
  '[Catalog] Split Failure',
  props<{ message: string }>()
);

export const assignCatalog = createAction(
  '[Catalog] Assign',
  props<{ draft: CatalogDraft; note: string }>()
);

export const assignCatalogSuccess = createAction(
  '[Catalog] Assign Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const assignCatalogFailure = createAction(
  '[Catalog] Assign Failure',
  props<{ message: string }>()
);

export const releaseCatalog = createAction(
  '[Catalog] Release',
  props<{ draft: CatalogDraft; note: string }>()
);

export const releaseCatalogSuccess = createAction(
  '[Catalog] Release Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const releaseCatalogFailure = createAction(
  '[Catalog] Release Failure',
  props<{ message: string }>()
);

export const notifyCatalog = createAction(
  '[Catalog] Notify',
  props<{ draft: CatalogDraft; note: string }>()
);

export const notifyCatalogSuccess = createAction(
  '[Catalog] Notify Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const notifyCatalogFailure = createAction(
  '[Catalog] Notify Failure',
  props<{ message: string }>()
);

export const auditCatalog = createAction(
  '[Catalog] Audit',
  props<{ draft: CatalogDraft; note: string }>()
);

export const auditCatalogSuccess = createAction(
  '[Catalog] Audit Success',
  props<{ items: CatalogItem[]; note: string }>()
);

export const auditCatalogFailure = createAction(
  '[Catalog] Audit Failure',
  props<{ message: string }>()
);

export function describeCatalogAction(note: string): string {
  return 'Catalog: ' + note;
}
