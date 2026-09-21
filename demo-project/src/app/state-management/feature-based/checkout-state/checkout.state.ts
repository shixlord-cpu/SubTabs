export interface CheckoutState {
  step: number;
  busy: boolean;
}

export const initialCheckoutState: CheckoutState = {
  step: 0,
  busy: false
};
