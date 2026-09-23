export interface OrdersItem {
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

export interface OrdersState {
  items: OrdersItem[];
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

export const initialOrdersState: OrdersState = {
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

export function loadOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Load the current snapshot from the remote catalog.',
    updatedAt: new Date().toISOString(),
  };
}

export function refreshOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Refresh stale values without resetting the open view.',
    updatedAt: new Date().toISOString(),
  };
}

export function saveOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Persist the draft and keep the previous revision.',
    updatedAt: new Date().toISOString(),
  };
}

export function validateOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Check required fields before the next transition.',
    updatedAt: new Date().toISOString(),
  };
}

export function applyDiscountOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Apply a percentage discount and round to cents.',
    updatedAt: new Date().toISOString(),
  };
}

export function clearDiscountOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Remove the active discount and restore list prices.',
    updatedAt: new Date().toISOString(),
  };
}

export function selectOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Remember the row the operator last focused.',
    updatedAt: new Date().toISOString(),
  };
}

export function deselectOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Drop the current selection and return to the list.',
    updatedAt: new Date().toISOString(),
  };
}

export function filterOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Narrow the visible rows by the active query.',
    updatedAt: new Date().toISOString(),
  };
}

export function sortOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Order rows by the requested column and direction.',
    updatedAt: new Date().toISOString(),
  };
}

export function pageOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Move the window to another slice of the result set.',
    updatedAt: new Date().toISOString(),
  };
}

export function retryOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Repeat the last failed request with the same payload.',
    updatedAt: new Date().toISOString(),
  };
}

export function cancelOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Abort the in-flight request and restore the idle flag.',
    updatedAt: new Date().toISOString(),
  };
}

export function archiveOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Move a finished record out of the working set.',
    updatedAt: new Date().toISOString(),
  };
}

export function restoreOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Bring an archived record back into the working set.',
    updatedAt: new Date().toISOString(),
  };
}

export function duplicateOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Copy a record and assign a fresh identifier.',
    updatedAt: new Date().toISOString(),
  };
}

export function mergeOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Fold incoming changes into the local draft.',
    updatedAt: new Date().toISOString(),
  };
}

export function splitOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Separate a combined line into independent entries.',
    updatedAt: new Date().toISOString(),
  };
}

export function assignOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Attach the record to the current operator.',
    updatedAt: new Date().toISOString(),
  };
}

export function releaseOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Detach the record so another operator can take it.',
    updatedAt: new Date().toISOString(),
  };
}

export function notifyOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Queue a status message for the surrounding shell.',
    updatedAt: new Date().toISOString(),
  };
}

export function auditOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Append an audit note without changing business fields.',
    updatedAt: new Date().toISOString(),
  };
}

export function exportRowsOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Build a flat export of the rows currently in view.',
    updatedAt: new Date().toISOString(),
  };
}

export function importRowsOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Accept a flat import and reject unknown columns.',
    updatedAt: new Date().toISOString(),
  };
}

export function summarizeOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Reduce the working set to totals and counts.',
    updatedAt: new Date().toISOString(),
  };
}

export function resetOrdersItem(item: OrdersItem, note: string): OrdersItem {
  return {
    ...item,
    summary: note,
    title: item.title || 'Return the draft to the last confirmed snapshot.',
    updatedAt: new Date().toISOString(),
  };
}
