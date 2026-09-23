import { createFeatureSelector, createSelector } from '@ngrx/store';
import { ProductsState } from './products.state';

export const selectProductsState = createFeatureSelector<ProductsState>('products');

export const selectLoadProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Load the current snapshot from the remote catalog.',
  })
);

export const selectRefreshProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Refresh stale values without resetting the open view.',
  })
);

export const selectSaveProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Persist the draft and keep the previous revision.',
  })
);

export const selectValidateProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Check required fields before the next transition.',
  })
);

export const selectApplyDiscountProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Apply a percentage discount and round to cents.',
  })
);

export const selectClearDiscountProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Remove the active discount and restore list prices.',
  })
);

export const selectSelectProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Remember the row the operator last focused.',
  })
);

export const selectDeselectProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Drop the current selection and return to the list.',
  })
);

export const selectFilterProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Narrow the visible rows by the active query.',
  })
);

export const selectSortProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Order rows by the requested column and direction.',
  })
);

export const selectPageProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Move the window to another slice of the result set.',
  })
);

export const selectRetryProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Repeat the last failed request with the same payload.',
  })
);

export const selectCancelProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Abort the in-flight request and restore the idle flag.',
  })
);

export const selectArchiveProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Move a finished record out of the working set.',
  })
);

export const selectRestoreProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Bring an archived record back into the working set.',
  })
);

export const selectDuplicateProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Copy a record and assign a fresh identifier.',
  })
);

export const selectMergeProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Fold incoming changes into the local draft.',
  })
);

export const selectSplitProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Separate a combined line into independent entries.',
  })
);

export const selectAssignProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Attach the record to the current operator.',
  })
);

export const selectReleaseProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Detach the record so another operator can take it.',
  })
);

export const selectNotifyProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Queue a status message for the surrounding shell.',
  })
);

export const selectAuditProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Append an audit note without changing business fields.',
  })
);

export const selectExportRowsProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Build a flat export of the rows currently in view.',
  })
);

export const selectImportRowsProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Accept a flat import and reject unknown columns.',
  })
);

export const selectSummarizeProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Reduce the working set to totals and counts.',
  })
);

export const selectResetProducts = createSelector(
  selectProductsState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Return the draft to the last confirmed snapshot.',
  })
);

export function readProductsSelection(id: string | null): string {
  return id ?? '';
}
