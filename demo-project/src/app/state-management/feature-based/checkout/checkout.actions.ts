import { createAction, props } from '@ngrx/store';

interface CheckoutItem {
  id: string;
  sku: string;
  title: string;
  quantity: number;
  priceCents: number;
}

interface CheckoutDraft {
  title: string;
  summary: string;
  priceCents: number;
  status: 'draft' | 'active' | 'archived';
}

export const loadCheckout = createAction(
  '[Checkout] Load',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const loadCheckoutSuccess = createAction(
  '[Checkout] Load Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const loadCheckoutFailure = createAction(
  '[Checkout] Load Failure',
  props<{ message: string }>()
);

export const refreshCheckout = createAction(
  '[Checkout] Refresh',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const refreshCheckoutSuccess = createAction(
  '[Checkout] Refresh Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const refreshCheckoutFailure = createAction(
  '[Checkout] Refresh Failure',
  props<{ message: string }>()
);

export const saveCheckout = createAction(
  '[Checkout] Save',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const saveCheckoutSuccess = createAction(
  '[Checkout] Save Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const saveCheckoutFailure = createAction(
  '[Checkout] Save Failure',
  props<{ message: string }>()
);

export const validateCheckout = createAction(
  '[Checkout] Validate',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const validateCheckoutSuccess = createAction(
  '[Checkout] Validate Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const validateCheckoutFailure = createAction(
  '[Checkout] Validate Failure',
  props<{ message: string }>()
);

export const applyDiscountCheckout = createAction(
  '[Checkout] Apply Discount',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const applyDiscountCheckoutSuccess = createAction(
  '[Checkout] Apply Discount Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const applyDiscountCheckoutFailure = createAction(
  '[Checkout] Apply Discount Failure',
  props<{ message: string }>()
);

export const clearDiscountCheckout = createAction(
  '[Checkout] Clear Discount',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const clearDiscountCheckoutSuccess = createAction(
  '[Checkout] Clear Discount Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const clearDiscountCheckoutFailure = createAction(
  '[Checkout] Clear Discount Failure',
  props<{ message: string }>()
);

export const selectCheckout = createAction(
  '[Checkout] Select',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const selectCheckoutSuccess = createAction(
  '[Checkout] Select Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const selectCheckoutFailure = createAction(
  '[Checkout] Select Failure',
  props<{ message: string }>()
);

export const deselectCheckout = createAction(
  '[Checkout] Deselect',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const deselectCheckoutSuccess = createAction(
  '[Checkout] Deselect Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const deselectCheckoutFailure = createAction(
  '[Checkout] Deselect Failure',
  props<{ message: string }>()
);

export const filterCheckout = createAction(
  '[Checkout] Filter',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const filterCheckoutSuccess = createAction(
  '[Checkout] Filter Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const filterCheckoutFailure = createAction(
  '[Checkout] Filter Failure',
  props<{ message: string }>()
);

export const sortCheckout = createAction(
  '[Checkout] Sort',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const sortCheckoutSuccess = createAction(
  '[Checkout] Sort Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const sortCheckoutFailure = createAction(
  '[Checkout] Sort Failure',
  props<{ message: string }>()
);

export const pageCheckout = createAction(
  '[Checkout] Page',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const pageCheckoutSuccess = createAction(
  '[Checkout] Page Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const pageCheckoutFailure = createAction(
  '[Checkout] Page Failure',
  props<{ message: string }>()
);

export const retryCheckout = createAction(
  '[Checkout] Retry',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const retryCheckoutSuccess = createAction(
  '[Checkout] Retry Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const retryCheckoutFailure = createAction(
  '[Checkout] Retry Failure',
  props<{ message: string }>()
);

export const cancelCheckout = createAction(
  '[Checkout] Cancel',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const cancelCheckoutSuccess = createAction(
  '[Checkout] Cancel Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const cancelCheckoutFailure = createAction(
  '[Checkout] Cancel Failure',
  props<{ message: string }>()
);

export const archiveCheckout = createAction(
  '[Checkout] Archive',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const archiveCheckoutSuccess = createAction(
  '[Checkout] Archive Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const archiveCheckoutFailure = createAction(
  '[Checkout] Archive Failure',
  props<{ message: string }>()
);

export const restoreCheckout = createAction(
  '[Checkout] Restore',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const restoreCheckoutSuccess = createAction(
  '[Checkout] Restore Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const restoreCheckoutFailure = createAction(
  '[Checkout] Restore Failure',
  props<{ message: string }>()
);

export const duplicateCheckout = createAction(
  '[Checkout] Duplicate',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const duplicateCheckoutSuccess = createAction(
  '[Checkout] Duplicate Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const duplicateCheckoutFailure = createAction(
  '[Checkout] Duplicate Failure',
  props<{ message: string }>()
);

export const mergeCheckout = createAction(
  '[Checkout] Merge',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const mergeCheckoutSuccess = createAction(
  '[Checkout] Merge Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const mergeCheckoutFailure = createAction(
  '[Checkout] Merge Failure',
  props<{ message: string }>()
);

export const splitCheckout = createAction(
  '[Checkout] Split',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const splitCheckoutSuccess = createAction(
  '[Checkout] Split Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const splitCheckoutFailure = createAction(
  '[Checkout] Split Failure',
  props<{ message: string }>()
);

export const assignCheckout = createAction(
  '[Checkout] Assign',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const assignCheckoutSuccess = createAction(
  '[Checkout] Assign Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const assignCheckoutFailure = createAction(
  '[Checkout] Assign Failure',
  props<{ message: string }>()
);

export const releaseCheckout = createAction(
  '[Checkout] Release',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const releaseCheckoutSuccess = createAction(
  '[Checkout] Release Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const releaseCheckoutFailure = createAction(
  '[Checkout] Release Failure',
  props<{ message: string }>()
);

export const notifyCheckout = createAction(
  '[Checkout] Notify',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const notifyCheckoutSuccess = createAction(
  '[Checkout] Notify Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const notifyCheckoutFailure = createAction(
  '[Checkout] Notify Failure',
  props<{ message: string }>()
);

export const auditCheckout = createAction(
  '[Checkout] Audit',
  props<{ draft: CheckoutDraft; note: string }>()
);

export const auditCheckoutSuccess = createAction(
  '[Checkout] Audit Success',
  props<{ items: CheckoutItem[]; note: string }>()
);

export const auditCheckoutFailure = createAction(
  '[Checkout] Audit Failure',
  props<{ message: string }>()
);

export function describeCheckoutAction(note: string): string {
  return 'Checkout: ' + note;
}
