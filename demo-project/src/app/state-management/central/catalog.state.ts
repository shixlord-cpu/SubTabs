export interface CatalogState {
  query: string;
  page: number;
}

export const initialCatalogState: CatalogState = {
  query: '',
  page: 1
};
