import { CatalogState } from './catalog.state';

export const selectCatalogQuery = (state: CatalogState) => state.query;
export const selectCatalogPage = (state: CatalogState) => state.page;
