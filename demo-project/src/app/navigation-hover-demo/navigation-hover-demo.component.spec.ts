import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NavigationHoverDemoComponent } from './navigation-hover-demo.component';

describe('NavigationHoverDemoComponent', () => {
  let fixture: ComponentFixture<NavigationHoverDemoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavigationHoverDemoComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(NavigationHoverDemoComponent);
    fixture.detectChanges();
  });

  it('creates', () => {
    expect(fixture.componentInstance.demoCaption).toBe('sandbox');
  });
});
