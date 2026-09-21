package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

final class SidetabSeparatorColors {
    private SidetabSeparatorColors() {
    }

    static @NotNull Color forFile(@Nullable VirtualFile file) {
        Color groupColor = SubtabGroupColors.colorForFile(file);
        return groupColor != null ? groupColor : defaultSeparatorColor();
    }

    static @NotNull Color defaultSeparatorColor() {
        return JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground();
    }
}
