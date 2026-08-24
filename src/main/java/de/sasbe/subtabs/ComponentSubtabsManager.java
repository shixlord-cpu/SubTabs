package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
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
        VirtualFile parent = file.getParent();
        String baseName = ComponentFileNaming.componentBaseName(file.getName());
        if (parent == null || baseName == null) {
            return;
        }

        ComponentSubtabGroupRegistry registry = ComponentSubtabGroupRegistry.getInstance(project);
        ComponentSubtabGroup group = registry.getOrCreateGroup(parent, baseName, file);
        if (group == null) {
            return;
        }

        FileEditorManager manager = FileEditorManager.getInstance(project);
        boolean collapsed = SubtabsCollapseState.getInstance(project).isCollapsed();
        for (FileEditor editor : editorsFor(manager, file)) {
            ComponentSubtabBarPanel existing = editor.getUserData(SUBTAB_BAR_KEY);
            if (existing != null) {
                existing.bind(group, file);
                installBar(project, manager, editor, existing, collapsed);
                continue;
            }

            ComponentSubtabBarPanel panel = registry.createOrReusePanel(group, parent, baseName, file);
            editor.putUserData(SUBTAB_BAR_KEY, panel);
            installBar(project, manager, editor, panel, collapsed);
        }
    }

    static void applyCollapsedState(@NotNull Project project, boolean collapsed) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            ComponentSubtabBarPanel panel = editor.getUserData(SUBTAB_BAR_KEY);
            if (panel == null) {
                continue;
            }
            installBar(project, manager, editor, panel, collapsed);
        }

        for (VirtualFile file : manager.getOpenFiles()) {
            if (ComponentFileNaming.componentBaseName(file.getName()) != null) {
                manager.updateFilePresentation(file);
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
            registry.recyclePanel(parent, baseName, panel);
        }

        ApplicationManager.getApplication().invokeLater(() -> {
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
            @NotNull ComponentSubtabBarPanel panel,
            boolean collapsed
    ) {
        if (collapsed) {
            detachFromSwing(panel);
            manager.removeTopComponent(editor, panel);
            SubtabsExpandOverlay.show(project, editor);
            return;
        }

        SubtabsExpandOverlay.hide(editor);
        detachFromSwing(panel);
        manager.removeTopComponent(editor, panel);
        manager.addTopComponent(editor, panel);
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
