package shop;

import java.util.Objects;

/**
 * Java fallback for the Familia navigation-popup demo (same sandbox project).
 * Caret on {@link #familiaNavigationDemoTarget()} → Navigate to Declaration shows overloads.
 */
public final class NavigationHoverDemoAnchor {
    private NavigationHoverDemoAnchor() {
    }

    public static String familiaNavigationDemoTarget() {
        return "sandbox";
    }

    public static String familiaNavigationDemoTarget(String centralUserId) {
        return Objects.requireNonNullElse(centralUserId, "central");
    }

    public static String familiaNavigationDemoTarget(String featureUserId, boolean feature) {
        if (!feature) {
            return familiaNavigationDemoTarget(featureUserId);
        }
        return Objects.requireNonNullElse(featureUserId, "feature");
    }

    public static void demoCallSite() {
        familiaNavigationDemoTarget();
    }
}
