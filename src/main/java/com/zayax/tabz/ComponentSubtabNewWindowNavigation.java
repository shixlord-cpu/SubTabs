package com.zayax.tabz;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

final class ComponentSubtabNewWindowNavigation {
    private ComponentSubtabNewWindowNavigation() {
    }

    static void openTabzInNewWindow(
            @NotNull Project project,
            @NotNull VirtualFile targetFile
    ) {
        ComponentSubtabNavigation.runWithSwitchGuard(project, () -> {
            InternalPlatformBridge.openFileInNewWindow(project, targetFile);
            ComponentSubtabsManager.attachIfNeeded(project, targetFile);
            ComponentSubtabsManager.syncSelectionForFile(project, targetFile);
        });
    }
}
