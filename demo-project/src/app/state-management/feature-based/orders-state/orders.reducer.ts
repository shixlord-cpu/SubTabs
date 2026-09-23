import { createReducer, on } from '@ngrx/store';
import * as Actions from './orders.actions';

export interface OrdersState {
  items: Array<{ id: string; title: string; quantity: number; priceCents: number }>;
  loading: boolean;
  saving: boolean;
  error: string | null;
  query: string;
  pageIndex: number;
  selectedId: string | null;
  revision: number;
}

export const initialOrdersState: OrdersState = {
  items: [],
  loading: false,
  saving: false,
  error: null,
  query: '',
  pageIndex: 0,
  selectedId: null,
  revision: 0,
};

export const ordersReducer = createReducer(
  initialOrdersState,
  on(Actions.loadOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.loadOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.loadOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.refreshOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.refreshOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.refreshOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.saveOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.saveOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.saveOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.validateOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.validateOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.validateOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.applyDiscountOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.applyDiscountOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.applyDiscountOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.clearDiscountOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.clearDiscountOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.clearDiscountOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.selectOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.selectOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.selectOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.deselectOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.deselectOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.deselectOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.filterOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.filterOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.filterOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.sortOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.sortOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.sortOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.pageOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.pageOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.pageOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.retryOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.retryOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.retryOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.cancelOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.cancelOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.cancelOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.archiveOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.archiveOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.archiveOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.restoreOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.restoreOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.restoreOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.duplicateOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.duplicateOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.duplicateOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.mergeOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.mergeOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.mergeOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.splitOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.splitOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.splitOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.assignOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.assignOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.assignOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.releaseOrders, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.releaseOrdersSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.releaseOrdersFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
);

export function reduceOrdersNote(note: string): string {
  return note;
}
