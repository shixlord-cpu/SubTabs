package de.sasbe.subtabs;

import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.Color;

/**
 * Wraps the platform file icon with the group color for tabs and other {@link FileIconProvider} consumers.
 * Base icons are resolved by chaining other providers only — never {@code PsiFile.getIcon()}, which would
 * re-enter {@link FileIconProvider} and overflow the stack.
 */
final class SubtabGroupFileIconProvider implements FileIconProvider, DumbAware {
    private static final ThreadLocal<Boolean> RESOLVING_BASE = ThreadLocal.withInitial(() -> false);

    @Override
    public @Nullable Icon getIcon(
            @NotNull VirtualFile file,
            @Iconable.IconFlags int flags,
            @Nullable Project project
    ) {
        if (Boolean.TRUE.equals(RESOLVING_BASE.get())) {
            return null;
        }
        if (project == null || project.isDisposed() || !SubtabGroupColors.isEnabled()) {
            return null;
        }
        Color groupColor = SubtabGroupColors.colorForFile(file);
        if (groupColor == null) {
            return null;
        }
        Icon base = uncoloredPlatformIcon(file, flags, project);
        if (base == null) {
            return null;
        }
        return IconUtil.colorize(base, groupColor);
    }

    static @Nullable Icon uncoloredPlatformIcon(
            @NotNull VirtualFile file,
            @Iconable.IconFlags int flags,
            @Nullable Project project
    ) {
        RESOLVING_BASE.set(true);
        try {
            for (FileIconProvider provider : FileIconProvider.EP_NAME.getExtensionList()) {
                if (provider instanceof SubtabGroupFileIconProvider) {
                    continue;
                }
                Icon icon = provider.getIcon(file, flags, project);
                if (icon != null) {
                    return icon;
                }
            }
            return ReadAction.compute(() -> FileTypeManager.getInstance().getFileTypeByFile(file).getIcon());
        } finally {
            RESOLVING_BASE.remove();
        }
    }
}
