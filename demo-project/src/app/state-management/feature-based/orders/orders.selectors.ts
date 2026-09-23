import { createFeatureSelector, createSelector } from '@ngrx/store';
import { OrdersState } from './orders.state';

export const selectOrdersState = createFeatureSelector<OrdersState>('orders');

export const selectLoadOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Load the current snapshot from the remote catalog.',
  })
);

export const selectRefreshOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Refresh stale values without resetting the open view.',
  })
);

export const selectSaveOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Persist the draft and keep the previous revision.',
  })
);

export const selectValidateOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Check required fields before the next transition.',
  })
);

export const selectApplyDiscountOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Apply a percentage discount and round to cents.',
  })
);

export const selectClearDiscountOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Remove the active discount and restore list prices.',
  })
);

export const selectSelectOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Remember the row the operator last focused.',
  })
);

export const selectDeselectOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Drop the current selection and return to the list.',
  })
);

export const selectFilterOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Narrow the visible rows by the active query.',
  })
);

export const selectSortOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Order rows by the requested column and direction.',
  })
);

export const selectPageOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Move the window to another slice of the result set.',
  })
);

export const selectRetryOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Repeat the last failed request with the same payload.',
  })
);

export const selectCancelOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Abort the in-flight request and restore the idle flag.',
  })
);

export const selectArchiveOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Move a finished record out of the working set.',
  })
);

export const selectRestoreOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Bring an archived record back into the working set.',
  })
);

export const selectDuplicateOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Copy a record and assign a fresh identifier.',
  })
);

export const selectMergeOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Fold incoming changes into the local draft.',
  })
);

export const selectSplitOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Separate a combined line into independent entries.',
  })
);

export const selectAssignOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Attach the record to the current operator.',
  })
);

export const selectReleaseOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Detach the record so another operator can take it.',
  })
);

export const selectNotifyOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Queue a status message for the surrounding shell.',
  })
);

export const selectAuditOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Append an audit note without changing business fields.',
  })
);

export const selectExportRowsOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Build a flat export of the rows currently in view.',
  })
);

export const selectImportRowsOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Accept a flat import and reject unknown columns.',
  })
);

export const selectSummarizeOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Reduce the working set to totals and counts.',
  })
);

export const selectResetOrders = createSelector(
  selectOrdersState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Return the draft to the last confirmed snapshot.',
  })
);

export function readOrdersSelection(id: string | null): string {
  return id ?? '';
}
