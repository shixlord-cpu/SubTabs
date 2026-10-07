import { Component } from '@angular/core';
import type { User as CentralUser } from '../models/central/user.model';
import type { User as FeatureUser } from '../models/feature-based/user/user.model';
import { familiaNavigationDemoTarget } from './navigation-hover-demo.anchor';

@Component({
  selector: 'app-navigation-hover-demo',
  templateUrl: './navigation-hover-demo.component.html',
  styleUrl: './navigation-hover-demo.component.scss',
  standalone: true,
})
export class NavigationHoverDemoComponent {
  /**
   * Demo: caret on {@code familiaNavigationDemoTarget} below → Navigate to Declaration (Ctrl+B).
   * The overload chooser appears; hover entries for Hover Sync (Familia settings).
   */
  readonly demoCaption = familiaNavigationDemoTarget();

  /** Secondary target: Ctrl+B on {@code CentralUser} or {@code FeatureUser} jumps to each user.model.ts */
  readonly centralLine = familiaNavigationDemoTarget({} as CentralUser);
  readonly featureLine = familiaNavigationDemoTarget({} as FeatureUser, 'feature');
}
