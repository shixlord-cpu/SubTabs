package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ComponentTabTitles {
    private ComponentTabTitles() {
    }

    static @Nullable String mainTabTitle(boolean subtabsCollapsed, @NotNull VirtualFile file) {
        if (subtabsCollapsed) {
            return null;
        }
        if (ComponentRelatedFiles.find(file) == null) {
            return null;
        }
        return displayGroupedTitle(file);
    }

    static @Nullable String displayGroupedTitle(@NotNull VirtualFile file) {
        String baseName = ComponentFileNaming.componentBaseName(file.getName());
        if (baseName == null) {
            return null;
        }
        if (CustomSubtabRuleMatcher.isFolderGroupKey(baseName)) {
            VirtualFile parent = file.getParent();
            return parent != null ? parent.getName() : ComponentFileNaming.displayName(baseName);
        }
        return ComponentFileNaming.displayName(baseName);
    }

    static @Nullable String displayGroupedTitle(@NotNull String fileName) {
        String baseName = ComponentFileNaming.componentBaseName(fileName);
        if (baseName == null) {
            return null;
        }
        return ComponentFileNaming.displayName(baseName);
    }
}
