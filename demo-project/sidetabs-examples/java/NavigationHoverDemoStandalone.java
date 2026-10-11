package shop;

/**
 * Tabz demo: Navigate to Declaration on {@code tabzNavigationDemoTarget} below.
 * <p>
 * Open <strong>this file only</strong>, place the caret on {@code tabzNavigationDemoTarget}
 * in {@link #runNavigationHoverDemo()}, press Ctrl+B, then hover the chooser entries.
 * (All targets live in this file so Java resolution does not depend on module wiring.)
 */
public final class NavigationHoverDemoStandalone {
    private NavigationHoverDemoStandalone() {
    }

    public static void runNavigationHoverDemo() {
        String caption = tabzNavigationDemoTarget();
        if (caption.isEmpty()) {
            tabzNavigationDemoTarget("central-id");
            tabzNavigationDemoTarget("feature-id", true);
        }
    }

    public static String tabzNavigationDemoTarget() {
        return "sandbox";
    }

    public static String tabzNavigationDemoTarget(String centralUserId) {
        return centralUserId == null ? "central" : centralUserId;
    }

    public static String tabzNavigationDemoTarget(String featureUserId, boolean feature) {
        if (!feature) {
            return tabzNavigationDemoTarget(featureUserId);
        }
        return featureUserId == null ? "feature" : featureUserId;
    }
}
