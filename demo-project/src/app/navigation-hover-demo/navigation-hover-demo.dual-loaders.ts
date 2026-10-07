import { familiaNavigationDemoCentralLoader } from './navigation-hover-demo.entity-part-a';
import { familiaNavigationDemoFeatureLoader } from './navigation-hover-demo.entity-part-b';
import type { User as CentralUser } from '../models/central/user.model';
import type { User as FeatureUser } from '../models/feature-based/user/user.model';

/**
 * Ctrl+B on {@code familiaNavigationDemoCentralLoader} / {@code familiaNavigationDemoFeatureLoader}
 * below — one target each, in the two existing user.model.ts groups.
 */
export function navigationHoverDemoDualLoaders(
    mode: 'central' | 'feature',
    sample: CentralUser | FeatureUser,
    note: string,
): CentralUser | FeatureUser {
  return mode === 'central'
      ? familiaNavigationDemoCentralLoader(sample as CentralUser, note)
      : familiaNavigationDemoFeatureLoader(sample as FeatureUser, note);
}
