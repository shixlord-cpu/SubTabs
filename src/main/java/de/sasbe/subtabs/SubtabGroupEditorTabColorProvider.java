package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

final class SubtabGroupEditorTabColorProvider implements EditorTabColorProvider, DumbAware {
    @Override
    public @Nullable Color getEditorTabColor(@NotNull Project project, @NotNull VirtualFile file) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return null;
        }
        if (!SubtabGroupColors.isEnabled()) {
            return null;
        }
        return SubtabGroupMainTabColorRegistry.getInstance(project).get(file);
    }
}
