package de.sasbe.subtabs;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.Color;

/**
 * Tints PSI file icons in the project view (and elsewhere) with the file group's color.
 * Uses the same base-icon resolution as {@link SubtabGroupFileIconProvider} to avoid recursion.
 */
final class SubtabGroupPsiIconProvider extends IconProvider implements DumbAware {
    private static final ThreadLocal<Boolean> RESOLVING_BASE = ThreadLocal.withInitial(() -> false);

    @Override
    public @Nullable Icon getIcon(@NotNull PsiElement element, @Iconable.IconFlags int flags) {
        if (Boolean.TRUE.equals(RESOLVING_BASE.get())) {
            return null;
        }
        if (!SubtabGroupColors.isEnabled() || !(element instanceof PsiFile psiFile)) {
            return null;
        }
        VirtualFile file = psiFile.getVirtualFile();
        if (file == null) {
            return null;
        }
        Color groupColor = SubtabGroupColors.colorForFile(file);
        if (groupColor == null) {
            return null;
        }
        Icon base = uncoloredPsiIcon(element, flags, file);
        if (base == null) {
            return null;
        }
        return IconUtil.colorize(base, groupColor);
    }

    private static @Nullable Icon uncoloredPsiIcon(
            @NotNull PsiElement element,
            @Iconable.IconFlags int flags,
            @NotNull VirtualFile file
    ) {
        RESOLVING_BASE.set(true);
        try {
            for (IconProvider provider : IconProvider.EXTENSION_POINT_NAME.getExtensionList()) {
                if (provider instanceof SubtabGroupPsiIconProvider) {
                    continue;
                }
                Icon icon = provider.getIcon(element, flags);
                if (icon != null) {
                    return icon;
                }
            }
            if (element instanceof Iconable iconable) {
                return iconable.getIcon(flags);
            }
            return SubtabGroupFileIconProvider.uncoloredPlatformIcon(
                    file,
                    flags,
                    element.getProject()
            );
        } finally {
            RESOLVING_BASE.remove();
        }
    }
}
