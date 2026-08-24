package de.sasbe.subtabs;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.PopupHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.JToggleButton;
import java.awt.Component;
import java.util.function.Supplier;

final class ComponentSubtabBarPopup {
    private ComponentSubtabBarPopup() {
    }

    static void install(
            @NotNull Project project,
            @NotNull JToggleButton button,
            @NotNull VirtualFile targetFile,
            @NotNull Supplier<VirtualFile> displayedFileSupplier
    ) {
        PopupHandler popupHandler = new PopupHandler() {
            @Override
            public void invokePopup(@NotNull Component comp, int x, int y) {
                VirtualFile displayedFile = displayedFileSupplier.get();
                if (targetFile.equals(displayedFile) || button.isSelected()) {
                    return;
                }

                showContextMenu(project, targetFile, comp, x, y);
            }
        };
        button.addMouseListener(popupHandler);
    }

    static void showContextMenu(
            @NotNull Project project,
            @NotNull VirtualFile targetFile,
            @NotNull Component component,
            int x,
            int y
    ) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new OpenInNewTabAction(project, targetFile));
        group.add(new OpenInNewWindowAction(project, targetFile));
        ActionManager.getInstance()
                .createActionPopupMenu("SubTabs.SubtabContextMenu", group)
                .getComponent()
                .show(component, x, y);
    }

    private static final class OpenInNewTabAction extends AnAction {
        private final Project project;
        private final VirtualFile targetFile;

        private OpenInNewTabAction(@NotNull Project project, @NotNull VirtualFile targetFile) {
            super("Sub-Tab im neuen Tab öffnen");
            this.project = project;
            this.targetFile = targetFile;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            ComponentSubtabNewTabNavigation.openSubtabInNewTab(project, targetFile);
        }
    }

    private static final class OpenInNewWindowAction extends AnAction {
        private final Project project;
        private final VirtualFile targetFile;

        private OpenInNewWindowAction(@NotNull Project project, @NotNull VirtualFile targetFile) {
            super("Sub-Tab im neuen Fenster öffnen");
            this.project = project;
            this.targetFile = targetFile;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            ComponentSubtabNewWindowNavigation.openSubtabInNewWindow(project, targetFile);
        }
    }
}
