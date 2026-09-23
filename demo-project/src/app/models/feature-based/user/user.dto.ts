export interface UserDto {
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

export function loadUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Load the current snapshot from the remote catalog.',
    updatedAt: new Date().toISOString(),
  };
}

export function refreshUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Refresh stale values without resetting the open view.',
    updatedAt: new Date().toISOString(),
  };
}

export function saveUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Persist the draft and keep the previous revision.',
    updatedAt: new Date().toISOString(),
  };
}

export function validateUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Check required fields before the next transition.',
    updatedAt: new Date().toISOString(),
  };
}

export function applyDiscountUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Apply a percentage discount and round to cents.',
    updatedAt: new Date().toISOString(),
  };
}

export function clearDiscountUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Remove the active discount and restore list prices.',
    updatedAt: new Date().toISOString(),
  };
}

export function selectUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Remember the row the operator last focused.',
    updatedAt: new Date().toISOString(),
  };
}

export function deselectUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Drop the current selection and return to the list.',
    updatedAt: new Date().toISOString(),
  };
}

export function filterUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Narrow the visible rows by the active query.',
    updatedAt: new Date().toISOString(),
  };
}

export function sortUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Order rows by the requested column and direction.',
    updatedAt: new Date().toISOString(),
  };
}

export function pageUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Move the window to another slice of the result set.',
    updatedAt: new Date().toISOString(),
  };
}

export function retryUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Repeat the last failed request with the same payload.',
    updatedAt: new Date().toISOString(),
  };
}

export function cancelUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Abort the in-flight request and restore the idle flag.',
    updatedAt: new Date().toISOString(),
  };
}

export function archiveUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Move a finished record out of the working set.',
    updatedAt: new Date().toISOString(),
  };
}

export function restoreUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Bring an archived record back into the working set.',
    updatedAt: new Date().toISOString(),
  };
}

export function duplicateUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Copy a record and assign a fresh identifier.',
    updatedAt: new Date().toISOString(),
  };
}

export function mergeUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Fold incoming changes into the local draft.',
    updatedAt: new Date().toISOString(),
  };
}

export function splitUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Separate a combined line into independent entries.',
    updatedAt: new Date().toISOString(),
  };
}

export function assignUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Attach the record to the current operator.',
    updatedAt: new Date().toISOString(),
  };
}

export function releaseUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Detach the record so another operator can take it.',
    updatedAt: new Date().toISOString(),
  };
}

export function notifyUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Queue a status message for the surrounding shell.',
    updatedAt: new Date().toISOString(),
  };
}

export function auditUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Append an audit note without changing business fields.',
    updatedAt: new Date().toISOString(),
  };
}

export function exportRowsUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Build a flat export of the rows currently in view.',
    updatedAt: new Date().toISOString(),
  };
}

export function importRowsUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Accept a flat import and reject unknown columns.',
    updatedAt: new Date().toISOString(),
  };
}

export function summarizeUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Reduce the working set to totals and counts.',
    updatedAt: new Date().toISOString(),
  };
}

export function resetUserDto(value: UserDto, note: string): UserDto {
  return {
    ...value,
    summary: note,
    description: 'Return the draft to the last confirmed snapshot.',
    updatedAt: new Date().toISOString(),
  };
}
