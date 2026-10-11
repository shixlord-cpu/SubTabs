package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ComponentSubtabsSplittabUi {
    static final Key<SplittabSwitchBarPanel> SPLITTAB_SWITCH_BAR_KEY =
            Key.create("componentSubtabs.splittabSwitchBar");
    static final Key<SplittabPaneHeaderPanel> SPLITTAB_HEADER_KEY =
            Key.create("componentSubtabs.splittabHeader");

    private ComponentSubtabsSplittabUi() {
    }

    static @Nullable ComponentSubtabEditorSplitRegistry.SplittabPair activeSplittabPairForEditor(
            @NotNull Project project,
            @NotNull FileEditor editor
    ) {
        VirtualFile file = editor.getFile();
        if (file == null) {
            return null;
        }
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        if (active == null || !active.covers(file)) {
            return null;
        }
        if (!ComponentSubtabEditorSplitNavigation.shouldPresentSplittabChrome(project, active)) {
            return null;
        }
        return active;
    }

    static void attachForPair(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        attachForFile(project, pair.leftFile(), pair);
        attachForFile(project, pair.rightFile(), pair);
    }

    static void detachForPair(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        detachForFile(project, pair.leftFile());
        detachForFile(project, pair.rightFile());
    }

    static void refreshPairChrome(@NotNull Project project, @NotNull String pairId) {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.findById(pairId);
        if (pair == null) {
            return;
        }
        attachForPair(project, pair);
    }

    /** Updates switch-bar selection and right-pane header after a dedicated pair switch (shared anchor). */
    static void refreshDedicatedPairSwitch(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : ComponentSubtabsManager.editorsFor(manager, pair.leftFile())) {
            SplittabSwitchBarPanel bar = editor.getUserData(SPLITTAB_SWITCH_BAR_KEY);
            if (bar != null) {
                bar.refreshActiveSelection();
            } else {
                attachForFile(project, pair.leftFile(), pair);
            }
        }
        for (FileEditor editor : ComponentSubtabsManager.editorsFor(manager, pair.rightFile())) {
            SplittabPaneHeaderPanel header = editor.getUserData(SPLITTAB_HEADER_KEY);
            if (header != null) {
                header.bind(project, pair);
            } else {
                attachForFile(project, pair.rightFile(), pair);
            }
        }
    }

    static void applyPresentationForProject(@NotNull Project project) {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        for (ComponentSubtabEditorSplitRegistry.SplittabPair pair : registry.all()) {
            if (!ComponentSubtabEditorSplitNavigation.shouldPresentSplittabChrome(project, pair)) {
                continue;
            }
            attachForPair(project, pair);
        }
    }

    static void attachIfNeeded(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        if (active == null || !active.covers(file)) {
            return;
        }
        if (!ComponentSubtabEditorSplitMainTab.isForeground(project, active)) {
            return;
        }
        attachForFile(project, file, active);
    }

    private static void attachForFile(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        FileEditorManagerEx managerEx = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow window = ComponentSubtabEditorLookup.findWindowWithFile(managerEx, file);
        if (window == null) {
            return;
        }

        boolean physicalLeftPane = !ComponentSubtabEditorLookup.isRightSplitPane(managerEx, window);
        for (FileEditor editor : ComponentSubtabsManager.editorsFor(manager, file)) {
            detachSubtabBar(project, manager, editor);

            if (physicalLeftPane && file.equals(pair.leftFile())) {
                attachSwitchBar(project, manager, editor, pair);
            } else if (!physicalLeftPane && file.equals(pair.rightFile())) {
                attachHeader(project, manager, editor, pair);
            } else {
                detachSplittabChrome(manager, editor);
            }
        }
    }

    static void detachForFile(@NotNull Project project, @NotNull VirtualFile file) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : ComponentSubtabsManager.editorsFor(manager, file)) {
            detachSplittabChrome(manager, editor);
        }
    }

    private static void attachSwitchBar(
            @NotNull Project project,
            @NotNull FileEditorManager manager,
            @NotNull FileEditor editor,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        SplittabPaneHeaderPanel header = editor.getUserData(SPLITTAB_HEADER_KEY);
        if (header != null) {
            manager.removeTopComponent(editor, header);
            editor.putUserData(SPLITTAB_HEADER_KEY, null);
        }

        if (!ComponentSubtabsScopedVisibility.splittabSubtabsVisibleForPair(project, pair)) {
            hideSwitchBar(manager, editor);
            applySplittabCollapsedOverlays(project, editor);
            RuleSwitchOverlay.hide(editor);
            return;
        }

        SplittabSwitchBarPanel bar = editor.getUserData(SPLITTAB_SWITCH_BAR_KEY);
        if (bar == null) {
            bar = new SplittabSwitchBarPanel(project);
            editor.putUserData(SPLITTAB_SWITCH_BAR_KEY, bar);
        }
        bar.refresh();
        if (bar.getParent() == null) {
            manager.addTopComponent(editor, bar);
        }
        ComponentSubtabsManager.refreshOverlayIconReserve(project, editor);
        applySplittabSubtabIconOverlays(project, editor, true);
        RuleSwitchOverlay.hide(editor);
        SplittabRestoreOverlay.syncEditor(project, editor);
        ComponentSubtabsManager.relayoutEditorOverlayIcons(editor);
    }

    private static void attachHeader(
            @NotNull Project project,
            @NotNull FileEditorManager manager,
            @NotNull FileEditor editor,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        SplittabSwitchBarPanel bar = editor.getUserData(SPLITTAB_SWITCH_BAR_KEY);
        if (bar != null) {
            manager.removeTopComponent(editor, bar);
            editor.putUserData(SPLITTAB_SWITCH_BAR_KEY, null);
        }

        SplittabPaneHeaderPanel header = editor.getUserData(SPLITTAB_HEADER_KEY);
        if (header == null) {
            header = new SplittabPaneHeaderPanel(project);
            editor.putUserData(SPLITTAB_HEADER_KEY, header);
        }
        header.bind(project, pair);
        boolean subtabsVisible = ComponentSubtabsScopedVisibility.splittabSubtabsVisibleForPair(project, pair);
        header.setTitleVisible(subtabsVisible);
        if (header.getParent() == null) {
            manager.addTopComponent(editor, header);
        }
        ComponentSubtabsManager.refreshOverlayIconReserve(project, editor);
        applySplittabSubtabIconOverlays(project, editor, subtabsVisible);
        RuleSwitchOverlay.hide(editor);
        SplittabRestoreOverlay.syncEditor(project, editor);
        ComponentSubtabsManager.relayoutEditorOverlayIcons(editor);
    }

    private static void hideSwitchBar(
            @NotNull FileEditorManager manager,
            @NotNull FileEditor editor
    ) {
        SplittabSwitchBarPanel bar = editor.getUserData(SPLITTAB_SWITCH_BAR_KEY);
        if (bar != null) {
            manager.removeTopComponent(editor, bar);
            editor.putUserData(SPLITTAB_SWITCH_BAR_KEY, null);
        }
    }

    private static void applySplittabSubtabIconOverlays(
            @NotNull Project project,
            @NotNull FileEditor editor,
            boolean subtabsVisibleForPair
    ) {
        if (!subtabsVisibleForPair) {
            applySplittabCollapsedOverlays(project, editor);
            return;
        }
        SubtabsSettings settings = SubtabsSettings.getInstance();
        if (!shouldShowSubtabIconOnSplittabPane(project, editor)) {
            SubtabsCollapseOverlay.hide(editor);
            SubtabsExpandOverlay.hide(editor);
            SplittabRestoreOverlay.syncEditor(project, editor);
            ComponentSubtabsManager.refreshOverlayIconReserve(project, editor);
            ComponentSubtabsManager.relayoutEditorOverlayIcons(editor);
            return;
        }
        SubtabsExpandOverlay.hide(editor);
        if (settings.isShowCollapseButton()) {
            SubtabsCollapseOverlay.show(project, editor);
        } else {
            SubtabsCollapseOverlay.hide(editor);
        }
    }

    /**
     * With two panes: subtab icon on the top split (stacked) or right split (side by side), same as the split-pair icon.
     */
    private static boolean shouldShowSubtabIconOnSplittabPane(
            @NotNull Project project,
            @NotNull FileEditor editor
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (manager.getWindows().length != 2) {
            return true;
        }
        return ComponentSubtabEditorSplitNavigation.isSplitPairIconHostEditor(project, editor);
    }

    private static void applySplittabCollapsedOverlays(
            @NotNull Project project,
            @NotNull FileEditor editor
    ) {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        SubtabsCollapseOverlay.hide(editor);
        boolean showExpandOnPane = shouldShowSubtabIconOnSplittabPane(project, editor);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = activeSplittabPairForEditor(project, editor);
        if (pair != null && ComponentSubtabsScopedVisibility.splittabSubtabsHiddenByGroupOrGlobal(project, pair)) {
            showExpandOnPane = true;
        }
        if (!showExpandOnPane) {
            SubtabsExpandOverlay.hide(editor);
            SplittabRestoreOverlay.syncEditor(project, editor);
            ComponentSubtabsManager.refreshOverlayIconReserve(project, editor);
            ComponentSubtabsManager.relayoutEditorOverlayIcons(editor);
            return;
        }
        if (settings.isShowCollapseButton()) {
            SubtabsExpandOverlay.show(project, editor);
        } else {
            SubtabsExpandOverlay.hide(editor);
        }
        SplittabRestoreOverlay.syncEditor(project, editor);
        ComponentSubtabsManager.refreshOverlayIconReserve(project, editor);
        ComponentSubtabsManager.relayoutEditorOverlayIcons(editor);
    }

    private static void detachSplittabChrome(
            @NotNull FileEditorManager manager,
            @NotNull FileEditor editor
    ) {
        hideSwitchBar(manager, editor);
        SplittabPaneHeaderPanel header = editor.getUserData(SPLITTAB_HEADER_KEY);
        if (header != null) {
            manager.removeTopComponent(editor, header);
            editor.putUserData(SPLITTAB_HEADER_KEY, null);
        }
        SplittabRestoreOverlay.hide(editor);
    }

    private static void detachSubtabBar(
            @NotNull Project project,
            @NotNull FileEditorManager manager,
            @NotNull FileEditor editor
    ) {
        ComponentSubtabsManager.detachSubtabBarFromEditor(project, editor);
    }
}
