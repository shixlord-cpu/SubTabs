package de.sasbe.subtabs;

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
    static final Key<ComponentSubtabBarPanel> SUBTAB_BAR_KEY = Key.create("componentSubtabs.bar");

    private ComponentSubtabsManager() {
    }

    static void attachIfNeeded(@NotNull Project project, @NotNull VirtualFile file) {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        if (!settings.isFamiliaEnabled()) {
            return;
        }
        if (!settings.isSubtabsActive() && !settings.isShowCollapseButton()) {
            return;
        }

        ComponentSubtabGroupRegistry registry = ComponentSubtabGroupRegistry.getInstance(project);
        ComponentSubtabGroup group = registry.getOrCreateGroup(file);
        if (group == null) {
            refreshMainTabPresentation(project, file);
            return;
        }

        FileEditorManager manager = FileEditorManager.getInstance(project);
        FileEditor[] editors = editorsFor(manager, file);
        if (editors.length == 0) {
            return;
        }

        ComponentSubtabBarPanel panel = findSubtabBarPanel(manager, file, editors);
        if (panel == null) {
            panel = registry.createOrReusePanel(group, file);
        }

        for (FileEditor editor : editors) {
            editor.putUserData(SUBTAB_BAR_KEY, panel);
            panel.bind(group, file);
            registry.rememberActive(panel);
            installBar(project, manager, editor, panel);
        }
    }

    private static @Nullable ComponentSubtabBarPanel findSubtabBarPanel(
            @NotNull FileEditorManager manager,
            @NotNull VirtualFile file,
            @NotNull FileEditor[] preferredEditors
    ) {
        for (FileEditor editor : preferredEditors) {
            ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
            if (panel != null) {
                return panel;
            }
        }
        for (FileEditor editor : manager.getAllEditors()) {
            if (!file.equals(editor.getFile())) {
                continue;
            }
            ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
            if (panel != null) {
                return panel;
            }
        }
        return null;
    }

    static void applyPresentationState(@NotNull Project project) {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        FileEditorManager manager = FileEditorManager.getInstance(project);

        if (!settings.isFamiliaEnabled()) {
            shutdown(project);
            SidetabsManager.applyPresentationState(project);
            return;
        }

        if (!settings.isSubtabsActive() && !settings.isShowCollapseButton()) {
            for (FileEditor editor : manager.getAllEditors()) {
                SubtabsExpandOverlay.hide(editor);
                ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
                if (panel != null) {
                    detachFromSwing(panel);
                    manager.removeTopComponent(editor, panel);
                }
            }
            updateTabPresentations(manager);
            refreshProjectViewGroupingOverlay(project);
            SidetabsManager.applyPresentationState(project);
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
    }

    private static void refreshEditorCollapsePresentation(
            @NotNull Project project,
            @NotNull FileEditorManager manager,
            @NotNull FileEditor editor
    ) {
        ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
        if (panel == null) {
            SubtabsExpandOverlay.hide(editor);
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

    static void rotateSubtabRuleForFile(@NotNull Project project, @NotNull VirtualFile file) {
        if (!SubtabsSettings.getInstance().rotateMatchingSubtabRules(file.getName())) {
            return;
        }
        SubtabsPresentation.applySettingsChange();
        attachIfNeeded(project, file);
    }

    static void applySettingsChange(@NotNull Project project) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            shutdown(project);
            return;
        }

        ComponentRelatedFilesCache.getInstance(project).clear();
        SidetabSectionsCache.getInstance(project).clear();
        ComponentSubtabGroupRegistry.getInstance(project).clearGroups();

        SubtabsSettings settings = SubtabsSettings.getInstance();
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
            ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
            if (panel == null) {
                SubtabsExpandOverlay.hide(editor);
            }
        }

        updateTabPresentations(manager);
        refreshOpenStates(project);
        ComponentSubtabMainTabSelectPopup.installOn(project);
        ComponentSubtabMainTabColors.refresh(project);
        SidetabsManager.applyPresentationState(project);
        SubtabsPresentation.refreshTypography();
    }

    static void prepareTransfer(
            @NotNull Project project,
            @NotNull FileEditor editor,
            @NotNull VirtualFile targetFile
    ) {
        ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
        if (panel == null) {
            return;
        }

        FileEditorManager manager = FileEditorManager.getInstance(project);
        SubtabsExpandOverlay.hide(editor);
        editor.putUserData(SUBTAB_BAR_KEY, null);
        detachFromSwing(panel);
        manager.removeTopComponent(editor, panel);
        panel.setDisplayedFile(targetFile);
        ComponentSubtabGroupRegistry.getInstance(project).offerTransfer(panel);
    }

    static void syncSelectionForFile(@NotNull Project project, @NotNull VirtualFile file) {
        updateSelectionForFile(project, file);
        refreshMainTabPresentation(project, file);
        refreshOpenStates(project);
    }

    static void refreshMainTabPresentation(@NotNull Project project, @NotNull VirtualFile file) {
        if (ComponentTabTitles.displayGroupedTitle(file) == null) {
            return;
        }
        FileEditorManager.getInstance(project).updateFilePresentation(file);
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
            ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
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

    static void refreshModifiedStateForDocument(@NotNull Project project, @NotNull Document document) {
        if (!SubtabsSettings.getInstance().isSubtabsActive()) {
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
        if (!SubtabsSettings.getInstance().isSubtabsActive()) {
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
            ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
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

            ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
            if (panel == null) {
                continue;
            }
            SubtabsExpandOverlay.hide(editor);
            detachFromSwing(panel);
            manager.removeTopComponent(editor, panel);
            editor.putUserData(SUBTAB_BAR_KEY, null);
            registry.recyclePanel(file, panel);
        }

        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                registry.onFilePossiblyClosed(file);
            }
        });
    }

    static @Nullable JComponent visibleCollapseButton(@NotNull FileEditor editor) {
        ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
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
        SubtabsSettings settings = SubtabsSettings.getInstance();
        boolean active = settings.isSubtabsActive();
        boolean showCollapseButton = settings.isShowCollapseButton();
        boolean reserveTopRightCollapseIcons = showCollapseButton;

        panel.setCollapseButtonVisible(showCollapseButton && !reserveTopRightCollapseIcons);

        if (!active) {
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
            return;
        }

        SubtabsExpandOverlay.hide(editor);
        if (reserveTopRightCollapseIcons) {
            SubtabsCollapseOverlay.show(project, editor);
        } else {
            SubtabsCollapseOverlay.hide(editor);
        }
        detachFromSwing(panel);
        manager.removeTopComponent(editor, panel);
        manager.addTopComponent(editor, panel);
        ensureCollapseIconRelayoutOnBarResize(editor, panel);
        relayoutCollapseIcons(editor);
        SidetabBarOverlay.relayout(editor);
    }

    private static void relayoutCollapseIcons(@NotNull FileEditor editor) {
        SidetabsToggleOverlay.relayout(editor);
        SubtabsCollapseOverlay.relayout(editor);
        SubtabsExpandOverlay.relayout(editor);
    }

    private static final com.intellij.openapi.util.Key<Boolean> BAR_ICON_RELAYOUT_LISTENER_KEY =
            com.intellij.openapi.util.Key.create("componentSubtabs.barIconRelayoutListener");

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
        for (VirtualFile file : manager.getOpenFiles()) {
            if (ComponentFileNaming.componentBaseName(file.getName()) != null) {
                manager.updateFilePresentation(file);
            }
        }
    }

    static void shutdown(@NotNull Project project) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            SubtabsExpandOverlay.hide(editor);
            SubtabsCollapseOverlay.hide(editor);
            ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
            if (panel != null) {
                detachFromSwing(panel);
                manager.removeTopComponent(editor, panel);
                editor.putUserData(SUBTAB_BAR_KEY, null);
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

    private static void detachSubtabBarFromEditor(@NotNull Project project, @NotNull VirtualFile file) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getEditors(file)) {
            ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
            if (panel == null) {
                continue;
            }
            manager.removeTopComponent(editor, panel);
            editor.putUserData(SUBTAB_BAR_KEY, null);
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
            ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
            if (panel != null) {
                panels.add(panel);
            }
        }
        return new ArrayList<>(panels);
    }
}
