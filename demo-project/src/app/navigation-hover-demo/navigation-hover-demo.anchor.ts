import type { User as CentralUser } from '../models/central/user.model';
import type { User as FeatureUser } from '../models/feature-based/user/user.model';

/**
 * Familia demo for Navigate to Declaration (Ctrl+B): use the call
 * {@code familiaNavigationDemoTarget()} in the component — the chooser lists these overloads.
 * Hover Sync applies while you move over that popup list.
 */
export function familiaNavigationDemoTarget(): string;
export function familiaNavigationDemoTarget(user: CentralUser): string;
export function familiaNavigationDemoTarget(user: FeatureUser, kind: 'feature'): string;
export function familiaNavigationDemoTarget(user?: CentralUser | FeatureUser, kind?: 'feature'): string {
  if (user == null) {
    return 'sandbox';
  }
  return kind === 'feature' ? 'feature-user-model' : 'central-user-model';
}
