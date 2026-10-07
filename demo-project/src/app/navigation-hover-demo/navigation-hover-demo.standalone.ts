/**
 * Zuverlässige TS-Demo: Aufruf und Überladungen in **einer** Datei (wie {@code NavigationHoverDemoStandalone.java}).
 * Caret auf {@code familiaNavigationDemoTarget} in {@link runNavigationHoverDemo} → Ctrl+B → Popup → hovern.
 */
export function runNavigationHoverDemo(): void {
  const caption = familiaNavigationDemoTarget();
  if (!caption) {
    familiaNavigationDemoTarget('central-id');
    familiaNavigationDemoTarget('feature-id', true);
  }
}

export function familiaNavigationDemoTarget(): string {
  return 'sandbox';
}

export function familiaNavigationDemoTarget(centralUserId: string): string {
  return centralUserId ?? 'central';
}

export function familiaNavigationDemoTarget(featureUserId: string, feature: boolean): string {
  if (!feature) {
    return familiaNavigationDemoTarget(featureUserId);
  }
  return featureUserId ?? 'feature';
}
