export interface OrdersState {
  loaded: boolean;
  selectedOrderId: string | null;
}

export const initialOrdersState: OrdersState = {
  loaded: false,
  selectedOrderId: null
};
