package de.sasbe.subtabs;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.PopupHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
                boolean revealOnly = button.isSelected() || ComponentSubtabUi.isOpenElsewhere(button);
                showContextMenu(
                        project,
                        displayedFileSupplier.get(),
                        targetFile,
                        comp,
                        x,
                        y,
                        revealOnly
                );
            }
        };
        button.addMouseListener(popupHandler);
    }

    static void showContextMenu(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile,
            @NotNull Component component,
            int x,
            int y
    ) {
        showContextMenu(project, anchorFile, targetFile, component, x, y, false);
    }

    static void showContextMenu(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile,
            @NotNull Component component,
            int x,
            int y,
            boolean revealOnly
    ) {
        ActionManager.getInstance()
                .createActionPopupMenu(
                        "SubTabs.SubtabContextMenu",
                        buildMenu(project, anchorFile, targetFile, revealOnly)
                )
                .getComponent()
                .show(component, x, y);
    }

    static @NotNull DefaultActionGroup buildMenu(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile
    ) {
        return buildMenu(project, anchorFile, targetFile, false);
    }

    static @NotNull DefaultActionGroup buildMenu(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile,
            boolean revealOnly
    ) {
        if (ComponentRelatedFiles.find(targetFile) == null) {
            return new DefaultActionGroup();
        }

        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new RevealInProjectViewAction(project, targetFile));
        if (!revealOnly) {
            group.add(new OpenInNewTabAction(project, targetFile));
            group.add(new OpenInNewWindowAction(project, targetFile));
        }
        return group;
    }

    static void addSplitActions(
            @NotNull DefaultActionGroup group,
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile
    ) {
        ComponentSubtabGroupSplitRegistry.SplitState split = activeSplitFor(project, targetFile);
        if (split != null) {
            group.add(new CloseGroupSplitSideAction(project, split, targetFile));
            return;
        }
        group.add(new OpenInGroupSplitAction(
                project, anchorFile, targetFile, ComponentSubtabGroupSplitNavigation.SplitSide.LEFT));
        group.add(new OpenInGroupSplitAction(
                project, anchorFile, targetFile, ComponentSubtabGroupSplitNavigation.SplitSide.RIGHT));
    }

    private static @Nullable ComponentSubtabGroupSplitRegistry.SplitState activeSplitFor(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        return ComponentSubtabGroupSplitRegistry.getInstance(project).findByFile(file);
    }

    private static final class RevealInProjectViewAction extends AnAction {
        private final Project project;
        private final VirtualFile file;

        private RevealInProjectViewAction(@NotNull Project project, @NotNull VirtualFile file) {
            super("Im Projektbaum anzeigen");
            this.project = project;
            this.file = file;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            SubtabProjectViewReveal.revealSubtab(project, file);
        }
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

    private static final class CloseGroupSplitSideAction extends AnAction {
        private final Project project;
        private final ComponentSubtabGroupSplitRegistry.SplitState state;
        private final VirtualFile paneFile;

        private CloseGroupSplitSideAction(
                @NotNull Project project,
                @NotNull ComponentSubtabGroupSplitRegistry.SplitState state,
                @NotNull VirtualFile paneFile
        ) {
            super("Sub-Tab \"" + paneFile.getName() + "\" im Split schließen");
            this.project = project;
            this.state = state;
            this.paneFile = paneFile;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            ComponentSubtabGroupSplitNavigation.closeSide(project, state, paneFile);
        }
    }

    private static final class OpenInGroupSplitAction extends AnAction {
        private final Project project;
        private final VirtualFile anchorFile;
        private final VirtualFile targetFile;
        private final ComponentSubtabGroupSplitNavigation.SplitSide side;

        private OpenInGroupSplitAction(
                @NotNull Project project,
                @NotNull VirtualFile anchorFile,
                @NotNull VirtualFile targetFile,
                @NotNull ComponentSubtabGroupSplitNavigation.SplitSide side
        ) {
            super(side == ComponentSubtabGroupSplitNavigation.SplitSide.LEFT
                    ? "Im Split links öffnen"
                    : "Im Split rechts öffnen");
            this.project = project;
            this.anchorFile = anchorFile;
            this.targetFile = targetFile;
            this.side = side;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            ComponentSubtabGroupSplitNavigation.openInGroupSplit(project, anchorFile, targetFile, side);
        }
    }
}
