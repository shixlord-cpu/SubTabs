package com.zayax.tabz;

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ComponentEditorTabTitleProvider implements EditorTabTitleProvider, DumbAware {
    @Override
    public @Nullable String getEditorTabTitle(@NotNull Project project, @NotNull VirtualFile file) {
        if (!TabzSettings.getInstance().isTabzEnabled()) {
            return null;
        }
        String splittabTitle = ComponentSubtabEditorSplitPresentation.foregroundSplittabMainTabTitle(project, file);
        if (splittabTitle != null) {
            return splittabTitle;
        }
        boolean collapsed = SubtabsCollapseState.getInstance(project).isCollapsed();
        if (!collapsed) {
            collapsed = !ComponentSubtabScopedVisibility.tabzVisibleForFile(project, file);
        }
        return ComponentTabTitles.mainTabTitle(collapsed, file);
    }

    @Override
    public @Nullable String getEditorTabTooltipText(@NotNull Project project, @NotNull VirtualFile file) {
        if (!TabzSettings.getInstance().isTabzEnabled()) {
            return null;
        }
        String baseName = ComponentFileNaming.componentBaseName(file.getName());
        if (baseName == null) {
            return null;
        }
        return file.getPresentableUrl();
    }
}
