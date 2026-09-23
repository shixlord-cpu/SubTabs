export interface OrderDto {
  id: string;
  sku: string;
  title: string;
  summary: string;
  description: string;
  priceCents: number;
  currency: string;
  stock: number;
  status: 'draft' | 'active' | 'archived';
  createdAt: string;
  updatedAt: string;
}

export function loadOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Load the current snapshot from the remote catalog.',
    updatedAt: new Date().toISOString(),
  };
}

export function refreshOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Refresh stale values without resetting the open view.',
    updatedAt: new Date().toISOString(),
  };
}

export function saveOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Persist the draft and keep the previous revision.',
    updatedAt: new Date().toISOString(),
  };
}

export function validateOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Check required fields before the next transition.',
    updatedAt: new Date().toISOString(),
  };
}

export function applyDiscountOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Apply a percentage discount and round to cents.',
    updatedAt: new Date().toISOString(),
  };
}

export function clearDiscountOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Remove the active discount and restore list prices.',
    updatedAt: new Date().toISOString(),
  };
}

export function selectOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Remember the row the operator last focused.',
    updatedAt: new Date().toISOString(),
  };
}

export function deselectOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Drop the current selection and return to the list.',
    updatedAt: new Date().toISOString(),
  };
}

export function filterOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Narrow the visible rows by the active query.',
    updatedAt: new Date().toISOString(),
  };
}

export function sortOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Order rows by the requested column and direction.',
    updatedAt: new Date().toISOString(),
  };
}

export function pageOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Move the window to another slice of the result set.',
    updatedAt: new Date().toISOString(),
  };
}

export function retryOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Repeat the last failed request with the same payload.',
    updatedAt: new Date().toISOString(),
  };
}

export function cancelOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Abort the in-flight request and restore the idle flag.',
    updatedAt: new Date().toISOString(),
  };
}

export function archiveOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Move a finished record out of the working set.',
    updatedAt: new Date().toISOString(),
  };
}

export function restoreOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Bring an archived record back into the working set.',
    updatedAt: new Date().toISOString(),
  };
}

export function duplicateOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Copy a record and assign a fresh identifier.',
    updatedAt: new Date().toISOString(),
  };
}

export function mergeOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Fold incoming changes into the local draft.',
    updatedAt: new Date().toISOString(),
  };
}

export function splitOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Separate a combined line into independent entries.',
    updatedAt: new Date().toISOString(),
  };
}

export function assignOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Attach the record to the current operator.',
    updatedAt: new Date().toISOString(),
  };
}

export function releaseOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Detach the record so another operator can take it.',
    updatedAt: new Date().toISOString(),
  };
}

export function notifyOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Queue a status message for the surrounding shell.',
    updatedAt: new Date().toISOString(),
  };
}

export function auditOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Append an audit note without changing business fields.',
    updatedAt: new Date().toISOString(),
  };
}

export function exportRowsOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Build a flat export of the rows currently in view.',
    updatedAt: new Date().toISOString(),
  };
}

export function importRowsOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Accept a flat import and reject unknown columns.',
    updatedAt: new Date().toISOString(),
  };
}

export function summarizeOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Reduce the working set to totals and counts.',
    updatedAt: new Date().toISOString(),
  };
}

export function resetOrderDto(value: OrderDto, note: string): OrderDto {
  return {
    ...value,
    summary: note,
    description: 'Return the draft to the last confirmed snapshot.',
    updatedAt: new Date().toISOString(),
  };
}
