export interface MoneyAmount {
  cents: number;
  currency: string;
}

export function loadMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const loadMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Load the current snapshot from the remote catalog.');
};

export function refreshMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const refreshMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Refresh stale values without resetting the open view.');
};

export function saveMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const saveMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Persist the draft and keep the previous revision.');
};

export function validateMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const validateMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Check required fields before the next transition.');
};

export function applyDiscountMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const applyDiscountMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Apply a percentage discount and round to cents.');
};

export function clearDiscountMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const clearDiscountMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Remove the active discount and restore list prices.');
};

export function selectMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const selectMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Remember the row the operator last focused.');
};

export function deselectMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const deselectMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Drop the current selection and return to the list.');
};

export function filterMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const filterMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Narrow the visible rows by the active query.');
};

export function sortMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const sortMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Order rows by the requested column and direction.');
};

export function pageMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const pageMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Move the window to another slice of the result set.');
};

export function retryMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const retryMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Repeat the last failed request with the same payload.');
};

export function cancelMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const cancelMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Abort the in-flight request and restore the idle flag.');
};

export function archiveMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const archiveMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Move a finished record out of the working set.');
};

export function restoreMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const restoreMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Bring an archived record back into the working set.');
};

export function duplicateMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const duplicateMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Copy a record and assign a fresh identifier.');
};

export function mergeMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const mergeMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Fold incoming changes into the local draft.');
};

export function splitMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const splitMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Separate a combined line into independent entries.');
};

export function assignMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const assignMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Attach the record to the current operator.');
};

export function releaseMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const releaseMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Detach the record so another operator can take it.');
};

export function notifyMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const notifyMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Queue a status message for the surrounding shell.');
};

export function auditMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const auditMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Append an audit note without changing business fields.');
};

export function exportRowsMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const exportRowsMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Build a flat export of the rows currently in view.');
};

export function importRowsMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const importRowsMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Accept a flat import and reject unknown columns.');
};

export function summarizeMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const summarizeMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Reduce the working set to totals and counts.');
};

export function resetMoney(amount: MoneyAmount, note: string): MoneyAmount {
  const cents = Number.isFinite(amount.cents) ? amount.cents : 0;
  return { cents, currency: amount.currency || 'EUR' };
}

export const resetMoneyLabel = (note: string): string => {
  return 'Money: ' + (note || 'Return the draft to the last confirmed snapshot.');
};
