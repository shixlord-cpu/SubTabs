package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionMenuItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.PopupHandler;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Container;
import java.awt.Window;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Component;
import java.util.function.Supplier;

final class ComponentSubtabBarPopup {
    private static final float POPUP_OPACITY = 0.9f;

    private ComponentSubtabBarPopup() {
    }

    static void install(
            @NotNull Project project,
            @NotNull JToggleButton button,
            @NotNull VirtualFile targetFile,
            @NotNull Supplier<VirtualFile> displayedFileSupplier,
            @NotNull ComponentSubtabBarPanel barPanel
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
                        revealOnly,
                        barPanel
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
        showContextMenu(project, anchorFile, targetFile, component, x, y, false, null);
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
        showContextMenu(project, anchorFile, targetFile, component, x, y, revealOnly, null);
    }

    static void showContextMenu(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile,
            @NotNull Component component,
            int x,
            int y,
            boolean revealOnly,
            @Nullable ComponentSubtabBarPanel barPanel
    ) {
        MenuBuildResult menu = buildMenuWithActions(project, anchorFile, targetFile, revealOnly, barPanel);
        ActionPopupMenu popupMenu = ActionManager.getInstance()
                .createActionPopupMenu("SubTabs.SubtabContextMenu", menu.group());
        popupMenu.getComponent().show(component, x, y);
        attachPopupPresentationAfterShow(
                popupMenu.getComponent(),
                barPanel,
                targetFile,
                menu.moveLeft(),
                menu.moveRight()
        );
    }

    private static void attachPopupPresentationAfterShow(
            @NotNull Component popupRoot,
            @Nullable ComponentSubtabBarPanel barPanel,
            @NotNull VirtualFile targetFile,
            @Nullable MoveSubtabLeftAction moveLeft,
            @Nullable MoveSubtabRightAction moveRight
    ) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            applyPopupOpacity(popupRoot, POPUP_OPACITY);
            if (barPanel != null && moveLeft != null && moveRight != null) {
                JPopupMenu popup = findJPopupMenu(popupRoot);
                if (popup != null) {
                    installReorderPreviewOnPopup(popup, barPanel, targetFile, moveLeft, moveRight);
                }
            }
        });
    }

    private static void applyPopupOpacity(@NotNull Component popupRoot, float opacity) {
        Window window = ComponentUtil.getWindow(popupRoot);
        if (window != null) {
            try {
                window.setOpacity(opacity);
            } catch (UnsupportedOperationException ignored) {
                applyFallbackPopupOpacity(popupRoot, opacity);
            }
            return;
        }
        applyFallbackPopupOpacity(popupRoot, opacity);
    }

    private static void applyFallbackPopupOpacity(@NotNull Component popupRoot, float opacity) {
        JPopupMenu popup = findJPopupMenu(popupRoot);
        if (popup != null) {
            applyComponentTreeOpacity(popup, opacity);
        }
    }

    private static void applyComponentTreeOpacity(@NotNull Component component, float opacity) {
        if (component instanceof JComponent jComponent) {
            Color background = jComponent.getBackground();
            if (background != null) {
                jComponent.setOpaque(true);
                jComponent.setBackground(withOpacity(background, opacity));
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyComponentTreeOpacity(child, opacity);
            }
        }
    }

    private static @NotNull Color withOpacity(@NotNull Color color, float opacity) {
        int alpha = Math.max(0, Math.min(255, Math.round(255f * opacity)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static @Nullable JPopupMenu findJPopupMenu(@NotNull Component component) {
        if (component instanceof JPopupMenu popup) {
            return popup;
        }
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                JPopupMenu found = findJPopupMenu(child);
                if (found != null) {
                    return found;
                }
            }
        }
        Component parent = component.getParent();
        if (parent != null && parent != component) {
            return findJPopupMenu(parent);
        }
        return null;
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
        return buildMenuWithActions(project, anchorFile, targetFile, revealOnly, null, true).group();
    }

    static @NotNull DefaultActionGroup buildAnchorContextMenu(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile,
            boolean revealOnly
    ) {
        return buildMenuWithActions(project, anchorFile, targetFile, revealOnly, null, false).group();
    }

    private static @NotNull MenuBuildResult buildMenuWithActions(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile,
            boolean revealOnly,
            @Nullable ComponentSubtabBarPanel barPanel
    ) {
        return buildMenuWithActions(project, anchorFile, targetFile, revealOnly, barPanel, barPanel != null);
    }

    private static @NotNull MenuBuildResult buildMenuWithActions(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile,
            boolean revealOnly,
            @Nullable ComponentSubtabBarPanel barPanel,
            boolean subtabBarMenu
    ) {
        if (ComponentRelatedFiles.find(targetFile) == null) {
            return new MenuBuildResult(new DefaultActionGroup(), null, null);
        }

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(targetFile);
        MoveSubtabLeftAction moveLeft = null;
        MoveSubtabRightAction moveRight = null;

        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new RevealInProjectViewAction(project, targetFile));
        if (match != null && match.relatedFiles().size() > 1) {
            moveLeft = new MoveSubtabLeftAction(project, targetFile, barPanel);
            moveRight = new MoveSubtabRightAction(project, targetFile, barPanel);
            group.addSeparator();
            group.add(moveLeft);
            group.add(moveRight);
        }
        if (canOfferSplittabCreate(project, anchorFile, targetFile, subtabBarMenu)) {
            group.addSeparator();
            group.add(new CreateEditorSplitAction(project, anchorFile, targetFile));
        }
        if (!revealOnly) {
            group.addSeparator();
            group.add(new OpenInNewTabAction(project, targetFile));
            group.add(new OpenInNewWindowAction(project, targetFile));
        }
        if (SubtabsSettings.getInstance().isFamiliaEnabled()) {
            ComponentSubtabEditorSplitFamiliaMenu.addOpenSavedSplittabActions(
                    group, project, targetFile, null);
        }
        return new MenuBuildResult(group, moveLeft, moveRight);
    }

    private static void installReorderPreviewOnPopup(
            @NotNull JPopupMenu popup,
            @NotNull ComponentSubtabBarPanel barPanel,
            @NotNull VirtualFile targetFile,
            @Nullable MoveSubtabLeftAction moveLeft,
            @Nullable MoveSubtabRightAction moveRight
    ) {
        if (moveLeft == null || moveRight == null) {
            return;
        }

        ChangeListener selectionListener = new ChangeListener() {
            @Override
            public void stateChanged(@NotNull ChangeEvent event) {
                updateReorderPreviewFromSelection(barPanel, targetFile, moveLeft, moveRight);
            }
        };

        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(@NotNull PopupMenuEvent event) {
                javax.swing.MenuSelectionManager.defaultManager().addChangeListener(selectionListener);
            }

            @Override
            public void popupMenuWillBecomeInvisible(@NotNull PopupMenuEvent event) {
                javax.swing.MenuSelectionManager.defaultManager().removeChangeListener(selectionListener);
                barPanel.clearReorderPreview();
            }

            @Override
            public void popupMenuCanceled(@NotNull PopupMenuEvent event) {
                javax.swing.MenuSelectionManager.defaultManager().removeChangeListener(selectionListener);
                barPanel.clearReorderPreview();
            }
        });
    }

    private static void updateReorderPreviewFromSelection(
            @NotNull ComponentSubtabBarPanel barPanel,
            @NotNull VirtualFile targetFile,
            @NotNull MoveSubtabLeftAction moveLeft,
            @NotNull MoveSubtabRightAction moveRight
    ) {
        AnAction highlighted = highlightedMenuAction();
        if (highlighted == moveLeft && ComponentSubtabOrder.canMoveLeft(targetFile)) {
            barPanel.setReorderPreview(ComponentSubtabOrder.previewDropIndexForMoveLeft(targetFile), targetFile);
            return;
        }
        if (highlighted == moveRight && ComponentSubtabOrder.canMoveRight(targetFile)) {
            barPanel.setReorderPreview(ComponentSubtabOrder.previewDropIndexForMoveRight(targetFile), targetFile);
            return;
        }
        barPanel.clearReorderPreview();
    }

    private static @Nullable AnAction highlightedMenuAction() {
        for (javax.swing.MenuElement element : javax.swing.MenuSelectionManager.defaultManager().getSelectedPath()) {
            Component component = element.getComponent();
            if (component instanceof ActionMenuItem menuItem) {
                return menuItem.getAnAction();
            }
        }
        return null;
    }

    static void addSplitActions(
            @NotNull DefaultActionGroup group,
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile
    ) {
        if (!ComponentSubtabGroupSplitNavigation.ENABLED) {
            return;
        }
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

    private static boolean canOfferSplittabCreate(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile,
            boolean fromSubtabBar
    ) {
        if (anchorFile.equals(targetFile)) {
            return false;
        }
        if (isSplittabLinkedFile(project, targetFile)) {
            return false;
        }
        if (!SubtabsSettings.getInstance().isSplittabsEnabled()) {
            return false;
        }
        if (ComponentRelatedFiles.find(targetFile) == null) {
            return false;
        }
        if (fromSubtabBar) {
            return ComponentSubtabNavigation.sameSubtabGroup(anchorFile, targetFile);
        }
        return FileEditorManager.getInstance(project).isFileOpen(anchorFile);
    }

    private static boolean isSplittabLinkedFile(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(project).findByFile(file);
        return pair != null
                && ComponentSubtabEditorSplitNavigation.isSplittabWorkspace(project, pair);
    }

    static final String SPLITTAB_PAIR_ID_KEY = "componentSubtabs.splittabContextPairId";
    static final String SPLITTAB_HEADER_POPUP_INSTALLED = "componentSubtabs.splittabHeaderPopupInstalled";

    static void installSplittabPopup(
            @NotNull Project project,
            @NotNull JToggleButton button,
            @NotNull String pairId,
            @NotNull SplittabSwitchBarPanel barPanel
    ) {
        PopupHandler popupHandler = new PopupHandler() {
            @Override
            public void invokePopup(@NotNull Component comp, int x, int y) {
                showSplittabContextMenu(project, pairId, barPanel, comp, x, y);
            }
        };
        button.addMouseListener(popupHandler);
    }

    static void showSplittabContextMenu(
            @NotNull Project project,
            @NotNull String pairId,
            @NotNull SplittabSwitchBarPanel barPanel,
            @NotNull Component component,
            int x,
            int y
    ) {
        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(project).findById(pairId);
        if (pair == null) {
            return;
        }

        DefaultActionGroup group = buildSplittabSwitchBarContextGroup(project, pairId, barPanel, pair);
        ActionPopupMenu popupMenu = ActionManager.getInstance()
                .createActionPopupMenu("SubTabs.SplittabContextMenu", group);
        popupMenu.getComponent().show(component, x, y);
    }

    static void showSplittabHeaderContextMenu(
            @NotNull Project project,
            @NotNull String pairId,
            @NotNull Component component,
            int x,
            int y
    ) {
        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(project).findById(pairId);
        if (pair == null) {
            return;
        }

        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new RenameSplittabHeaderLabelAction(project, pairId));
        group.add(new RevealInProjectViewAction(project, pair.leftFile()));
        group.addSeparator();
        group.add(new DissolveSplittabAction(project, pairId, null));

        ActionPopupMenu popupMenu = ActionManager.getInstance()
                .createActionPopupMenu("SubTabs.SplittabHeaderContextMenu", group);
        popupMenu.getComponent().show(component, x, y);
    }

    private static @NotNull DefaultActionGroup buildSplittabSwitchBarContextGroup(
            @NotNull Project project,
            @NotNull String pairId,
            @NotNull SplittabSwitchBarPanel barPanel,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new RenameSplittabLinkAction(project, pairId, barPanel));
        group.add(new RevealInProjectViewAction(project, pair.leftFile()));
        if (ComponentSubtabEditorSplitOrder.canMoveLeft(project, pairId)
                || ComponentSubtabEditorSplitOrder.canMoveRight(project, pairId)) {
            group.addSeparator();
            if (ComponentSubtabEditorSplitOrder.canMoveLeft(project, pairId)) {
                group.add(new MoveSplittabLeftAction(project, pairId, barPanel));
            }
            if (ComponentSubtabEditorSplitOrder.canMoveRight(project, pairId)) {
                group.add(new MoveSplittabRightAction(project, pairId, barPanel));
            }
        }
        group.addSeparator();
        group.add(new DissolveSplittabAction(project, pairId, barPanel));
        return group;
    }

    private record MenuBuildResult(
            @NotNull DefaultActionGroup group,
            @Nullable MoveSubtabLeftAction moveLeft,
            @Nullable MoveSubtabRightAction moveRight
    ) {
    }

    private static final class RevealInProjectViewAction extends AnAction {
        private final Project project;
        private final VirtualFile file;

        private RevealInProjectViewAction(@NotNull Project project, @NotNull VirtualFile file) {
            super("Show in Project view");
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
            super("Open subtab in new tab");
            this.project = project;
            this.targetFile = targetFile;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            ComponentSubtabNewTabNavigation.openSubtabInNewTab(project, targetFile);
        }
    }

    private static final class MoveSubtabLeftAction extends AnAction {
        private final Project project;
        private final VirtualFile file;
        private final @Nullable ComponentSubtabBarPanel barPanel;

        private MoveSubtabLeftAction(
                @NotNull Project project,
                @NotNull VirtualFile file,
                @Nullable ComponentSubtabBarPanel barPanel
        ) {
            super("Move left");
            this.project = project;
            this.file = file;
            this.barPanel = barPanel;
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
            event.getPresentation().setEnabled(ComponentSubtabOrder.canMoveLeft(file));
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            try {
                ComponentSubtabOrder.moveLeft(project, file);
            } finally {
                clearPreview();
            }
        }

        private void clearPreview() {
            if (barPanel != null) {
                barPanel.clearReorderPreview();
            }
        }
    }

    private static final class MoveSubtabRightAction extends AnAction {
        private final Project project;
        private final VirtualFile file;
        private final @Nullable ComponentSubtabBarPanel barPanel;

        private MoveSubtabRightAction(
                @NotNull Project project,
                @NotNull VirtualFile file,
                @Nullable ComponentSubtabBarPanel barPanel
        ) {
            super("Move right");
            this.project = project;
            this.file = file;
            this.barPanel = barPanel;
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
            event.getPresentation().setEnabled(ComponentSubtabOrder.canMoveRight(file));
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            try {
                ComponentSubtabOrder.moveRight(project, file);
            } finally {
                clearPreview();
            }
        }

        private void clearPreview() {
            if (barPanel != null) {
                barPanel.clearReorderPreview();
            }
        }
    }

    private static final class RenameSplittabLinkAction extends AnAction {
        private final Project project;
        private final String pairId;
        private final SplittabSwitchBarPanel barPanel;

        private RenameSplittabLinkAction(
                @NotNull Project project,
                @NotNull String pairId,
                @NotNull SplittabSwitchBarPanel barPanel
        ) {
            super("Rename split pair…");
            this.project = project;
            this.pairId = pairId;
            this.barPanel = barPanel;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            ComponentSubtabEditorSplitNaming.renameLinkName(project, pairId, barPanel);
        }
    }

    private static final class RenameSplittabHeaderLabelAction extends AnAction {
        private final Project project;
        private final String pairId;

        private RenameSplittabHeaderLabelAction(@NotNull Project project, @NotNull String pairId) {
            super("Rename label…");
            this.project = project;
            this.pairId = pairId;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            ComponentSubtabEditorSplitNaming.renameHeaderLabel(project, pairId);
        }
    }

    private static final class DissolveSplittabAction extends AnAction {
        private final Project project;
        private final String pairId;
        private final @Nullable SplittabSwitchBarPanel barPanel;

        private DissolveSplittabAction(
                @NotNull Project project,
                @NotNull String pairId,
                @Nullable SplittabSwitchBarPanel barPanel
        ) {
            super("Dissolve split pair");
            this.project = project;
            this.pairId = pairId;
            this.barPanel = barPanel;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            ComponentSubtabEditorSplitPresentation.dissolvePair(project, pairId);
            if (barPanel != null) {
                barPanel.refresh();
            }
        }
    }

    private static final class MoveSplittabLeftAction extends AnAction {
        private final Project project;
        private final String pairId;
        private final SplittabSwitchBarPanel barPanel;

        private MoveSplittabLeftAction(
                @NotNull Project project,
                @NotNull String pairId,
                @NotNull SplittabSwitchBarPanel barPanel
        ) {
            super("Move left");
            this.project = project;
            this.pairId = pairId;
            this.barPanel = barPanel;
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
            event.getPresentation().setEnabled(ComponentSubtabEditorSplitOrder.canMoveLeft(project, pairId));
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            ComponentSubtabEditorSplitOrder.moveLeft(project, pairId);
            barPanel.refresh();
        }
    }

    private static final class MoveSplittabRightAction extends AnAction {
        private final Project project;
        private final String pairId;
        private final SplittabSwitchBarPanel barPanel;

        private MoveSplittabRightAction(
                @NotNull Project project,
                @NotNull String pairId,
                @NotNull SplittabSwitchBarPanel barPanel
        ) {
            super("Move right");
            this.project = project;
            this.pairId = pairId;
            this.barPanel = barPanel;
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
            event.getPresentation().setEnabled(ComponentSubtabEditorSplitOrder.canMoveRight(project, pairId));
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            ComponentSubtabEditorSplitOrder.moveRight(project, pairId);
            barPanel.refresh();
        }
    }

    private static final class OpenInNewWindowAction extends AnAction {
        private final Project project;
        private final VirtualFile targetFile;

        private OpenInNewWindowAction(@NotNull Project project, @NotNull VirtualFile targetFile) {
            super("Open subtab in new window");
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
            super("Close subtab \"" + paneFile.getName() + "\" in split");
            this.project = project;
            this.state = state;
            this.paneFile = paneFile;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            ComponentSubtabGroupSplitNavigation.closeSide(project, state, paneFile);
        }
    }

    private static final class CreateEditorSplitAction extends AnAction {
        private final Project project;
        private final VirtualFile initiatingPaneFile;
        private final VirtualFile linkedFile;

        private CreateEditorSplitAction(
                @NotNull Project project,
                @NotNull VirtualFile initiatingPaneFile,
                @NotNull VirtualFile linkedFile
        ) {
            super("Create split pair");
            this.project = project;
            this.initiatingPaneFile = initiatingPaneFile;
            this.linkedFile = linkedFile;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            ComponentSubtabEditorSplitNavigation.createSplit(project, initiatingPaneFile, linkedFile);
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
                    ? "Open on left in split"
                    : "Open on right in split");
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
