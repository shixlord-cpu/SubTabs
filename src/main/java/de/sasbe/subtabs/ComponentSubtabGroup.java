package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class ComponentSubtabGroup {
    private final @NotNull List<ComponentRelatedFiles.Entry> relatedFiles;

    ComponentSubtabGroup(@NotNull List<ComponentRelatedFiles.Entry> relatedFiles) {
        this.relatedFiles = relatedFiles;
    }

    @NotNull List<ComponentRelatedFiles.Entry> relatedFiles() {
        return relatedFiles;
    }

    boolean contains(@NotNull VirtualFile file) {
        for (ComponentRelatedFiles.Entry entry : relatedFiles) {
            if (entry.file().equals(file)) {
                return true;
            }
        }
        return false;
    }
}
