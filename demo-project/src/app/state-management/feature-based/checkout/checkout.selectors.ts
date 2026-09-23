import { createFeatureSelector, createSelector } from '@ngrx/store';
import { CheckoutState } from './checkout.state';

export const selectCheckoutState = createFeatureSelector<CheckoutState>('checkout');

export const selectLoadCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Load the current snapshot from the remote catalog.',
  })
);

export const selectRefreshCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Refresh stale values without resetting the open view.',
  })
);

export const selectSaveCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Persist the draft and keep the previous revision.',
  })
);

export const selectValidateCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Check required fields before the next transition.',
  })
);

export const selectApplyDiscountCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Apply a percentage discount and round to cents.',
  })
);

export const selectClearDiscountCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Remove the active discount and restore list prices.',
  })
);

export const selectSelectCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Remember the row the operator last focused.',
  })
);

export const selectDeselectCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Drop the current selection and return to the list.',
  })
);

export const selectFilterCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Narrow the visible rows by the active query.',
  })
);

export const selectSortCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Order rows by the requested column and direction.',
  })
);

export const selectPageCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Move the window to another slice of the result set.',
  })
);

export const selectRetryCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Repeat the last failed request with the same payload.',
  })
);

export const selectCancelCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Abort the in-flight request and restore the idle flag.',
  })
);

export const selectArchiveCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Move a finished record out of the working set.',
  })
);

export const selectRestoreCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Bring an archived record back into the working set.',
  })
);

export const selectDuplicateCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Copy a record and assign a fresh identifier.',
  })
);

export const selectMergeCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Fold incoming changes into the local draft.',
  })
);

export const selectSplitCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Separate a combined line into independent entries.',
  })
);

export const selectAssignCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Attach the record to the current operator.',
  })
);

export const selectReleaseCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Detach the record so another operator can take it.',
  })
);

export const selectNotifyCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Queue a status message for the surrounding shell.',
  })
);

export const selectAuditCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Append an audit note without changing business fields.',
  })
);

export const selectExportRowsCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Build a flat export of the rows currently in view.',
  })
);

export const selectImportRowsCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Accept a flat import and reject unknown columns.',
  })
);

export const selectSummarizeCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Reduce the working set to totals and counts.',
  })
);

export const selectResetCheckout = createSelector(
  selectCheckoutState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Return the draft to the last confirmed snapshot.',
  })
);

export function readCheckoutSelection(id: string | null): string {
  return id ?? '';
}
