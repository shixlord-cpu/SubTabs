package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ComponentTabTitles {
    private ComponentTabTitles() {
    }

    static @Nullable String mainTabTitle(boolean subtabsCollapsed, @NotNull String fileName) {
        if (subtabsCollapsed) {
            return null;
        }
        String baseName = ComponentFileNaming.componentBaseName(fileName);
        if (baseName == null) {
            return null;
        }
        return ComponentFileNaming.displayName(baseName);
    }
}
