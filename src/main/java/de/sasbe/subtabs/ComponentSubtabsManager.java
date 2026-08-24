package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

final class ComponentSubtabsManager {
    static final Key<ComponentSubtabBarPanel> SUBTAB_BAR_KEY = Key.create("componentSubtabs.bar");

    private ComponentSubtabsManager() {
    }

    static void attachIfNeeded(@NotNull Project project, @NotNull VirtualFile file) {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        if (!settings.isSubtabsActive() && !settings.isShowCollapseButton()) {
            return;
        }

        ComponentSubtabGroupRegistry registry = ComponentSubtabGroupRegistry.getInstance(project);
        ComponentSubtabGroup group = registry.getOrCreateGroup(file);
        if (group == null) {
            return;
        }

        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : editorsFor(manager, file)) {
            ComponentSubtabBarPanel existing = editor.getUserData(SUBTAB_BAR_KEY);
            if (existing != null) {
                existing.bind(group, file);
                installBar(project, manager, editor, existing);
                continue;
            }

            ComponentSubtabBarPanel panel = registry.createOrReusePanel(group, file);
            editor.putUserData(SUBTAB_BAR_KEY, panel);
            installBar(project, manager, editor, panel);
        }
    }

    static void applyPresentationState(@NotNull Project project) {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        FileEditorManager manager = FileEditorManager.getInstance(project);

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
            return;
        }

        for (FileEditor editor : manager.getAllEditors()) {
            ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
            if (panel == null) {
                SubtabsExpandOverlay.hide(editor);
                continue;
            }
            installBar(project, manager, editor, panel);
        }

        updateTabPresentations(manager);
    }

    static void refreshAllOpenProjects() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed()) {
                applyPresentationState(project);
            }
        }
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
        if (ComponentFileNaming.componentBaseName(file.getName()) == null) {
            return;
        }

        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : editorsFor(manager, file)) {
            ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
            if (panel != null) {
                panel.setDisplayedFile(file);
            }
        }
        refreshOpenStates(project);
    }

    static void refreshOpenStates(@NotNull Project project) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
            if (panel != null) {
                panel.refreshOpenStates();
            }
        }
    }

    static void refreshAppearance(@NotNull Project project) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
            if (panel != null) {
                panel.refreshAppearance();
            }
        }
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

        panel.setCollapseButtonVisible(showCollapseButton);

        if (!active) {
            detachFromSwing(panel);
            manager.removeTopComponent(editor, panel);
            if (showCollapseButton) {
                SubtabsExpandOverlay.show(project, editor);
            } else {
                SubtabsExpandOverlay.hide(editor);
            }
            return;
        }

        SubtabsExpandOverlay.hide(editor);
        detachFromSwing(panel);
        manager.removeTopComponent(editor, panel);
        manager.addTopComponent(editor, panel);
    }

    private static void updateTabPresentations(@NotNull FileEditorManager manager) {
        for (VirtualFile file : manager.getOpenFiles()) {
            if (ComponentFileNaming.componentBaseName(file.getName()) != null) {
                manager.updateFilePresentation(file);
            }
        }
    }

    private static void detachFromSwing(@NotNull ComponentSubtabBarPanel panel) {
        Container parent = panel.getParent();
        if (parent != null) {
            parent.remove(panel);
        }
    }

    private static @NotNull FileEditor[] editorsFor(
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
}
