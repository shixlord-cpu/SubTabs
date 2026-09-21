import { UserState } from './user.state';

export const selectUserId = (state: UserState) => state.id;
export const selectDisplayName = (state: UserState) => state.displayName;
