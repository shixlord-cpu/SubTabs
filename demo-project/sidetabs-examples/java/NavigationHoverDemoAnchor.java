package shop;

import java.util.Objects;

/**
 * Java fallback for the Tabz navigation-popup demo (same sandbox project).
 * Caret on {@link #tabzNavigationDemoTarget()} → Navigate to Declaration shows overloads.
 */
public final class NavigationHoverDemoAnchor {
    private NavigationHoverDemoAnchor() {
    }

    public static String tabzNavigationDemoTarget() {
        return "sandbox";
    }

    public static String tabzNavigationDemoTarget(String centralUserId) {
        return Objects.requireNonNullElse(centralUserId, "central");
    }

    public static String tabzNavigationDemoTarget(String featureUserId, boolean feature) {
        if (!feature) {
            return tabzNavigationDemoTarget(featureUserId);
        }
        return Objects.requireNonNullElse(featureUserId, "feature");
    }

    public static void demoCallSite() {
        tabzNavigationDemoTarget();
    }
}
