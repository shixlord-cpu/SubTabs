package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

/**
 * Central gate for the bidirectional hover highlighting between editor tabs, subtabs and the project
 * tree. Every hover entry point checks here first so the setting can switch the whole view off.
 */
final class SubtabHoverView {
    private SubtabHoverView() {
    }

    static boolean isEnabled() {
        return SubtabsSettings.getInstance().isHoverViewEnabled();
    }

    static boolean isDisabled() {
        return !isEnabled();
    }

    static void runIfEnabled(@NotNull Runnable action) {
        if (isEnabled()) {
            action.run();
        }
    }
}
