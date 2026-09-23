import { createAction, props } from '@ngrx/store';

interface UserItem {
  id: string;
  sku: string;
  title: string;
  quantity: number;
  priceCents: number;
}

interface UserDraft {
  title: string;
  summary: string;
  priceCents: number;
  status: 'draft' | 'active' | 'archived';
}

export const loadUser = createAction(
  '[User] Load',
  props<{ draft: UserDraft; note: string }>()
);

export const loadUserSuccess = createAction(
  '[User] Load Success',
  props<{ items: UserItem[]; note: string }>()
);

export const loadUserFailure = createAction(
  '[User] Load Failure',
  props<{ message: string }>()
);

export const refreshUser = createAction(
  '[User] Refresh',
  props<{ draft: UserDraft; note: string }>()
);

export const refreshUserSuccess = createAction(
  '[User] Refresh Success',
  props<{ items: UserItem[]; note: string }>()
);

export const refreshUserFailure = createAction(
  '[User] Refresh Failure',
  props<{ message: string }>()
);

export const saveUser = createAction(
  '[User] Save',
  props<{ draft: UserDraft; note: string }>()
);

export const saveUserSuccess = createAction(
  '[User] Save Success',
  props<{ items: UserItem[]; note: string }>()
);

export const saveUserFailure = createAction(
  '[User] Save Failure',
  props<{ message: string }>()
);

export const validateUser = createAction(
  '[User] Validate',
  props<{ draft: UserDraft; note: string }>()
);

export const validateUserSuccess = createAction(
  '[User] Validate Success',
  props<{ items: UserItem[]; note: string }>()
);

export const validateUserFailure = createAction(
  '[User] Validate Failure',
  props<{ message: string }>()
);

export const applyDiscountUser = createAction(
  '[User] Apply Discount',
  props<{ draft: UserDraft; note: string }>()
);

export const applyDiscountUserSuccess = createAction(
  '[User] Apply Discount Success',
  props<{ items: UserItem[]; note: string }>()
);

export const applyDiscountUserFailure = createAction(
  '[User] Apply Discount Failure',
  props<{ message: string }>()
);

export const clearDiscountUser = createAction(
  '[User] Clear Discount',
  props<{ draft: UserDraft; note: string }>()
);

export const clearDiscountUserSuccess = createAction(
  '[User] Clear Discount Success',
  props<{ items: UserItem[]; note: string }>()
);

export const clearDiscountUserFailure = createAction(
  '[User] Clear Discount Failure',
  props<{ message: string }>()
);

export const selectUser = createAction(
  '[User] Select',
  props<{ draft: UserDraft; note: string }>()
);

export const selectUserSuccess = createAction(
  '[User] Select Success',
  props<{ items: UserItem[]; note: string }>()
);

export const selectUserFailure = createAction(
  '[User] Select Failure',
  props<{ message: string }>()
);

export const deselectUser = createAction(
  '[User] Deselect',
  props<{ draft: UserDraft; note: string }>()
);

export const deselectUserSuccess = createAction(
  '[User] Deselect Success',
  props<{ items: UserItem[]; note: string }>()
);

export const deselectUserFailure = createAction(
  '[User] Deselect Failure',
  props<{ message: string }>()
);

export const filterUser = createAction(
  '[User] Filter',
  props<{ draft: UserDraft; note: string }>()
);

export const filterUserSuccess = createAction(
  '[User] Filter Success',
  props<{ items: UserItem[]; note: string }>()
);

export const filterUserFailure = createAction(
  '[User] Filter Failure',
  props<{ message: string }>()
);

export const sortUser = createAction(
  '[User] Sort',
  props<{ draft: UserDraft; note: string }>()
);

export const sortUserSuccess = createAction(
  '[User] Sort Success',
  props<{ items: UserItem[]; note: string }>()
);

export const sortUserFailure = createAction(
  '[User] Sort Failure',
  props<{ message: string }>()
);

export const pageUser = createAction(
  '[User] Page',
  props<{ draft: UserDraft; note: string }>()
);

export const pageUserSuccess = createAction(
  '[User] Page Success',
  props<{ items: UserItem[]; note: string }>()
);

export const pageUserFailure = createAction(
  '[User] Page Failure',
  props<{ message: string }>()
);

export const retryUser = createAction(
  '[User] Retry',
  props<{ draft: UserDraft; note: string }>()
);

export const retryUserSuccess = createAction(
  '[User] Retry Success',
  props<{ items: UserItem[]; note: string }>()
);

export const retryUserFailure = createAction(
  '[User] Retry Failure',
  props<{ message: string }>()
);

export const cancelUser = createAction(
  '[User] Cancel',
  props<{ draft: UserDraft; note: string }>()
);

export const cancelUserSuccess = createAction(
  '[User] Cancel Success',
  props<{ items: UserItem[]; note: string }>()
);

export const cancelUserFailure = createAction(
  '[User] Cancel Failure',
  props<{ message: string }>()
);

export const archiveUser = createAction(
  '[User] Archive',
  props<{ draft: UserDraft; note: string }>()
);

export const archiveUserSuccess = createAction(
  '[User] Archive Success',
  props<{ items: UserItem[]; note: string }>()
);

export const archiveUserFailure = createAction(
  '[User] Archive Failure',
  props<{ message: string }>()
);

export const restoreUser = createAction(
  '[User] Restore',
  props<{ draft: UserDraft; note: string }>()
);

export const restoreUserSuccess = createAction(
  '[User] Restore Success',
  props<{ items: UserItem[]; note: string }>()
);

export const restoreUserFailure = createAction(
  '[User] Restore Failure',
  props<{ message: string }>()
);

export const duplicateUser = createAction(
  '[User] Duplicate',
  props<{ draft: UserDraft; note: string }>()
);

export const duplicateUserSuccess = createAction(
  '[User] Duplicate Success',
  props<{ items: UserItem[]; note: string }>()
);

export const duplicateUserFailure = createAction(
  '[User] Duplicate Failure',
  props<{ message: string }>()
);

export const mergeUser = createAction(
  '[User] Merge',
  props<{ draft: UserDraft; note: string }>()
);

export const mergeUserSuccess = createAction(
  '[User] Merge Success',
  props<{ items: UserItem[]; note: string }>()
);

export const mergeUserFailure = createAction(
  '[User] Merge Failure',
  props<{ message: string }>()
);

export const splitUser = createAction(
  '[User] Split',
  props<{ draft: UserDraft; note: string }>()
);

export const splitUserSuccess = createAction(
  '[User] Split Success',
  props<{ items: UserItem[]; note: string }>()
);

export const splitUserFailure = createAction(
  '[User] Split Failure',
  props<{ message: string }>()
);

export const assignUser = createAction(
  '[User] Assign',
  props<{ draft: UserDraft; note: string }>()
);

export const assignUserSuccess = createAction(
  '[User] Assign Success',
  props<{ items: UserItem[]; note: string }>()
);

export const assignUserFailure = createAction(
  '[User] Assign Failure',
  props<{ message: string }>()
);

export const releaseUser = createAction(
  '[User] Release',
  props<{ draft: UserDraft; note: string }>()
);

export const releaseUserSuccess = createAction(
  '[User] Release Success',
  props<{ items: UserItem[]; note: string }>()
);

export const releaseUserFailure = createAction(
  '[User] Release Failure',
  props<{ message: string }>()
);

export const notifyUser = createAction(
  '[User] Notify',
  props<{ draft: UserDraft; note: string }>()
);

export const notifyUserSuccess = createAction(
  '[User] Notify Success',
  props<{ items: UserItem[]; note: string }>()
);

export const notifyUserFailure = createAction(
  '[User] Notify Failure',
  props<{ message: string }>()
);

export const auditUser = createAction(
  '[User] Audit',
  props<{ draft: UserDraft; note: string }>()
);

export const auditUserSuccess = createAction(
  '[User] Audit Success',
  props<{ items: UserItem[]; note: string }>()
);

export const auditUserFailure = createAction(
  '[User] Audit Failure',
  props<{ message: string }>()
);

export function describeUserAction(note: string): string {
  return 'User: ' + note;
}
