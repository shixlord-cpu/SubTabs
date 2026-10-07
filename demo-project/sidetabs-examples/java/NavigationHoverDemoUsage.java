package shop;

/**
 * Open this file in the demo IDE and run Navigate to Declaration (Ctrl+B) on
 * {@link #familiaNavigationDemoTarget} in {@link #runNavigationHoverDemo()}.
 * Then hover entries in the chooser to exercise Familia navigation-popup Hover Sync.
 */
public final class NavigationHoverDemoUsage {
    private NavigationHoverDemoUsage() {
    }

    public static void runNavigationHoverDemo() {
        String caption = NavigationHoverDemoAnchor.familiaNavigationDemoTarget();
        if (caption.isEmpty()) {
            NavigationHoverDemoAnchor.familiaNavigationDemoTarget("central-id");
            NavigationHoverDemoAnchor.familiaNavigationDemoTarget("feature-id", true);
        }
    }
}
