import { CartState } from './cart.state';

export const selectItemCount = (state: CartState) => state.itemCount;
