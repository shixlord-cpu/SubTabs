import { createFeatureSelector, createSelector } from '@ngrx/store';
import { UserState } from './user.state';

export const selectUserState = createFeatureSelector<UserState>('user');

export const selectLoadUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Load the current snapshot from the remote catalog.',
  })
);

export const selectRefreshUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Refresh stale values without resetting the open view.',
  })
);

export const selectSaveUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Persist the draft and keep the previous revision.',
  })
);

export const selectValidateUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Check required fields before the next transition.',
  })
);

export const selectApplyDiscountUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Apply a percentage discount and round to cents.',
  })
);

export const selectClearDiscountUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Remove the active discount and restore list prices.',
  })
);

export const selectSelectUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Remember the row the operator last focused.',
  })
);

export const selectDeselectUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Drop the current selection and return to the list.',
  })
);

export const selectFilterUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Narrow the visible rows by the active query.',
  })
);

export const selectSortUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Order rows by the requested column and direction.',
  })
);

export const selectPageUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Move the window to another slice of the result set.',
  })
);

export const selectRetryUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Repeat the last failed request with the same payload.',
  })
);

export const selectCancelUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Abort the in-flight request and restore the idle flag.',
  })
);

export const selectArchiveUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Move a finished record out of the working set.',
  })
);

export const selectRestoreUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Bring an archived record back into the working set.',
  })
);

export const selectDuplicateUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Copy a record and assign a fresh identifier.',
  })
);

export const selectMergeUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Fold incoming changes into the local draft.',
  })
);

export const selectSplitUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Separate a combined line into independent entries.',
  })
);

export const selectAssignUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Attach the record to the current operator.',
  })
);

export const selectReleaseUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Detach the record so another operator can take it.',
  })
);

export const selectNotifyUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Queue a status message for the surrounding shell.',
  })
);

export const selectAuditUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Append an audit note without changing business fields.',
  })
);

export const selectExportRowsUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Build a flat export of the rows currently in view.',
  })
);

export const selectImportRowsUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Accept a flat import and reject unknown columns.',
  })
);

export const selectSummarizeUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Reduce the working set to totals and counts.',
  })
);

export const selectResetUser = createSelector(
  selectUserState,
  (state) => ({
    items: state.items,
    loading: state.loading,
    error: state.error,
    note: 'Return the draft to the last confirmed snapshot.',
  })
);

export function readUserSelection(id: string | null): string {
  return id ?? '';
}
