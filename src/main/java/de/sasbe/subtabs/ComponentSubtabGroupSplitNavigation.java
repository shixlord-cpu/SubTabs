package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.JBTabsPresentation;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.ui.tabs.impl.TabLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * Opens two files of the same subtab group side by side under a single visible main tab, with one
 * subtab bar per pane.
 *
 * <p>The platform renders a tab strip per editor pane, so the secondary pane's strip is switched off
 * via {@link JBTabsPresentation#setHideTabs(boolean)} instead of hiding individual tabs. Hiding a
 * single {@code TabInfo} left the pane without a valid selection. Because the secondary pane then has
 * no strip at all, its subtab bar carries a spacer of exactly that height so both editors line up.
 *
 * <p>A split always holds exactly two files: picking a third subtab replaces the file of the pane
 * whose bar was used, and picking the partner's file swaps the two panes.
 */
final class ComponentSubtabGroupSplitNavigation {
    /**
     * Switches the group split off. Its behaviour did not hold up in practice and has to be reworked
     * before it is offered again.
     *
     * <p>Every part of the split presentation (hidden tab strip, bar spacer, close-side button, subtab
     * clicks that swap or replace a pane) is driven by an entry in
     * {@link ComponentSubtabGroupSplitRegistry}. Refusing to create such an entry therefore keeps the
     * whole feature out of the way, and the context menu drops its split entries on top of that.
     */
    static final boolean ENABLED = false;

    enum SplitSide {
        LEFT,
        RIGHT
    }

    private ComponentSubtabGroupSplitNavigation() {
    }

    static void openInGroupSplit(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile,
            @NotNull SplitSide side
    ) {
        if (!ENABLED) {
            return;
        }
        if (anchorFile.equals(targetFile)) {
            return;
        }
        if (!ComponentSubtabNavigation.sameSubtabGroup(anchorFile, targetFile)) {
            return;
        }

        ComponentSubtabNavigation.runWithSwitchGuard(
                project,
                () -> openInGroupSplitImpl(project, anchorFile, targetFile, side)
        );
    }

    private static void openInGroupSplitImpl(
            @NotNull Project project,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile,
            @NotNull SplitSide side
    ) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(anchorFile);
        if (match == null) {
            return;
        }

        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        ComponentSubtabGroupSplitRegistry registry = ComponentSubtabGroupSplitRegistry.getInstance(project);
        SubtabGroupSplitPlan plan = SubtabGroupSplitPlan.of(side, anchorFile, targetFile);

        ComponentSubtabGroupSplitRegistry.SplitState existing = registry.findByGroupKey(match.key());
        if (existing != null) {
            if (plan.leftFile().equals(existing.leftFile()) && plan.rightFile().equals(existing.rightFile())) {
                applySplitPresentation(project, existing);
                focusSplitFile(project, existing, targetFile);
                return;
            }
            // Anything else (swapped sides, replaced side) is rebuilt from a single pane, otherwise
            // splitting again would add a third pane instead of rearranging the existing two.
            releaseSplit(project, existing);
            closeUnlessAnchor(manager, existing.leftFile(), anchorFile);
            closeUnlessAnchor(manager, existing.rightFile(), anchorFile);
        }

        EditorWindow anchorWindow = ComponentSubtabEditorLookup.windowForFileOrCurrent(manager, anchorFile);
        if (anchorWindow == null) {
            return;
        }
        if (!anchorWindow.isFileOpen(anchorFile)) {
            manager.openFileWithProviders(anchorFile, false, anchorWindow);
        }

        EditorWindow secondaryWindow =
                anchorWindow.split(SwingConstants.VERTICAL, true, plan.newPaneFile(), false);
        if (secondaryWindow == null || secondaryWindow == anchorWindow) {
            return;
        }

        if (!anchorWindow.isFileOpen(plan.anchorPaneFile())) {
            manager.openFileWithProviders(plan.anchorPaneFile(), false, anchorWindow);
        }

        // Each of the two files must live in exactly one pane, otherwise the group would show
        // several main tabs instead of the single tab this layout is about.
        closeFileOutsideWindow(manager, plan.anchorPaneFile(), anchorWindow);
        closeFileOutsideWindow(manager, plan.newPaneFile(), secondaryWindow);

        ComponentSubtabGroupSplitRegistry.SplitState state = new ComponentSubtabGroupSplitRegistry.SplitState(
                match.key(),
                plan.leftFile(),
                plan.rightFile()
        );
        registry.register(state);
        applySplitPresentation(project, state);
        focusSplitFile(project, state, targetFile);

        // Rearranging panes closes editors, and a close detaches the subtab bar from every editor of
        // that file. Re-applying once the queued editor events are through restores both bars.
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                reapplyAll(project);
            }
        });
    }

    static void focusSplitFile(
            @NotNull Project project,
            @NotNull ComponentSubtabGroupSplitRegistry.SplitState state,
            @NotNull VirtualFile targetFile
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow window = ComponentSubtabEditorLookup.findWindowWithFile(manager, targetFile);
        if (window == null) {
            return;
        }
        manager.setCurrentWindow(window);
        window.setSelectedComposite(targetFile, true);
        ComponentSubtabsManager.syncSelectionForFile(project, state.leftFile());
    }

    /**
     * Re-applies the presentation of every active split. Called from editor lifecycle events because
     * the platform recreates tab strips when panes are rebuilt.
     */
    static void reapplyAll(@NotNull Project project) {
        ComponentSubtabGroupSplitRegistry registry = ComponentSubtabGroupSplitRegistry.getInstance(project);
        for (ComponentSubtabGroupSplitRegistry.SplitState state : registry.all()) {
            applySplitPresentation(project, state);
        }
    }

    static void applySplitPresentation(
            @NotNull Project project,
            @NotNull ComponentSubtabGroupSplitRegistry.SplitState state
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow primaryWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, state.leftFile());
        EditorWindow secondaryWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, state.rightFile());

        if (primaryWindow == null || secondaryWindow == null || primaryWindow == secondaryWindow) {
            releaseSplit(project, state);
            return;
        }

        setTabStripVisible(primaryWindow, true);
        setTabStripVisible(secondaryWindow, false);

        // Both panes carry their own subtab bar. The secondary pane has no main tab strip above it,
        // so its bar needs a spacer of exactly that height to line both editors up.
        ComponentSubtabsManager.attachIfNeeded(project, state.leftFile());
        ComponentSubtabsManager.attachIfNeeded(project, state.rightFile());
        ComponentSubtabsManager.syncSelectionForFile(project, state.leftFile());
        ComponentSubtabsManager.syncSelectionForFile(project, state.rightFile());
        applyBarLayout(project, state.leftFile(), 0, true);
        applyBarLayout(project, state.rightFile(), tabStripHeight(primaryWindow), true);

        ComponentSubtabsManager.refreshOpenStates(project);
        ComponentSubtabMainTabSelectPopup.installOn(project);
        ComponentSubtabMainTabColors.refresh(project);
    }

    static int tabStripHeight(@NotNull EditorWindow window) {
        JBTabs tabs = window.getTabbedPane().getTabs();
        if (!(tabs instanceof JBTabsImpl impl)) {
            return 0;
        }
        Dimension header = impl.getHeaderFitSize();
        if (header != null && header.height > 0) {
            return header.height;
        }
        // The header size is only known once the tabs have been laid out, so before that the tab
        // label itself is the best estimate. Editor lifecycle events re-apply the exact value later.
        int fallback = 0;
        for (TabInfo info : impl.getTabs()) {
            TabLabel label = impl.getTabLabel(info);
            if (label != null) {
                fallback = Math.max(fallback, label.getPreferredSize().height);
            }
        }
        return fallback;
    }

    private static void applyBarLayout(
            @NotNull Project project,
            @NotNull VirtualFile file,
            int topSpacerHeight,
            boolean splitActive
    ) {
        for (ComponentSubtabBarPanel panel : barsFor(project, file)) {
            panel.applySplitLayout(topSpacerHeight, splitActive);
        }
    }

    private static @NotNull List<ComponentSubtabBarPanel> barsFor(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        List<ComponentSubtabBarPanel> panels = new ArrayList<>();
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : ComponentSubtabsManager.editorsFor(manager, file)) {
            ComponentSubtabBarPanel panel = editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY);
            if (panel != null) {
                panels.add(panel);
            }
        }
        return panels;
    }

    /**
     * Reacts to a closed file: because both panes live under one main tab, closing one side closes
     * the other as well.
     */
    static void clearSplitPresentation(@NotNull Project project, @NotNull VirtualFile file) {
        ComponentSubtabGroupSplitRegistry registry = ComponentSubtabGroupSplitRegistry.getInstance(project);
        ComponentSubtabGroupSplitRegistry.SplitState state = registry.findByFile(file);
        if (state == null) {
            return;
        }

        VirtualFile partner = state.partnerOf(file);
        releaseSplit(project, state);

        FileEditorManager manager = FileEditorManager.getInstance(project);
        if (manager.isFileOpen(partner)) {
            manager.closeFile(partner);
        }
    }

    /**
     * Exchanges the two panes of an active split, so the file that was on the right ends up on the left.
     */
    static void swapSides(
            @NotNull Project project,
            @NotNull ComponentSubtabGroupSplitRegistry.SplitState state
    ) {
        openInGroupSplit(project, state.leftFile(), state.rightFile(), SplitSide.LEFT);
    }

    /**
     * Replaces the file of one split pane while the other pane keeps its file. This is how a split
     * stays limited to two subtabs when a third subtab is picked in one of the two bars.
     */
    static void replacePaneFile(
            @NotNull Project project,
            @NotNull ComponentSubtabGroupSplitRegistry.SplitState state,
            @NotNull VirtualFile paneFile,
            @NotNull VirtualFile newFile
    ) {
        if (!state.covers(paneFile) || state.covers(newFile)) {
            return;
        }
        VirtualFile keptFile = state.partnerOf(paneFile);
        boolean paneWasLeft = state.leftFile().equals(paneFile);
        // The kept file acts as the anchor, so the new file has to land on the side of the replaced pane.
        openInGroupSplit(project, keptFile, newFile, paneWasLeft ? SplitSide.LEFT : SplitSide.RIGHT);
    }

    /**
     * Closes one side of an active split and leaves the other file open as an ordinary main tab.
     */
    static void closeSide(
            @NotNull Project project,
            @NotNull ComponentSubtabGroupSplitRegistry.SplitState state,
            @NotNull VirtualFile paneFile
    ) {
        if (!state.covers(paneFile)) {
            return;
        }
        VirtualFile partner = state.partnerOf(paneFile);
        releaseSplit(project, state);

        FileEditorManager manager = FileEditorManager.getInstance(project);
        if (manager.isFileOpen(paneFile)) {
            manager.closeFile(paneFile);
        }
        ComponentSubtabsManager.attachIfNeeded(project, partner);
        ComponentSubtabsManager.syncSelectionForFile(project, partner);
        ComponentSubtabsManager.refreshOpenStates(project);
        ComponentSubtabMainTabSelectPopup.installOn(project);
        ComponentSubtabMainTabColors.refresh(project);
    }

    /**
     * Removes the split bookkeeping and restores the secondary pane's tab strip without closing files.
     */
    private static void releaseSplit(
            @NotNull Project project,
            @NotNull ComponentSubtabGroupSplitRegistry.SplitState state
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow secondaryWindow = ComponentSubtabEditorLookup.findWindowWithFile(manager, state.rightFile());
        if (secondaryWindow != null) {
            setTabStripVisible(secondaryWindow, true);
        }

        ComponentSubtabGroupSplitRegistry.getInstance(project).unregisterGroup(state.groupKey());
        applyBarLayout(project, state.leftFile(), 0, false);
        applyBarLayout(project, state.rightFile(), 0, false);
        ComponentSubtabsManager.attachIfNeeded(project, state.rightFile());
        ComponentSubtabsManager.refreshOpenStates(project);
        ComponentSubtabMainTabSelectPopup.installOn(project);
        ComponentSubtabMainTabColors.refresh(project);
    }

    static boolean isTabStripVisible(@NotNull EditorWindow window) {
        JBTabs tabs = window.getTabbedPane().getTabs();
        return !(tabs instanceof JBTabsPresentation presentation) || !presentation.isHideTabs();
    }

    private static void setTabStripVisible(@NotNull EditorWindow window, boolean visible) {
        JBTabs tabs = window.getTabbedPane().getTabs();
        if (tabs instanceof JBTabsPresentation presentation && presentation.isHideTabs() == visible) {
            presentation.setHideTabs(!visible);
        }
    }

    private static void closeFileOutsideWindow(
            @NotNull FileEditorManagerEx manager,
            @NotNull VirtualFile file,
            @Nullable EditorWindow keepWindow
    ) {
        for (EditorWindow window : manager.getWindows()) {
            if (window == keepWindow || !window.isFileOpen(file)) {
                continue;
            }
            window.closeFile(file);
        }
    }

    private static void closeUnlessAnchor(
            @NotNull FileEditorManagerEx manager,
            @NotNull VirtualFile file,
            @NotNull VirtualFile anchorFile
    ) {
        if (file.equals(anchorFile) || !manager.isFileOpen(file)) {
            return;
        }
        manager.closeFile(file);
    }
}
