import { createReducer, on } from '@ngrx/store';
import * as Actions from './products.actions';

export interface ProductsState {
  items: Array<{ id: string; title: string; quantity: number; priceCents: number }>;
  loading: boolean;
  saving: boolean;
  error: string | null;
  query: string;
  pageIndex: number;
  selectedId: string | null;
  revision: number;
}

export const initialProductsState: ProductsState = {
  items: [],
  loading: false,
  saving: false,
  error: null,
  query: '',
  pageIndex: 0,
  selectedId: null,
  revision: 0,
};

export const productsReducer = createReducer(
  initialProductsState,
  on(Actions.loadProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.loadProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.loadProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.refreshProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.refreshProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.refreshProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.saveProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.saveProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.saveProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.validateProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.validateProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.validateProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.applyDiscountProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.applyDiscountProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.applyDiscountProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.clearDiscountProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.clearDiscountProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.clearDiscountProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.selectProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.selectProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.selectProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.deselectProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.deselectProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.deselectProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.filterProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.filterProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.filterProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.sortProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.sortProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.sortProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.pageProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.pageProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.pageProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.retryProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.retryProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.retryProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.cancelProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.cancelProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.cancelProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.archiveProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.archiveProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.archiveProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.restoreProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.restoreProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.restoreProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.duplicateProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.duplicateProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.duplicateProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.mergeProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.mergeProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.mergeProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.splitProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.splitProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.splitProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.assignProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.assignProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.assignProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.releaseProducts, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.releaseProductsSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.releaseProductsFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
);

export function reduceProductsNote(note: string): string {
  return note;
}
