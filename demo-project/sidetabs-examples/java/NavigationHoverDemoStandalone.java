package shop;

/**
 * Familia demo: Navigate to Declaration on {@code familiaNavigationDemoTarget} below.
 * <p>
 * Open <strong>this file only</strong>, place the caret on {@code familiaNavigationDemoTarget}
 * in {@link #runNavigationHoverDemo()}, press Ctrl+B, then hover the chooser entries.
 * (All targets live in this file so Java resolution does not depend on module wiring.)
 */
public final class NavigationHoverDemoStandalone {
    private NavigationHoverDemoStandalone() {
    }

    public static void runNavigationHoverDemo() {
        String caption = familiaNavigationDemoTarget();
        if (caption.isEmpty()) {
            familiaNavigationDemoTarget("central-id");
            familiaNavigationDemoTarget("feature-id", true);
        }
    }

    public static String familiaNavigationDemoTarget() {
        return "sandbox";
    }

    public static String familiaNavigationDemoTarget(String centralUserId) {
        return centralUserId == null ? "central" : centralUserId;
    }

    public static String familiaNavigationDemoTarget(String featureUserId, boolean feature) {
        if (!feature) {
            return familiaNavigationDemoTarget(featureUserId);
        }
        return featureUserId == null ? "feature" : featureUserId;
    }
}
