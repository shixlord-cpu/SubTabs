import { createAction, props } from '@ngrx/store';

interface CartItem {
  id: string;
  sku: string;
  title: string;
  quantity: number;
  priceCents: number;
}

interface CartDraft {
  title: string;
  summary: string;
  priceCents: number;
  status: 'draft' | 'active' | 'archived';
}

export const loadCart = createAction(
  '[Cart] Load',
  props<{ draft: CartDraft; note: string }>()
);

export const loadCartSuccess = createAction(
  '[Cart] Load Success',
  props<{ items: CartItem[]; note: string }>()
);

export const loadCartFailure = createAction(
  '[Cart] Load Failure',
  props<{ message: string }>()
);

export const refreshCart = createAction(
  '[Cart] Refresh',
  props<{ draft: CartDraft; note: string }>()
);

export const refreshCartSuccess = createAction(
  '[Cart] Refresh Success',
  props<{ items: CartItem[]; note: string }>()
);

export const refreshCartFailure = createAction(
  '[Cart] Refresh Failure',
  props<{ message: string }>()
);

export const saveCart = createAction(
  '[Cart] Save',
  props<{ draft: CartDraft; note: string }>()
);

export const saveCartSuccess = createAction(
  '[Cart] Save Success',
  props<{ items: CartItem[]; note: string }>()
);

export const saveCartFailure = createAction(
  '[Cart] Save Failure',
  props<{ message: string }>()
);

export const validateCart = createAction(
  '[Cart] Validate',
  props<{ draft: CartDraft; note: string }>()
);

export const validateCartSuccess = createAction(
  '[Cart] Validate Success',
  props<{ items: CartItem[]; note: string }>()
);

export const validateCartFailure = createAction(
  '[Cart] Validate Failure',
  props<{ message: string }>()
);

export const applyDiscountCart = createAction(
  '[Cart] Apply Discount',
  props<{ draft: CartDraft; note: string }>()
);

export const applyDiscountCartSuccess = createAction(
  '[Cart] Apply Discount Success',
  props<{ items: CartItem[]; note: string }>()
);

export const applyDiscountCartFailure = createAction(
  '[Cart] Apply Discount Failure',
  props<{ message: string }>()
);

export const clearDiscountCart = createAction(
  '[Cart] Clear Discount',
  props<{ draft: CartDraft; note: string }>()
);

export const clearDiscountCartSuccess = createAction(
  '[Cart] Clear Discount Success',
  props<{ items: CartItem[]; note: string }>()
);

export const clearDiscountCartFailure = createAction(
  '[Cart] Clear Discount Failure',
  props<{ message: string }>()
);

export const selectCart = createAction(
  '[Cart] Select',
  props<{ draft: CartDraft; note: string }>()
);

export const selectCartSuccess = createAction(
  '[Cart] Select Success',
  props<{ items: CartItem[]; note: string }>()
);

export const selectCartFailure = createAction(
  '[Cart] Select Failure',
  props<{ message: string }>()
);

export const deselectCart = createAction(
  '[Cart] Deselect',
  props<{ draft: CartDraft; note: string }>()
);

export const deselectCartSuccess = createAction(
  '[Cart] Deselect Success',
  props<{ items: CartItem[]; note: string }>()
);

export const deselectCartFailure = createAction(
  '[Cart] Deselect Failure',
  props<{ message: string }>()
);

export const filterCart = createAction(
  '[Cart] Filter',
  props<{ draft: CartDraft; note: string }>()
);

export const filterCartSuccess = createAction(
  '[Cart] Filter Success',
  props<{ items: CartItem[]; note: string }>()
);

export const filterCartFailure = createAction(
  '[Cart] Filter Failure',
  props<{ message: string }>()
);

export const sortCart = createAction(
  '[Cart] Sort',
  props<{ draft: CartDraft; note: string }>()
);

export const sortCartSuccess = createAction(
  '[Cart] Sort Success',
  props<{ items: CartItem[]; note: string }>()
);

export const sortCartFailure = createAction(
  '[Cart] Sort Failure',
  props<{ message: string }>()
);

export const pageCart = createAction(
  '[Cart] Page',
  props<{ draft: CartDraft; note: string }>()
);

export const pageCartSuccess = createAction(
  '[Cart] Page Success',
  props<{ items: CartItem[]; note: string }>()
);

export const pageCartFailure = createAction(
  '[Cart] Page Failure',
  props<{ message: string }>()
);

export const retryCart = createAction(
  '[Cart] Retry',
  props<{ draft: CartDraft; note: string }>()
);

export const retryCartSuccess = createAction(
  '[Cart] Retry Success',
  props<{ items: CartItem[]; note: string }>()
);

export const retryCartFailure = createAction(
  '[Cart] Retry Failure',
  props<{ message: string }>()
);

export const cancelCart = createAction(
  '[Cart] Cancel',
  props<{ draft: CartDraft; note: string }>()
);

export const cancelCartSuccess = createAction(
  '[Cart] Cancel Success',
  props<{ items: CartItem[]; note: string }>()
);

export const cancelCartFailure = createAction(
  '[Cart] Cancel Failure',
  props<{ message: string }>()
);

export const archiveCart = createAction(
  '[Cart] Archive',
  props<{ draft: CartDraft; note: string }>()
);

export const archiveCartSuccess = createAction(
  '[Cart] Archive Success',
  props<{ items: CartItem[]; note: string }>()
);

export const archiveCartFailure = createAction(
  '[Cart] Archive Failure',
  props<{ message: string }>()
);

export const restoreCart = createAction(
  '[Cart] Restore',
  props<{ draft: CartDraft; note: string }>()
);

export const restoreCartSuccess = createAction(
  '[Cart] Restore Success',
  props<{ items: CartItem[]; note: string }>()
);

export const restoreCartFailure = createAction(
  '[Cart] Restore Failure',
  props<{ message: string }>()
);

export const duplicateCart = createAction(
  '[Cart] Duplicate',
  props<{ draft: CartDraft; note: string }>()
);

export const duplicateCartSuccess = createAction(
  '[Cart] Duplicate Success',
  props<{ items: CartItem[]; note: string }>()
);

export const duplicateCartFailure = createAction(
  '[Cart] Duplicate Failure',
  props<{ message: string }>()
);

export const mergeCart = createAction(
  '[Cart] Merge',
  props<{ draft: CartDraft; note: string }>()
);

export const mergeCartSuccess = createAction(
  '[Cart] Merge Success',
  props<{ items: CartItem[]; note: string }>()
);

export const mergeCartFailure = createAction(
  '[Cart] Merge Failure',
  props<{ message: string }>()
);

export const splitCart = createAction(
  '[Cart] Split',
  props<{ draft: CartDraft; note: string }>()
);

export const splitCartSuccess = createAction(
  '[Cart] Split Success',
  props<{ items: CartItem[]; note: string }>()
);

export const splitCartFailure = createAction(
  '[Cart] Split Failure',
  props<{ message: string }>()
);

export const assignCart = createAction(
  '[Cart] Assign',
  props<{ draft: CartDraft; note: string }>()
);

export const assignCartSuccess = createAction(
  '[Cart] Assign Success',
  props<{ items: CartItem[]; note: string }>()
);

export const assignCartFailure = createAction(
  '[Cart] Assign Failure',
  props<{ message: string }>()
);

export const releaseCart = createAction(
  '[Cart] Release',
  props<{ draft: CartDraft; note: string }>()
);

export const releaseCartSuccess = createAction(
  '[Cart] Release Success',
  props<{ items: CartItem[]; note: string }>()
);

export const releaseCartFailure = createAction(
  '[Cart] Release Failure',
  props<{ message: string }>()
);

export const notifyCart = createAction(
  '[Cart] Notify',
  props<{ draft: CartDraft; note: string }>()
);

export const notifyCartSuccess = createAction(
  '[Cart] Notify Success',
  props<{ items: CartItem[]; note: string }>()
);

export const notifyCartFailure = createAction(
  '[Cart] Notify Failure',
  props<{ message: string }>()
);

export const auditCart = createAction(
  '[Cart] Audit',
  props<{ draft: CartDraft; note: string }>()
);

export const auditCartSuccess = createAction(
  '[Cart] Audit Success',
  props<{ items: CartItem[]; note: string }>()
);

export const auditCartFailure = createAction(
  '[Cart] Audit Failure',
  props<{ message: string }>()
);

export function describeCartAction(note: string): string {
  return 'Cart: ' + note;
}
