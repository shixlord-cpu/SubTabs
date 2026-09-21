import { ProductCardComponent } from './product-card.component';

describe('ProductCardComponent', () => {
  it('should render a price label', () => {
    const card = new ProductCardComponent();
    expect(card.label()).toContain('Mug');
  });

  it('should toggle the selected state', () => {
    const card = new ProductCardComponent();
    card.toggleSelected();
    expect(card.selected).toBe(true);
  });
});
