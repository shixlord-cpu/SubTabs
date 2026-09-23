import { User } from './user.model';

export const userMock: User = {
  id: 'demo-1',
  sku: 'DEMO-1',
  title: 'User sample',
  summary: 'Seed row for the demo workspace.',
  description: 'Longer copy sits on the model so the editor has a body to scroll.',
  priceCents: 1890,
  currency: 'EUR',
  stock: 12,
  status: 'active',
  createdAt: '2026-01-01T00:00:00.000Z',
  updatedAt: '2026-01-02T00:00:00.000Z',
};

export function loadUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Load the current snapshot from the remote catalog.',
    updatedAt: new Date().toISOString(),
  };
}

export function refreshUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Refresh stale values without resetting the open view.',
    updatedAt: new Date().toISOString(),
  };
}

export function saveUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Persist the draft and keep the previous revision.',
    updatedAt: new Date().toISOString(),
  };
}

export function validateUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Check required fields before the next transition.',
    updatedAt: new Date().toISOString(),
  };
}

export function applyDiscountUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Apply a percentage discount and round to cents.',
    updatedAt: new Date().toISOString(),
  };
}

export function clearDiscountUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Remove the active discount and restore list prices.',
    updatedAt: new Date().toISOString(),
  };
}

export function selectUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Remember the row the operator last focused.',
    updatedAt: new Date().toISOString(),
  };
}

export function deselectUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Drop the current selection and return to the list.',
    updatedAt: new Date().toISOString(),
  };
}

export function filterUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Narrow the visible rows by the active query.',
    updatedAt: new Date().toISOString(),
  };
}

export function sortUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Order rows by the requested column and direction.',
    updatedAt: new Date().toISOString(),
  };
}

export function pageUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Move the window to another slice of the result set.',
    updatedAt: new Date().toISOString(),
  };
}

export function retryUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Repeat the last failed request with the same payload.',
    updatedAt: new Date().toISOString(),
  };
}

export function cancelUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Abort the in-flight request and restore the idle flag.',
    updatedAt: new Date().toISOString(),
  };
}

export function archiveUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Move a finished record out of the working set.',
    updatedAt: new Date().toISOString(),
  };
}

export function restoreUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Bring an archived record back into the working set.',
    updatedAt: new Date().toISOString(),
  };
}

export function duplicateUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Copy a record and assign a fresh identifier.',
    updatedAt: new Date().toISOString(),
  };
}

export function mergeUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Fold incoming changes into the local draft.',
    updatedAt: new Date().toISOString(),
  };
}

export function splitUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Separate a combined line into independent entries.',
    updatedAt: new Date().toISOString(),
  };
}

export function assignUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Attach the record to the current operator.',
    updatedAt: new Date().toISOString(),
  };
}

export function releaseUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Detach the record so another operator can take it.',
    updatedAt: new Date().toISOString(),
  };
}

export function notifyUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Queue a status message for the surrounding shell.',
    updatedAt: new Date().toISOString(),
  };
}

export function auditUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Append an audit note without changing business fields.',
    updatedAt: new Date().toISOString(),
  };
}

export function exportRowsUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Build a flat export of the rows currently in view.',
    updatedAt: new Date().toISOString(),
  };
}

export function importRowsUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Accept a flat import and reject unknown columns.',
    updatedAt: new Date().toISOString(),
  };
}

export function summarizeUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Reduce the working set to totals and counts.',
    updatedAt: new Date().toISOString(),
  };
}

export function resetUserMock(base: User, note: string): User {
  return {
    ...base,
    summary: note,
    description: 'Return the draft to the last confirmed snapshot.',
    updatedAt: new Date().toISOString(),
  };
}
