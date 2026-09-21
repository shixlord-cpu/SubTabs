import { HeaderComponent } from './header.component';

describe('HeaderComponent', () => {
  it('should create', () => {
    expect(new HeaderComponent()).toBeTruthy();
  });

  it('should toggle compact mode', () => {
    const header = new HeaderComponent();
    header.toggleCompact();
    expect(header.compact).toBe(true);
  });
});
