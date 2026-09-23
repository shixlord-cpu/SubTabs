export interface UserItem {
  id: string;
  sku: string;
  title: string;
  summary: string;
  quantity: number;
  priceCents: number;
  currency: string;
  status: 'draft' | 'active' | 'archived';
  updatedAt: string;
}

export interface UserState {
  items: UserItem[];
  loading: boolean;
  saving: boolean;
  error: string | null;
  query: string;
  sortColumn: string;
  sortDirection: 'asc' | 'desc';
  pageIndex: number;
  pageSize: number;
  selectedId: string | null;
  revision: number;
}

export const initialUserState: UserState = {
  items: [],
  loading: false,
  saving: false,
  error: null,
  query: '',
  sortColumn: 'title',
  sortDirection: 'asc',
  pageIndex: 0,
  pageSize: 24,
  selectedId: null,
  revision: 0,
};

export function loadUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Load the current snapshot from the remote catalog.',
    updatedAt: new Date().toISOString(),
  };
}

export function refreshUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Refresh stale values without resetting the open view.',
    updatedAt: new Date().toISOString(),
  };
}

export function saveUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Persist the draft and keep the previous revision.',
    updatedAt: new Date().toISOString(),
  };
}

export function validateUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Check required fields before the next transition.',
    updatedAt: new Date().toISOString(),
  };
}

export function applyDiscountUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Apply a percentage discount and round to cents.',
    updatedAt: new Date().toISOString(),
  };
}

export function clearDiscountUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Remove the active discount and restore list prices.',
    updatedAt: new Date().toISOString(),
  };
}

export function selectUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Remember the row the operator last focused.',
    updatedAt: new Date().toISOString(),
  };
}

export function deselectUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Drop the current selection and return to the list.',
    updatedAt: new Date().toISOString(),
  };
}

export function filterUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Narrow the visible rows by the active query.',
    updatedAt: new Date().toISOString(),
  };
}

export function sortUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Order rows by the requested column and direction.',
    updatedAt: new Date().toISOString(),
  };
}

export function pageUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Move the window to another slice of the result set.',
    updatedAt: new Date().toISOString(),
  };
}

export function retryUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Repeat the last failed request with the same payload.',
    updatedAt: new Date().toISOString(),
  };
}

export function cancelUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Abort the in-flight request and restore the idle flag.',
    updatedAt: new Date().toISOString(),
  };
}

export function archiveUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Move a finished record out of the working set.',
    updatedAt: new Date().toISOString(),
  };
}

export function restoreUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Bring an archived record back into the working set.',
    updatedAt: new Date().toISOString(),
  };
}

export function duplicateUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Copy a record and assign a fresh identifier.',
    updatedAt: new Date().toISOString(),
  };
}

export function mergeUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Fold incoming changes into the local draft.',
    updatedAt: new Date().toISOString(),
  };
}

export function splitUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Separate a combined line into independent entries.',
    updatedAt: new Date().toISOString(),
  };
}

export function assignUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Attach the record to the current operator.',
    updatedAt: new Date().toISOString(),
  };
}

export function releaseUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Detach the record so another operator can take it.',
    updatedAt: new Date().toISOString(),
  };
}

export function notifyUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Queue a status message for the surrounding shell.',
    updatedAt: new Date().toISOString(),
  };
}

export function auditUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Append an audit note without changing business fields.',
    updatedAt: new Date().toISOString(),
  };
}

export function exportRowsUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Build a flat export of the rows currently in view.',
    updatedAt: new Date().toISOString(),
  };
}

export function importRowsUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Accept a flat import and reject unknown columns.',
    updatedAt: new Date().toISOString(),
  };
}

export function summarizeUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Reduce the working set to totals and counts.',
    updatedAt: new Date().toISOString(),
  };
}

export function resetUserItem(item: UserItem, note: string): UserItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Return the draft to the last confirmed snapshot.',
    updatedAt: new Date().toISOString(),
  };
}
