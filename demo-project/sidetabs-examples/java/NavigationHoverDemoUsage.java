package shop;

/**
 * Open this file in the demo IDE and run Navigate to Declaration (Ctrl+B) on
 * {@link #tabzNavigationDemoTarget} in {@link #runNavigationHoverDemo()}.
 * Then hover entries in the chooser to exercise Tabz navigation-popup Hover Sync.
 */
public final class NavigationHoverDemoUsage {
    private NavigationHoverDemoUsage() {
    }

    public static void runNavigationHoverDemo() {
        String caption = NavigationHoverDemoAnchor.tabzNavigationDemoTarget();
        if (caption.isEmpty()) {
            NavigationHoverDemoAnchor.tabzNavigationDemoTarget("central-id");
            NavigationHoverDemoAnchor.tabzNavigationDemoTarget("feature-id", true);
        }
    }
}
