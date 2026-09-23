export interface CatalogItem {
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

export interface CatalogState {
  items: CatalogItem[];
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

export const initialCatalogState: CatalogState = {
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

export function loadCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Load the current snapshot from the remote catalog.',
    updatedAt: new Date().toISOString(),
  };
}

export function refreshCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Refresh stale values without resetting the open view.',
    updatedAt: new Date().toISOString(),
  };
}

export function saveCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Persist the draft and keep the previous revision.',
    updatedAt: new Date().toISOString(),
  };
}

export function validateCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Check required fields before the next transition.',
    updatedAt: new Date().toISOString(),
  };
}

export function applyDiscountCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Apply a percentage discount and round to cents.',
    updatedAt: new Date().toISOString(),
  };
}

export function clearDiscountCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Remove the active discount and restore list prices.',
    updatedAt: new Date().toISOString(),
  };
}

export function selectCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Remember the row the operator last focused.',
    updatedAt: new Date().toISOString(),
  };
}

export function deselectCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Drop the current selection and return to the list.',
    updatedAt: new Date().toISOString(),
  };
}

export function filterCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Narrow the visible rows by the active query.',
    updatedAt: new Date().toISOString(),
  };
}

export function sortCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Order rows by the requested column and direction.',
    updatedAt: new Date().toISOString(),
  };
}

export function pageCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Move the window to another slice of the result set.',
    updatedAt: new Date().toISOString(),
  };
}

export function retryCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Repeat the last failed request with the same payload.',
    updatedAt: new Date().toISOString(),
  };
}

export function cancelCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Abort the in-flight request and restore the idle flag.',
    updatedAt: new Date().toISOString(),
  };
}

export function archiveCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Move a finished record out of the working set.',
    updatedAt: new Date().toISOString(),
  };
}

export function restoreCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Bring an archived record back into the working set.',
    updatedAt: new Date().toISOString(),
  };
}

export function duplicateCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Copy a record and assign a fresh identifier.',
    updatedAt: new Date().toISOString(),
  };
}

export function mergeCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Fold incoming changes into the local draft.',
    updatedAt: new Date().toISOString(),
  };
}

export function splitCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Separate a combined line into independent entries.',
    updatedAt: new Date().toISOString(),
  };
}

export function assignCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Attach the record to the current operator.',
    updatedAt: new Date().toISOString(),
  };
}

export function releaseCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Detach the record so another operator can take it.',
    updatedAt: new Date().toISOString(),
  };
}

export function notifyCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Queue a status message for the surrounding shell.',
    updatedAt: new Date().toISOString(),
  };
}

export function auditCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Append an audit note without changing business fields.',
    updatedAt: new Date().toISOString(),
  };
}

export function exportRowsCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Build a flat export of the rows currently in view.',
    updatedAt: new Date().toISOString(),
  };
}

export function importRowsCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Accept a flat import and reject unknown columns.',
    updatedAt: new Date().toISOString(),
  };
}

export function summarizeCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Reduce the working set to totals and counts.',
    updatedAt: new Date().toISOString(),
  };
}

export function resetCatalogItem(item: CatalogItem, note: string): CatalogItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Return the draft to the last confirmed snapshot.',
    updatedAt: new Date().toISOString(),
  };
}
