export interface Product {
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

export function loadProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Load the current snapshot from the remote catalog.',
    updatedAt: new Date().toISOString(),
  };
}

export function refreshProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Refresh stale values without resetting the open view.',
    updatedAt: new Date().toISOString(),
  };
}

export function saveProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Persist the draft and keep the previous revision.',
    updatedAt: new Date().toISOString(),
  };
}

export function validateProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Check required fields before the next transition.',
    updatedAt: new Date().toISOString(),
  };
}

export function applyDiscountProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Apply a percentage discount and round to cents.',
    updatedAt: new Date().toISOString(),
  };
}

export function clearDiscountProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Remove the active discount and restore list prices.',
    updatedAt: new Date().toISOString(),
  };
}

export function selectProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Remember the row the operator last focused.',
    updatedAt: new Date().toISOString(),
  };
}

export function deselectProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Drop the current selection and return to the list.',
    updatedAt: new Date().toISOString(),
  };
}

export function filterProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Narrow the visible rows by the active query.',
    updatedAt: new Date().toISOString(),
  };
}

export function sortProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Order rows by the requested column and direction.',
    updatedAt: new Date().toISOString(),
  };
}

export function pageProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Move the window to another slice of the result set.',
    updatedAt: new Date().toISOString(),
  };
}

export function retryProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Repeat the last failed request with the same payload.',
    updatedAt: new Date().toISOString(),
  };
}

export function cancelProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Abort the in-flight request and restore the idle flag.',
    updatedAt: new Date().toISOString(),
  };
}

export function archiveProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Move a finished record out of the working set.',
    updatedAt: new Date().toISOString(),
  };
}

export function restoreProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Bring an archived record back into the working set.',
    updatedAt: new Date().toISOString(),
  };
}

export function duplicateProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Copy a record and assign a fresh identifier.',
    updatedAt: new Date().toISOString(),
  };
}

export function mergeProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Fold incoming changes into the local draft.',
    updatedAt: new Date().toISOString(),
  };
}

export function splitProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Separate a combined line into independent entries.',
    updatedAt: new Date().toISOString(),
  };
}

export function assignProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Attach the record to the current operator.',
    updatedAt: new Date().toISOString(),
  };
}

export function releaseProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Detach the record so another operator can take it.',
    updatedAt: new Date().toISOString(),
  };
}

export function notifyProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Queue a status message for the surrounding shell.',
    updatedAt: new Date().toISOString(),
  };
}

export function auditProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Append an audit note without changing business fields.',
    updatedAt: new Date().toISOString(),
  };
}

export function exportRowsProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Build a flat export of the rows currently in view.',
    updatedAt: new Date().toISOString(),
  };
}

export function importRowsProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Accept a flat import and reject unknown columns.',
    updatedAt: new Date().toISOString(),
  };
}

export function summarizeProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Reduce the working set to totals and counts.',
    updatedAt: new Date().toISOString(),
  };
}

export function resetProduct(value: Product, note: string): Product {
  return {
    ...value,
    summary: note,
    description: 'Return the draft to the last confirmed snapshot.',
    updatedAt: new Date().toISOString(),
  };
}
