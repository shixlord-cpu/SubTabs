export interface UserState {
  id: string | null;
  displayName: string;
}

export const initialUserState: UserState = {
  id: null,
  displayName: ''
};
