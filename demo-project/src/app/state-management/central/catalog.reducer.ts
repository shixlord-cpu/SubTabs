import { createReducer, on } from '@ngrx/store';
import * as Actions from './catalog.actions';

export interface CatalogState {
  items: Array<{ id: string; title: string; quantity: number; priceCents: number }>;
  loading: boolean;
  saving: boolean;
  error: string | null;
  query: string;
  pageIndex: number;
  selectedId: string | null;
  revision: number;
}

export const initialCatalogState: CatalogState = {
  items: [],
  loading: false,
  saving: false,
  error: null,
  query: '',
  pageIndex: 0,
  selectedId: null,
  revision: 0,
};

export const catalogReducer = createReducer(
  initialCatalogState,
  on(Actions.loadCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.loadCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.loadCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.refreshCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.refreshCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.refreshCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.saveCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.saveCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.saveCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.validateCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.validateCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.validateCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.applyDiscountCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.applyDiscountCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.applyDiscountCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.clearDiscountCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.clearDiscountCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.clearDiscountCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.selectCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.selectCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.selectCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.deselectCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.deselectCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.deselectCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.filterCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.filterCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.filterCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.sortCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.sortCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.sortCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.pageCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.pageCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.pageCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.retryCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.retryCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.retryCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.cancelCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.cancelCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.cancelCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.archiveCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.archiveCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.archiveCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.restoreCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.restoreCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.restoreCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.duplicateCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.duplicateCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.duplicateCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.mergeCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.mergeCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.mergeCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.splitCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.splitCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.splitCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.assignCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.assignCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.assignCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
  on(Actions.releaseCatalog, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  on(Actions.releaseCatalogSuccess, (state, { items }) => ({
    ...state,
    items,
    loading: false,
    revision: state.revision + 1,
  })),
  on(Actions.releaseCatalogFailure, (state, { message }) => ({
    ...state,
    loading: false,
    error: message,
  })),
);

export function reduceCatalogNote(note: string): string {
  return note;
}
