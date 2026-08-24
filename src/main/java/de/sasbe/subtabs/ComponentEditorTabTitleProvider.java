package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ComponentEditorTabTitleProvider implements EditorTabTitleProvider, DumbAware {
    @Override
    public @Nullable String getEditorTabTitle(@NotNull Project project, @NotNull VirtualFile file) {
        return ComponentTabTitles.mainTabTitle(
                SubtabsCollapseState.getInstance(project).isCollapsed(),
                file
        );
    }

    @Override
    public @Nullable String getEditorTabTooltipText(@NotNull Project project, @NotNull VirtualFile file) {
        String baseName = ComponentFileNaming.componentBaseName(file.getName());
        if (baseName == null) {
            return null;
        }
        return file.getPresentableUrl();
    }
}
