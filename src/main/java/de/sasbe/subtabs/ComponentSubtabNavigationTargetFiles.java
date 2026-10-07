package de.sasbe.subtabs;

import com.intellij.codeInsight.navigation.ItemWithPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ComponentSubtabNavigationTargetFiles {
    private ComponentSubtabNavigationTargetFiles() {
    }

    static boolean isNavigationTargetList(@NotNull javax.swing.JList<?> list) {
        javax.swing.ListModel<?> model = list.getModel();
        if (model.getSize() == 0) {
            return false;
        }
        return resolveVirtualFile(model.getElementAt(0)) != null;
    }

    static @Nullable VirtualFile resolveVirtualFile(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof ItemWithPresentation item) {
            Object pointer = item.getItem();
            if (pointer instanceof SmartPsiElementPointer<?> smart) {
                return virtualFileOf(smart.getElement());
            }
        }
        if (value instanceof PsiElement element) {
            return virtualFileOf(element);
        }
        if (value instanceof NavigationItem navigationItem) {
            PsiElement navigationElement = navigationItem instanceof PsiElement psi ? psi : null;
            if (navigationElement != null) {
                return virtualFileOf(navigationElement);
            }
        }
        return null;
    }

    private static @Nullable VirtualFile virtualFileOf(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }
        PsiFile file = element.getContainingFile();
        return file == null ? null : file.getVirtualFile();
    }
}
