package de.sasbe.subtabs;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service(Service.Level.PROJECT)
final class ComponentSubtabGroupRegistry {
    private final Project project;
    private final Map<String, ComponentSubtabGroup> groupsByKey = new ConcurrentHashMap<>();
    private final Map<String, List<ComponentSubtabBarPanel>> recycledPanelsByKey = new ConcurrentHashMap<>();
    private final Set<ComponentSubtabBarPanel> activePanels = ConcurrentHashMap.newKeySet();
    private ComponentSubtabBarPanel pendingTransfer;

    ComponentSubtabGroupRegistry(@NotNull Project project) {
        this.project = project;
    }

    static @NotNull ComponentSubtabGroupRegistry getInstance(@NotNull Project project) {
        return project.getService(ComponentSubtabGroupRegistry.class);
    }

    @Nullable ComponentSubtabGroup getOrCreateGroup(
            @NotNull VirtualFile parent,
            @NotNull String baseName,
            @NotNull VirtualFile currentFile
    ) {
        String key = componentKey(parent, baseName);
        ComponentSubtabGroup existing = groupsByKey.get(key);
        if (existing != null) {
            return existing;
        }

        var relatedFiles = ComponentRelatedFiles.find(parent, baseName, currentFile);
        if (relatedFiles == null) {
            return null;
        }

        ComponentSubtabGroup group = new ComponentSubtabGroup(relatedFiles);
        groupsByKey.put(key, group);
        return group;
    }

    @NotNull ComponentSubtabBarPanel createOrReusePanel(
            @NotNull ComponentSubtabGroup group,
            @NotNull VirtualFile parent,
            @NotNull String baseName,
            @NotNull VirtualFile displayedFile
    ) {
        ComponentSubtabBarPanel transfer = takePendingTransfer();
        if (transfer != null) {
            transfer.bind(group, displayedFile);
            activePanels.add(transfer);
            return transfer;
        }

        String key = componentKey(parent, baseName);
        List<ComponentSubtabBarPanel> recycled = recycledPanelsByKey.get(key);
        if (recycled != null && !recycled.isEmpty()) {
            ComponentSubtabBarPanel panel = recycled.remove(recycled.size() - 1);
            panel.bind(group, displayedFile);
            activePanels.add(panel);
            return panel;
        }

        ComponentSubtabBarPanel panel = new ComponentSubtabBarPanel(project, group, displayedFile);
        activePanels.add(panel);
        return panel;
    }

    void offerTransfer(@NotNull ComponentSubtabBarPanel panel) {
        pendingTransfer = panel;
        activePanels.remove(panel);
    }

    boolean hasPendingTransfer() {
        return pendingTransfer != null;
    }

    void recyclePanel(
            @NotNull VirtualFile parent,
            @NotNull String baseName,
            @NotNull ComponentSubtabBarPanel panel
    ) {
        activePanels.remove(panel);
        recycledPanelsByKey
                .computeIfAbsent(componentKey(parent, baseName), unused -> new ArrayList<>())
                .add(panel);
    }

    void onFilePossiblyClosed(@NotNull VirtualFile file) {
        VirtualFile parent = file.getParent();
        String baseName = ComponentFileNaming.componentBaseName(file.getName());
        if (parent == null || baseName == null) {
            return;
        }

        if (hasOpenComponentFile(parent, baseName)) {
            return;
        }

        String key = componentKey(parent, baseName);
        groupsByKey.remove(key);
        recycledPanelsByKey.remove(key);
    }

    private @Nullable ComponentSubtabBarPanel takePendingTransfer() {
        ComponentSubtabBarPanel panel = pendingTransfer;
        pendingTransfer = null;
        return panel;
    }

    private boolean hasOpenComponentFile(@NotNull VirtualFile parent, @NotNull String baseName) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (VirtualFile openFile : manager.getOpenFiles()) {
            if (!parent.equals(openFile.getParent())) {
                continue;
            }
            if (baseName.equals(ComponentFileNaming.componentBaseName(openFile.getName()))) {
                return true;
            }
        }
        return false;
    }

    static @NotNull String componentKey(@NotNull VirtualFile parent, @NotNull String baseName) {
        return parent.getPath() + "|" + baseName;
    }
}
