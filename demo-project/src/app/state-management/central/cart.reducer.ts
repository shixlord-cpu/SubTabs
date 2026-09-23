import { createReducer, on } from '@ngrx/store';
import * as Actions from './cart.actions';

export interface CartState {
  items: Array<{ id: string; title: string; quantity: number; priceCents: number }>;
  loading: boolean;
  saving: boolean;
  error: string | null;
  query: string;
  pageIndex: number;
  selectedId: string | null;
  revision: number;
}

export const initialCartState: CartState = {
  items: [],
  loading: false,
  saving: false,
  error: null,
  query: '',
  pageIndex: 0,
  selectedId: null,
  revision: 0,
};

export const cartReducer = createReducer(
  initialCartState,
  on(Actions.loadCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.loadCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.loadCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.refreshCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.refreshCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.refreshCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.saveCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.saveCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.saveCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.validateCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.validateCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.validateCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.applyDiscountCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.applyDiscountCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.applyDiscountCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.clearDiscountCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.clearDiscountCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.clearDiscountCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.selectCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.selectCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.selectCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.deselectCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.deselectCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.deselectCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.filterCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.filterCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.filterCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.sortCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.sortCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.sortCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.pageCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.pageCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.pageCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.retryCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.retryCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.retryCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.cancelCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.cancelCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.cancelCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.archiveCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.archiveCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.archiveCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.restoreCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.restoreCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.restoreCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.duplicateCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.duplicateCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.duplicateCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.mergeCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.mergeCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.mergeCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.splitCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.splitCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.splitCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.assignCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.assignCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.assignCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.releaseCart, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.releaseCartSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.releaseCartFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
);

export function reduceCartNote(note: string): string {
  return note;
}
