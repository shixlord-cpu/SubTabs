package com.zayax.tabz;

/** Runs once per IDE session without internal application lifecycle APIs. */
final class TabzApplicationStartup {
    private static boolean started;

    private TabzApplicationStartup() {
    }

    static void ensureStarted() {
        if (started) {
            return;
        }
        started = true;
        ComponentSubtabNavigationTargetPopupInstaller.installOnce();
        PerfFileOpenHarness.runIfEnabled();
        DemoNavigationVerifyHarness.runIfEnabled();
    }
}
