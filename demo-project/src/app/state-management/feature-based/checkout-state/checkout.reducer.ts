import { createReducer, on } from '@ngrx/store';
import * as Actions from './checkout.actions';

export interface CheckoutState {
  items: Array<{ id: string; title: string; quantity: number; priceCents: number }>;
  loading: boolean;
  saving: boolean;
  error: string | null;
  query: string;
  pageIndex: number;
  selectedId: string | null;
  revision: number;
}

export const initialCheckoutState: CheckoutState = {
  items: [],
  loading: false,
  saving: false,
  error: null,
  query: '',
  pageIndex: 0,
  selectedId: null,
  revision: 0,
};

export const checkoutReducer = createReducer(
  initialCheckoutState,
  on(Actions.loadCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.loadCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.loadCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.refreshCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.refreshCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.refreshCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.saveCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.saveCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.saveCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.validateCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.validateCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.validateCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.applyDiscountCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.applyDiscountCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.applyDiscountCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.clearDiscountCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.clearDiscountCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.clearDiscountCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.selectCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.selectCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.selectCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.deselectCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.deselectCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.deselectCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.filterCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.filterCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.filterCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.sortCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.sortCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.sortCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.pageCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.pageCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.pageCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.retryCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.retryCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.retryCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.cancelCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.cancelCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.cancelCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.archiveCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.archiveCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.archiveCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.restoreCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.restoreCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.restoreCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.duplicateCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.duplicateCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.duplicateCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.mergeCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.mergeCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.mergeCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.splitCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.splitCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.splitCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.assignCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.assignCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.assignCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.releaseCheckout, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.releaseCheckoutSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.releaseCheckoutFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
);

export function reduceCheckoutNote(note: string): string {
  return note;
}
