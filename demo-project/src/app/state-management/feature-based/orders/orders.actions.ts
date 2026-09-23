import { createAction, props } from '@ngrx/store';

interface OrdersItem {
  id: string;
  sku: string;
  title: string;
  quantity: number;
  priceCents: number;
}

interface OrdersDraft {
  title: string;
  summary: string;
  priceCents: number;
  status: 'draft' | 'active' | 'archived';
}

export const loadOrders = createAction(
  '[Orders] Load',
  props<{ draft: OrdersDraft; note: string }>()
);

export const loadOrdersSuccess = createAction(
  '[Orders] Load Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const loadOrdersFailure = createAction(
  '[Orders] Load Failure',
  props<{ message: string }>()
);

export const refreshOrders = createAction(
  '[Orders] Refresh',
  props<{ draft: OrdersDraft; note: string }>()
);

export const refreshOrdersSuccess = createAction(
  '[Orders] Refresh Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const refreshOrdersFailure = createAction(
  '[Orders] Refresh Failure',
  props<{ message: string }>()
);

export const saveOrders = createAction(
  '[Orders] Save',
  props<{ draft: OrdersDraft; note: string }>()
);

export const saveOrdersSuccess = createAction(
  '[Orders] Save Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const saveOrdersFailure = createAction(
  '[Orders] Save Failure',
  props<{ message: string }>()
);

export const validateOrders = createAction(
  '[Orders] Validate',
  props<{ draft: OrdersDraft; note: string }>()
);

export const validateOrdersSuccess = createAction(
  '[Orders] Validate Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const validateOrdersFailure = createAction(
  '[Orders] Validate Failure',
  props<{ message: string }>()
);

export const applyDiscountOrders = createAction(
  '[Orders] Apply Discount',
  props<{ draft: OrdersDraft; note: string }>()
);

export const applyDiscountOrdersSuccess = createAction(
  '[Orders] Apply Discount Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const applyDiscountOrdersFailure = createAction(
  '[Orders] Apply Discount Failure',
  props<{ message: string }>()
);

export const clearDiscountOrders = createAction(
  '[Orders] Clear Discount',
  props<{ draft: OrdersDraft; note: string }>()
);

export const clearDiscountOrdersSuccess = createAction(
  '[Orders] Clear Discount Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const clearDiscountOrdersFailure = createAction(
  '[Orders] Clear Discount Failure',
  props<{ message: string }>()
);

export const selectOrders = createAction(
  '[Orders] Select',
  props<{ draft: OrdersDraft; note: string }>()
);

export const selectOrdersSuccess = createAction(
  '[Orders] Select Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const selectOrdersFailure = createAction(
  '[Orders] Select Failure',
  props<{ message: string }>()
);

export const deselectOrders = createAction(
  '[Orders] Deselect',
  props<{ draft: OrdersDraft; note: string }>()
);

export const deselectOrdersSuccess = createAction(
  '[Orders] Deselect Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const deselectOrdersFailure = createAction(
  '[Orders] Deselect Failure',
  props<{ message: string }>()
);

export const filterOrders = createAction(
  '[Orders] Filter',
  props<{ draft: OrdersDraft; note: string }>()
);

export const filterOrdersSuccess = createAction(
  '[Orders] Filter Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const filterOrdersFailure = createAction(
  '[Orders] Filter Failure',
  props<{ message: string }>()
);

export const sortOrders = createAction(
  '[Orders] Sort',
  props<{ draft: OrdersDraft; note: string }>()
);

export const sortOrdersSuccess = createAction(
  '[Orders] Sort Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const sortOrdersFailure = createAction(
  '[Orders] Sort Failure',
  props<{ message: string }>()
);

export const pageOrders = createAction(
  '[Orders] Page',
  props<{ draft: OrdersDraft; note: string }>()
);

export const pageOrdersSuccess = createAction(
  '[Orders] Page Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const pageOrdersFailure = createAction(
  '[Orders] Page Failure',
  props<{ message: string }>()
);

export const retryOrders = createAction(
  '[Orders] Retry',
  props<{ draft: OrdersDraft; note: string }>()
);

export const retryOrdersSuccess = createAction(
  '[Orders] Retry Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const retryOrdersFailure = createAction(
  '[Orders] Retry Failure',
  props<{ message: string }>()
);

export const cancelOrders = createAction(
  '[Orders] Cancel',
  props<{ draft: OrdersDraft; note: string }>()
);

export const cancelOrdersSuccess = createAction(
  '[Orders] Cancel Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const cancelOrdersFailure = createAction(
  '[Orders] Cancel Failure',
  props<{ message: string }>()
);

export const archiveOrders = createAction(
  '[Orders] Archive',
  props<{ draft: OrdersDraft; note: string }>()
);

export const archiveOrdersSuccess = createAction(
  '[Orders] Archive Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const archiveOrdersFailure = createAction(
  '[Orders] Archive Failure',
  props<{ message: string }>()
);

export const restoreOrders = createAction(
  '[Orders] Restore',
  props<{ draft: OrdersDraft; note: string }>()
);

export const restoreOrdersSuccess = createAction(
  '[Orders] Restore Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const restoreOrdersFailure = createAction(
  '[Orders] Restore Failure',
  props<{ message: string }>()
);

export const duplicateOrders = createAction(
  '[Orders] Duplicate',
  props<{ draft: OrdersDraft; note: string }>()
);

export const duplicateOrdersSuccess = createAction(
  '[Orders] Duplicate Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const duplicateOrdersFailure = createAction(
  '[Orders] Duplicate Failure',
  props<{ message: string }>()
);

export const mergeOrders = createAction(
  '[Orders] Merge',
  props<{ draft: OrdersDraft; note: string }>()
);

export const mergeOrdersSuccess = createAction(
  '[Orders] Merge Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const mergeOrdersFailure = createAction(
  '[Orders] Merge Failure',
  props<{ message: string }>()
);

export const splitOrders = createAction(
  '[Orders] Split',
  props<{ draft: OrdersDraft; note: string }>()
);

export const splitOrdersSuccess = createAction(
  '[Orders] Split Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const splitOrdersFailure = createAction(
  '[Orders] Split Failure',
  props<{ message: string }>()
);

export const assignOrders = createAction(
  '[Orders] Assign',
  props<{ draft: OrdersDraft; note: string }>()
);

export const assignOrdersSuccess = createAction(
  '[Orders] Assign Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const assignOrdersFailure = createAction(
  '[Orders] Assign Failure',
  props<{ message: string }>()
);

export const releaseOrders = createAction(
  '[Orders] Release',
  props<{ draft: OrdersDraft; note: string }>()
);

export const releaseOrdersSuccess = createAction(
  '[Orders] Release Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const releaseOrdersFailure = createAction(
  '[Orders] Release Failure',
  props<{ message: string }>()
);

export const notifyOrders = createAction(
  '[Orders] Notify',
  props<{ draft: OrdersDraft; note: string }>()
);

export const notifyOrdersSuccess = createAction(
  '[Orders] Notify Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const notifyOrdersFailure = createAction(
  '[Orders] Notify Failure',
  props<{ message: string }>()
);

export const auditOrders = createAction(
  '[Orders] Audit',
  props<{ draft: OrdersDraft; note: string }>()
);

export const auditOrdersSuccess = createAction(
  '[Orders] Audit Success',
  props<{ items: OrdersItem[]; note: string }>()
);

export const auditOrdersFailure = createAction(
  '[Orders] Audit Failure',
  props<{ message: string }>()
);

export function describeOrdersAction(note: string): string {
  return 'Orders: ' + note;
}
