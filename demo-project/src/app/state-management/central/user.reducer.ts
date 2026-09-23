import { createReducer, on } from '@ngrx/store';
import * as Actions from './user.actions';

export interface UserState {
  items: Array<{ id: string; title: string; quantity: number; priceCents: number }>;
  loading: boolean;
  saving: boolean;
  error: string | null;
  query: string;
  pageIndex: number;
  selectedId: string | null;
  revision: number;
}

export const initialUserState: UserState = {
  items: [],
  loading: false,
  saving: false,
  error: null,
  query: '',
  pageIndex: 0,
  selectedId: null,
  revision: 0,
};

export const userReducer = createReducer(
  initialUserState,
  on(Actions.loadUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.loadUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.loadUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.refreshUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.refreshUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.refreshUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.saveUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.saveUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.saveUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.validateUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.validateUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.validateUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.applyDiscountUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.applyDiscountUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.applyDiscountUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.clearDiscountUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.clearDiscountUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.clearDiscountUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.selectUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.selectUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.selectUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.deselectUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.deselectUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.deselectUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.filterUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.filterUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.filterUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.sortUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.sortUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.sortUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.pageUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.pageUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.pageUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.retryUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.retryUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.retryUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.cancelUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.cancelUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.cancelUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.archiveUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.archiveUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.archiveUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.restoreUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.restoreUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.restoreUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.duplicateUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.duplicateUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.duplicateUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.mergeUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.mergeUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.mergeUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.splitUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.splitUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.splitUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.assignUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.assignUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.assignUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.releaseUser, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.releaseUserSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.releaseUserFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
);

export function reduceUserNote(note: string): string {
  return note;
}
