package com.zayax.tabz;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JToggleButton;

import java.awt.Container;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class ComponentSubtabsManager {
    static final Key<ComponentSubtabBarPanel> TABZ_BAR_KEY = Key.create("componentTabz.bar");

    private ComponentSubtabsManager() {
    }

    private static boolean needsRuleSwitchBar(@NotNull VirtualFile file) {
        return SubtabRuleRotation.hasMultipleMatches(file.getName(), ComponentFileNaming.rules());
    }

    private static boolean editorShowsSidetabs(@NotNull FileEditor editor) {
        if (!TabzSettings.getInstance().isSidetabsActive()) {
            return false;
        }
        return SidetabsToggleOverlay.isInstalled(editor)
                || editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY) != null;
    }

    static int overlayIconRightReserveForEditor(@NotNull FileEditor editor) {
        TabzSettings settings = TabzSettings.getInstance();
        if (!settings.isShowCollapseButton()) {
            return 0;
        }
        SidetabBarPanel sidetabBar = editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY);
        if (sidetabBar != null
                && settings.isSidetabsExpanded()
                && settings.getSidetabLayoutMode() == SidetabLayoutMode.BESIDE) {
            return 0;
        }
        return SidetabIconLayout.collapseOverlayIconRowWidth();
    }

    static int splittabHeaderLeftReserveForEditor(@NotNull FileEditor editor) {
        return sidetabBesideColumnReserve(editor, false);
    }

    static int splittabHeaderRightReserveForEditor(@NotNull FileEditor editor) {
        int sidetabColumn = sidetabBesideColumnReserve(editor, true);
        if (sidetabColumn > 0) {
            return sidetabColumn;
        }
        return overlayIconRightReserveForEditor(editor);
    }

    private static int sidetabBesideColumnReserve(@NotNull FileEditor editor, boolean onRightSide) {
        TabzSettings settings = TabzSettings.getInstance();
        if (!settings.isSidetabsActive() || !settings.isSidetabsExpanded()) {
            return 0;
        }
        if (settings.getSidetabLayoutMode() != SidetabLayoutMode.BESIDE) {
            return 0;
        }
        if (settings.isSidetabsOnRight() != onRightSide) {
            return 0;
        }
        SidetabBarPanel sidetabBar = editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY);
        if (sidetabBar == null) {
            return 0;
        }
        int width = sidetabBar.getWidth();
        if (width <= 0) {
            width = sidetabBar.getPreferredSize().width;
        }
        return Math.max(0, width);
    }

    static void refreshOverlayIconReserve(@NotNull Project project, @NotNull FileEditor editor) {
        int tabzBarReserve = overlayIconRightReserveForEditor(editor);
        ComponentSubtabBarPanel tabzBar = editor.getUserData(TABZ_BAR_KEY);
        if (tabzBar != null) {
            tabzBar.setOverlayIconRightReserve(tabzBarReserve);
        }
        SplittabPaneHeaderPanel header = editor.getUserData(ComponentSubtabSplittabUi.SPLITTAB_HEADER_KEY);
        if (header != null) {
            header.setLayoutReserves(
                    splittabHeaderLeftReserveForEditor(editor),
                    splittabHeaderRightReserveForEditor(editor)
            );
        }
        SplittabSwitchBarPanel switchBar = editor.getUserData(ComponentSubtabSplittabUi.SPLITTAB_SWITCH_BAR_KEY);
        if (switchBar != null) {
            switchBar.setOverlayIconRightReserve(tabzBarReserve);
        }
    }

    private static boolean sidetabsCollapsedOn(@NotNull FileEditor editor) {
        return editorShowsSidetabs(editor) && !TabzSettings.getInstance().isSidetabsExpanded();
    }

    static void placeRuleSwitchIcon(@NotNull Project project, @NotNull FileEditor editor) {
        ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
        if (panel == null) {
            RuleSwitchOverlay.hide(editor);
            relayoutCollapseIcons(editor);
            return;
        }
        panel.refreshRuleSwitchButton();
        TabzSettings settings = TabzSettings.getInstance();
        if (panel.isRuleSwitchVisible()
                && settings.isSubtabsActive()
                && settings.isShowCollapseButton()
                && sidetabsCollapsedOn(editor)) {
            panel.hideRuleSwitchOnBar();
            RuleSwitchOverlay.show(project, editor);
        } else {
            RuleSwitchOverlay.hide(editor);
        }
        relayoutCollapseIcons(editor);
    }

    static void scheduleStartupIconRelayout(@NotNull Project project) {
        ApplicationManager.getApplication().invokeLater(
                () -> relayoutStartupCollapseIcons(project),
                ModalityState.nonModal()
        );
    }

    private static void relayoutStartupCollapseIcons(@NotNull Project project) {
        if (project.isDisposed() || !TabzSettings.getInstance().isTabzEnabled()) {
            return;
        }
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            placeRuleSwitchIcon(project, editor);
        }
    }

    static @Nullable FileEditor editorHostingSubtabBar(
            @NotNull Project project,
            @NotNull ComponentSubtabBarPanel panel
    ) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            if (editor.getUserData(TABZ_BAR_KEY) == panel) {
                return editor;
            }
        }
        return null;
    }

    static void attachIfNeeded(@NotNull Project project, @NotNull VirtualFile file) {
        TabzSettings settings = TabzSettings.getInstance();
        if (!settings.isTabzEnabled()) {
            return;
        }

        ComponentSubtabEditorSplitRegistry splitRegistry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair activeSplittab = splitRegistry.activePair();
        if (activeSplittab != null && activeSplittab.covers(file)) {
            if (ComponentSubtabEditorSplitNavigation.shouldPresentSplittabChrome(project, activeSplittab)) {
                ComponentSubtabSplittabUi.attachIfNeeded(project, file);
                return;
            }
            if (!ComponentSubtabNavigation.isSwitchInProgress(project)) {
                ComponentSubtabSplittabUi.detachForFile(project, file);
            }
        }

        if (!settings.isSubtabsActive() && !settings.isShowCollapseButton()) {
            return;
        }

        ComponentSubtabGroupRegistry registry = ComponentSubtabGroupRegistry.getInstance(project);
        ComponentSubtabGroup group = registry.getOrCreateGroup(file);
        if (group == null && !needsRuleSwitchBar(file)) {
            refreshMainTabPresentation(project, file);
            return;
        }
        if (group == null) {
            group = new ComponentSubtabGroup(List.of());
        }

        FileEditorManager manager = FileEditorManager.getInstance(project);
        FileEditor[] editors = editorsFor(manager, file);
        if (editors.length == 0) {
            return;
        }

        ComponentSubtabBarPanel panel = findTabzBarPanel(manager, file, editors);
        if (panel == null) {
            panel = registry.createOrReusePanel(group, file);
        }

        for (FileEditor editor : editors) {
            editor.putUserData(TABZ_BAR_KEY, panel);
            panel.bind(group, file);
            registry.rememberActive(panel);
            installBar(project, manager, editor, panel);
        }
    }

    private static @Nullable ComponentSubtabBarPanel findTabzBarPanel(
            @NotNull FileEditorManager manager,
            @NotNull VirtualFile file,
            @NotNull FileEditor[] preferredEditors
    ) {
        for (FileEditor editor : preferredEditors) {
            ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
            if (panel != null) {
                return panel;
            }
        }
        for (FileEditor editor : manager.getAllEditors()) {
            if (!file.equals(editor.getFile())) {
                continue;
            }
            ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
            if (panel != null) {
                return panel;
            }
        }
        return null;
    }

    static void applyPresentationState(@NotNull Project project) {
        TabzSettings settings = TabzSettings.getInstance();
        FileEditorManager manager = FileEditorManager.getInstance(project);

        if (!settings.isTabzEnabled()) {
            shutdown(project);
            SidetabsManager.applyPresentationState(project);
            return;
        }

        if (!settings.isSubtabsActive() && !settings.isShowCollapseButton()) {
            for (FileEditor editor : manager.getAllEditors()) {
                SubtabsExpandOverlay.hide(editor);
                RuleSwitchOverlay.hide(editor);
                ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
                if (panel != null) {
                    detachFromSwing(panel);
                    manager.removeTopComponent(editor, panel);
                }
            }
            updateTabPresentations(manager);
            refreshProjectViewGroupingOverlay(project);
            SidetabsManager.applyPresentationState(project);
            ComponentSubtabSplittabUi.applyPresentationForProject(project);
            return;
        }

        updateTabPresentations(manager);

        Set<FileEditor> processedEditors = new HashSet<>();
        for (VirtualFile file : manager.getOpenFiles()) {
            if (ComponentFileNaming.componentBaseName(file.getName()) != null) {
                attachIfNeeded(project, file);
            }
            for (FileEditor editor : editorsFor(manager, file)) {
                if (!processedEditors.add(editor)) {
                    continue;
                }
                refreshEditorCollapsePresentation(project, manager, editor);
            }
        }

        for (FileEditor editor : manager.getAllEditors()) {
            if (processedEditors.contains(editor)) {
                continue;
            }
            refreshEditorCollapsePresentation(project, manager, editor);
        }

        refreshProjectViewGroupingOverlay(project);
        SidetabsManager.applyPresentationState(project);
        ComponentSubtabMainTabColors.refresh(project);
        ComponentSubtabSplittabUi.applyPresentationForProject(project);
    }

    private static void refreshEditorCollapsePresentation(
            @NotNull Project project,
            @NotNull FileEditorManager manager,
            @NotNull FileEditor editor
    ) {
        VirtualFile file = editor.getFile();
        if (file != null) {
            ComponentSubtabEditorSplitRegistry registry =
                    ComponentSubtabEditorSplitRegistry.getInstance(project);
            ComponentSubtabEditorSplitRegistry.SplittabPair activeSplittab = registry.activePair();
            if (activeSplittab != null
                    && activeSplittab.covers(file)
                    && ComponentSubtabEditorSplitNavigation.shouldPresentSplittabChrome(project, activeSplittab)) {
                ComponentSubtabSplittabUi.attachIfNeeded(project, file);
                return;
            }
        }
        if (editor.getUserData(ComponentSubtabSplittabUi.SPLITTAB_SWITCH_BAR_KEY) != null
                || editor.getUserData(ComponentSubtabSplittabUi.SPLITTAB_HEADER_KEY) != null) {
            if (file != null) {
                ComponentSubtabSplittabUi.attachIfNeeded(project, file);
            }
            return;
        }
        ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
        if (panel == null) {
            SubtabsExpandOverlay.hide(editor);
            RuleSwitchOverlay.hide(editor);
            return;
        }
        installBar(project, manager, editor, panel);
    }

    static void refreshAllOpenProjects() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed()) {
                applyPresentationState(project);
            }
        }
    }

    static void refreshTabHighlights(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            ComponentSubtabBarPanel tabzPanel = editor.getUserData(TABZ_BAR_KEY);
            if (tabzPanel != null) {
                tabzPanel.refreshTabHighlights();
            }
            SidetabBarPanel sidetabPanel = editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY);
            if (sidetabPanel != null) {
                sidetabPanel.refreshTabHighlights();
            }
        }
    }

    static void rotateTabzRuleForFile(@NotNull Project project, @NotNull VirtualFile file) {
        if (!TabzSettings.getInstance().rotateMatchingSubtabRules(file.getName())) {
            return;
        }
        TabzPresentation.applySettingsChange();
        attachIfNeeded(project, file);
    }

    static void applySettingsChange(@NotNull Project project) {
        if (!TabzSettings.getInstance().isTabzEnabled()) {
            shutdown(project);
            return;
        }

        ComponentRelatedFilesCache.getInstance(project).clear();
        SidetabSectionsCache.getInstance(project).clear();
        ComponentSubtabGroupRegistry.getInstance(project).clearGroups();

        TabzSettings settings = TabzSettings.getInstance();
        if (!settings.isSplittabsEnabled()
                || settings.getSplittabBehaviorMode() != SplittabBehaviorMode.DEDICATED_VIEW) {
            SplittabDedicatedViewService dedicatedView = SplittabDedicatedViewService.getInstance(project);
            if (dedicatedView.isDedicatedViewActive()) {
                dedicatedView.exitDedicatedView(true);
            }
        }
        FileEditorManager manager = FileEditorManager.getInstance(project);
        if (!settings.isSubtabsActive() && !settings.isShowCollapseButton()) {
            applyPresentationState(project);
            ComponentSubtabMainTabSelectPopup.installOn(project);
            return;
        }

        for (VirtualFile file : manager.getOpenFiles()) {
            attachIfNeeded(project, file);
            updateSelectionForFile(project, file);
        }

        for (FileEditor editor : manager.getAllEditors()) {
            ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
            if (panel == null) {
                SubtabsExpandOverlay.hide(editor);
                RuleSwitchOverlay.hide(editor);
            } else {
                panel.refreshRuleSwitchButton();
            }
            SplittabSwitchBarPanel switchBar =
                    editor.getUserData(ComponentSubtabSplittabUi.SPLITTAB_SWITCH_BAR_KEY);
            if (switchBar != null) {
                switchBar.applyOverflowSettings();
            }
        }

        updateTabPresentations(manager);
        refreshOpenStates(project);
        ComponentSubtabMainTabSelectPopup.installOn(project);
        ComponentSubtabMainTabColors.refresh(project);
        SidetabsManager.applyPresentationState(project);
        TabzPresentation.refreshTypography();
    }

    static void detachTabzBarFromEditor(
            @NotNull Project project,
            @NotNull FileEditor editor
    ) {
        ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
        if (panel == null) {
            return;
        }
        FileEditorManager manager = FileEditorManager.getInstance(project);
        SubtabsExpandOverlay.hide(editor);
        RuleSwitchOverlay.hide(editor);
        editor.putUserData(TABZ_BAR_KEY, null);
        detachFromSwing(panel);
        manager.removeTopComponent(editor, panel);
    }

    static void prepareTransfer(
            @NotNull Project project,
            @NotNull FileEditor editor,
            @NotNull VirtualFile targetFile
    ) {
        ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
        if (panel == null) {
            return;
        }

        FileEditorManager manager = FileEditorManager.getInstance(project);
        SubtabsExpandOverlay.hide(editor);
        RuleSwitchOverlay.hide(editor);
        editor.putUserData(TABZ_BAR_KEY, null);
        detachFromSwing(panel);
        manager.removeTopComponent(editor, panel);
        panel.setDisplayedFile(targetFile);
        ComponentSubtabGroupRegistry.getInstance(project).offerTransfer(panel);
    }

    static void syncSelectionForFile(@NotNull Project project, @NotNull VirtualFile file) {
        updateSelectionForFile(project, file);
        refreshMainTabPresentation(project, file);
        refreshOpenStates(project);
        ComponentSubtabBarHover.refreshAllActiveMainTabSync(project);
    }

    static void refreshMainTabPresentation(@NotNull Project project, @NotNull VirtualFile file) {
        if (ComponentTabTitles.displayGroupedTitle(file) == null) {
            return;
        }
        FileEditorManager manager = FileEditorManager.getInstance(project);
        manager.updateFilePresentation(file);
        ComponentSubtabMainTabIcons.refreshFile(project, file);
        ComponentSubtabMainTabIcons.scheduleRefreshAfterPlatformUpdate(project);
    }

    static void refreshAllMainTabPresentations(@NotNull Project project) {
        updateTabPresentations(FileEditorManager.getInstance(project));
    }

    static boolean updateSelectionForFile(@NotNull Project project, @NotNull VirtualFile file) {
        if (ComponentFileNaming.componentBaseName(file.getName()) == null) {
            return false;
        }

        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : editorsFor(manager, file)) {
            ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
            if (panel != null) {
                panel.setDisplayedFile(file);
            }
        }
        return true;
    }

    static void refreshPresentationStates(@NotNull Project project) {
        refreshOpenStates(project);
        ComponentSubtabMainTabErrorWaves.refresh(project);
    }

    static void refreshOpenStates(@NotNull Project project) {
        for (ComponentSubtabBarPanel panel : visibleBars(project)) {
            panel.refreshOpenStates();
        }
    }

    /** Refreshes tabz bars attached to {@code file} (typical after an in-tab tabz swap). */
    static void refreshOpenStatesForFile(@NotNull Project project, @NotNull VirtualFile file) {
        if (ComponentFileNaming.componentBaseName(file.getName()) == null) {
            return;
        }
        LinkedHashSet<ComponentSubtabBarPanel> panels = new LinkedHashSet<>();
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : editorsFor(manager, file)) {
            ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
            if (panel != null) {
                panels.add(panel);
            }
        }
        for (ComponentSubtabBarPanel panel : panels) {
            panel.refreshOpenStates();
        }
    }

    static void refreshModifiedStateForDocument(@NotNull Project project, @NotNull Document document) {
        if (!TabzSettings.getInstance().isSubtabsActive()) {
            return;
        }
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file != null) {
            refreshModifiedStateForFile(
                    project,
                    file,
                    ComponentSubtabFilePresentation.computeForDocument(project, document)
            );
            return;
        }
        for (ComponentSubtabBarPanel panel : visibleBars(project)) {
            panel.refreshModifiedStateForDocument(document);
        }
    }

    static void refreshModifiedStateForFile(@NotNull Project project, @NotNull VirtualFile file) {
        if (!TabzSettings.getInstance().isSubtabsActive()) {
            return;
        }
        refreshModifiedStateForFile(project, file, ComponentSubtabFilePresentation.compute(project, file));
    }

    private static void refreshModifiedStateForFile(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull ComponentSubtabFilePresentation presentation
    ) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (ComponentSubtabBarPanel panel : visibleBars(project)) {
            JToggleButton button = panel.buttonFor(file);
            if (button != null) {
                ComponentSubtabUi.setPresentation(button, presentation.modified(), presentation.hasErrors());
            }
        }
        ComponentSubtabMainTabSelectPopup.refreshModifiedStateForFile(
                project,
                file,
                presentation.modified(),
                presentation.hasErrors()
        );
        ComponentSubtabMainTabErrorWaves.applyToFile(project, file, presentation.hasErrors());
    }

    static void refreshAppearance(@NotNull Project project) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
            if (panel != null) {
                panel.refreshAppearance();
            }
        }
        SidetabsManager.refreshAppearance(project);
    }

    static void detachFromFile(@NotNull Project project, @NotNull VirtualFile file) {
        if (ComponentSubtabGroupRegistry.getInstance(project).hasPendingTransfer()) {
            return;
        }

        VirtualFile parent = file.getParent();
        String baseName = ComponentFileNaming.componentBaseName(file.getName());
        if (parent == null || baseName == null) {
            return;
        }

        FileEditorManager manager = FileEditorManager.getInstance(project);
        ComponentSubtabGroupRegistry registry = ComponentSubtabGroupRegistry.getInstance(project);

        for (FileEditor editor : manager.getAllEditors()) {
            VirtualFile editorFile = editor.getFile();
            if (editorFile == null || !editorFile.equals(file)) {
                continue;
            }

            ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
            if (panel == null) {
                continue;
            }
            SubtabsExpandOverlay.hide(editor);
            RuleSwitchOverlay.hide(editor);
            detachFromSwing(panel);
            manager.removeTopComponent(editor, panel);
            editor.putUserData(TABZ_BAR_KEY, null);
            registry.recyclePanel(file, panel);
        }

        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                registry.onFilePossiblyClosed(file);
            }
        });
    }

    static @Nullable JComponent visibleCollapseButton(@NotNull FileEditor editor) {
        ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
        return panel == null ? null : panel.collapseButtonIfShowing();
    }

    static @Nullable FileEditor selectedEditorFor(
            @NotNull FileEditorManager manager,
            @NotNull VirtualFile file
    ) {
        FileEditor selected = manager.getSelectedEditor();
        if (selected != null && file.equals(selected.getFile())) {
            return selected;
        }

        FileEditor[] editors = manager.getEditors(file);
        return editors.length == 0 ? null : editors[0];
    }

    private static void installBar(
            @NotNull Project project,
            @NotNull FileEditorManager manager,
            @NotNull FileEditor editor,
            @NotNull ComponentSubtabBarPanel panel
    ) {
        TabzSettings settings = TabzSettings.getInstance();
        VirtualFile file = editor.getFile();
        boolean active = settings.isSubtabsActive()
                && (file == null || ComponentSubtabScopedVisibility.tabzVisibleForFile(project, file));
        boolean showCollapseButton = settings.isShowCollapseButton();
        boolean reserveTopRightCollapseIcons = showCollapseButton;

        panel.setCollapseButtonVisible(showCollapseButton && !reserveTopRightCollapseIcons);

        panel.refreshRuleSwitchButton();
        if (!active) {
            panel.setCollapseButtonVisible(false);
            if (panel.isRuleSwitchVisible()) {
                if (showCollapseButton && editorShowsSidetabs(editor)) {
                    detachFromSwing(panel);
                    manager.removeTopComponent(editor, panel);
                    SubtabsCollapseOverlay.hide(editor);
                    SubtabsExpandOverlay.show(project, editor);
                } else {
                    SubtabsExpandOverlay.hide(editor);
                    SubtabsCollapseOverlay.hide(editor);
                    if (panel.getParent() == null) {
                        manager.addTopComponent(editor, panel);
                    }
                }
                relayoutCollapseIcons(editor);
                SidetabBarOverlay.relayout(editor);
                placeRuleSwitchIcon(project, editor);
                return;
            }
            detachFromSwing(panel);
            manager.removeTopComponent(editor, panel);
            SubtabsCollapseOverlay.hide(editor);
            if (showCollapseButton) {
                SubtabsExpandOverlay.show(project, editor);
            } else {
                SubtabsExpandOverlay.hide(editor);
            }
            relayoutCollapseIcons(editor);
            SidetabBarOverlay.relayout(editor);
            placeRuleSwitchIcon(project, editor);
            return;
        }

        SubtabsExpandOverlay.hide(editor);
        if (reserveTopRightCollapseIcons) {
            SubtabsCollapseOverlay.show(project, editor);
        } else {
            SubtabsCollapseOverlay.hide(editor);
        }
        if (panel.getParent() == null) {
            manager.addTopComponent(editor, panel);
        }
        ensureCollapseIconRelayoutOnBarResize(editor, panel);
        refreshOverlayIconReserve(project, editor);
        relayoutCollapseIcons(editor);
        SidetabBarOverlay.relayout(editor);
        placeRuleSwitchIcon(project, editor);
    }

    static void relayoutEditorOverlayIcons(@NotNull FileEditor editor) {
        relayoutCollapseIcons(editor);
    }

    private static void relayoutCollapseIcons(@NotNull FileEditor editor) {
        SidetabsToggleOverlay.relayout(editor);
        SubtabsCollapseOverlay.relayout(editor);
        SplittabRestoreOverlay.relayout(editor);
        SubtabsExpandOverlay.relayout(editor);
        RuleSwitchOverlay.relayout(editor);
    }

    private static final com.intellij.openapi.util.Key<Boolean> BAR_ICON_RELAYOUT_LISTENER_KEY =
            com.intellij.openapi.util.Key.create("componentTabz.barIconRelayoutListener");

    private static void ensureCollapseIconRelayoutOnBarResize(
            @NotNull FileEditor editor,
            @NotNull ComponentSubtabBarPanel panel
    ) {
        if (Boolean.TRUE.equals(editor.getUserData(BAR_ICON_RELAYOUT_LISTENER_KEY))) {
            return;
        }
        panel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent event) {
                relayoutCollapseIcons(editor);
                SidetabBarOverlay.relayout(editor);
            }
        });
        editor.putUserData(BAR_ICON_RELAYOUT_LISTENER_KEY, true);
    }

    private static void refreshProjectViewGroupingOverlay(@NotNull Project project) {
        SubtabsProjectViewGroupingOverlay.refresh(project);
    }

    private static void updateTabPresentations(@NotNull FileEditorManager manager) {
        Project project = manager.getProject();
        for (VirtualFile file : manager.getOpenFiles()) {
            if (ComponentFileNaming.componentBaseName(file.getName()) != null) {
                manager.updateFilePresentation(file);
            }
        }
        if (project != null && !project.isDisposed()) {
            ComponentSubtabMainTabIcons.refresh(project);
        }
    }

    static void shutdown(@NotNull Project project) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            SubtabsExpandOverlay.hide(editor);
            SubtabsCollapseOverlay.hide(editor);
            RuleSwitchOverlay.hide(editor);
            ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
            if (panel != null) {
                detachFromSwing(panel);
                manager.removeTopComponent(editor, panel);
                editor.putUserData(TABZ_BAR_KEY, null);
            }
        }
        ComponentSubtabGroupRegistry.getInstance(project).clearGroups();
        ComponentSubtabMainTabSelectPopup.hideAllPopups(project);
        updateTabPresentations(manager);
    }

    private static void detachFromSwing(@NotNull ComponentSubtabBarPanel panel) {
        Container parent = panel.getParent();
        if (parent != null) {
            parent.remove(panel);
        }
    }

    private static void detachTabzBarFromEditor(@NotNull Project project, @NotNull VirtualFile file) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getEditors(file)) {
            ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
            if (panel == null) {
                continue;
            }
            manager.removeTopComponent(editor, panel);
            editor.putUserData(TABZ_BAR_KEY, null);
        }
    }

    /**
     * {@code getEditors} can come back empty for a file that lives in a secondary split pane, so the
     * open editors are scanned as a fallback.
     */
    static @NotNull FileEditor[] editorsFor(
            @NotNull FileEditorManager manager,
            @NotNull VirtualFile file
    ) {
        FileEditor[] editors = manager.getEditors(file);
        if (editors.length > 0) {
            return editors;
        }

        List<FileEditor> matching = new ArrayList<>();
        for (FileEditor editor : manager.getAllEditors()) {
            if (file.equals(editor.getFile())) {
                matching.add(editor);
            }
        }
        return matching.toArray(FileEditor[]::new);
    }

    private static @NotNull List<ComponentSubtabBarPanel> visibleBars(@NotNull Project project) {
        LinkedHashSet<ComponentSubtabBarPanel> panels = new LinkedHashSet<>(
                ComponentSubtabGroupRegistry.getInstance(project).activePanels()
        );
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            ComponentSubtabBarPanel panel = editor.getUserData(TABZ_BAR_KEY);
            if (panel != null) {
                panels.add(panel);
            }
        }
        return new ArrayList<>(panels);
    }
}
