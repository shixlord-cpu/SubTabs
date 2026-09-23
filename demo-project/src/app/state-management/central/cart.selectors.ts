import { createFeatureSelector, createSelector } from '@ngrx/store';
import { CartState } from './cart.state';

export const selectCartState = createFeatureSelector<CartState>('cart');

export const selectLoadCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Load the current snapshot from the remote catalog.',
  })
);

export const selectRefreshCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Refresh stale values without resetting the open view.',
  })
);

export const selectSaveCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Persist the draft and keep the previous revision.',
  })
);

export const selectValidateCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Check required fields before the next transition.',
  })
);

export const selectApplyDiscountCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Apply a percentage discount and round to cents.',
  })
);

export const selectClearDiscountCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Remove the active discount and restore list prices.',
  })
);

export const selectSelectCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Remember the row the operator last focused.',
  })
);

export const selectDeselectCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Drop the current selection and return to the list.',
  })
);

export const selectFilterCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Narrow the visible rows by the active query.',
  })
);

export const selectSortCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Order rows by the requested column and direction.',
  })
);

export const selectPageCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Move the window to another slice of the result set.',
  })
);

export const selectRetryCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Repeat the last failed request with the same payload.',
  })
);

export const selectCancelCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Abort the in-flight request and restore the idle flag.',
  })
);

export const selectArchiveCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Move a finished record out of the working set.',
  })
);

export const selectRestoreCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Bring an archived record back into the working set.',
  })
);

export const selectDuplicateCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Copy a record and assign a fresh identifier.',
  })
);

export const selectMergeCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Fold incoming changes into the local draft.',
  })
);

export const selectSplitCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Separate a combined line into independent entries.',
  })
);

export const selectAssignCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Attach the record to the current operator.',
  })
);

export const selectReleaseCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Detach the record so another operator can take it.',
  })
);

export const selectNotifyCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Queue a status message for the surrounding shell.',
  })
);

export const selectAuditCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Append an audit note without changing business fields.',
  })
);

export const selectExportRowsCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Build a flat export of the rows currently in view.',
  })
);

export const selectImportRowsCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Accept a flat import and reject unknown columns.',
  })
);

export const selectSummarizeCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Reduce the working set to totals and counts.',
  })
);

export const selectResetCart = createSelector(
  selectCartState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Return the draft to the last confirmed snapshot.',
  })
);

export function readCartSelection(id: string | null): string {
  return id ?? '';
}
