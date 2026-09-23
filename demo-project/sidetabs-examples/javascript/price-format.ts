export interface PriceFormatAmount {
  cents: number;
  currency: string;
}

export function loadPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const loadPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Load the current snapshot from the remote catalog.');
};

export function refreshPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const refreshPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Refresh stale values without resetting the open view.');
};

export function savePriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const savePriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Persist the draft and keep the previous revision.');
};

export function validatePriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const validatePriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Check required fields before the next transition.');
};

export function applyDiscountPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const applyDiscountPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Apply a percentage discount and round to cents.');
};

export function clearDiscountPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const clearDiscountPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Remove the active discount and restore list prices.');
};

export function selectPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const selectPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Remember the row the operator last focused.');
};

export function deselectPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const deselectPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Drop the current selection and return to the list.');
};

export function filterPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const filterPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Narrow the visible rows by the active query.');
};

export function sortPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const sortPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Order rows by the requested column and direction.');
};

export function pagePriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const pagePriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Move the window to another slice of the result set.');
};

export function retryPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const retryPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Repeat the last failed request with the same payload.');
};

export function cancelPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const cancelPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Abort the in-flight request and restore the idle flag.');
};

export function archivePriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const archivePriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Move a finished record out of the working set.');
};

export function restorePriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const restorePriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Bring an archived record back into the working set.');
};

export function duplicatePriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const duplicatePriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Copy a record and assign a fresh identifier.');
};

export function mergePriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const mergePriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Fold incoming changes into the local draft.');
};

export function splitPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const splitPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Separate a combined line into independent entries.');
};

export function assignPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const assignPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Attach the record to the current operator.');
};

export function releasePriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const releasePriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Detach the record so another operator can take it.');
};

export function notifyPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const notifyPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Queue a status message for the surrounding shell.');
};

export function auditPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const auditPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Append an audit note without changing business fields.');
};

export function exportRowsPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const exportRowsPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Build a flat export of the rows currently in view.');
};

export function importRowsPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const importRowsPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Accept a flat import and reject unknown columns.');
};

export function summarizePriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const summarizePriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Reduce the working set to totals and counts.');
};

export function resetPriceFormat(amount: PriceFormatAmount, note: string): PriceFormatAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const resetPriceFormatLabel = (note: string): string => {
  return 'PriceFormat: ' + (note || 'Return the draft to the last confirmed snapshot.');
};
