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

    @Nullable ComponentSubtabGroup getOrCreateGroup(@NotNull VirtualFile currentFile) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(currentFile);
        if (match == null) {
            return null;
        }

        ComponentSubtabGroup existing = groupsByKey.get(match.key());
        if (existing != null) {
            return existing;
        }

        ComponentSubtabGroup group = new ComponentSubtabGroup(match.relatedFiles());
        groupsByKey.put(match.key(), group);
        SubtabGroupColors.onGroupDiscovered(match.key());
        return group;
    }

    @NotNull ComponentSubtabBarPanel createOrReusePanel(
            @NotNull ComponentSubtabGroup group,
            @NotNull VirtualFile displayedFile
    ) {
        ComponentSubtabBarPanel transfer = takePendingTransfer();
        if (transfer != null) {
            transfer.bind(group, displayedFile);
            activePanels.add(transfer);
            return transfer;
        }

        String key = keyFor(displayedFile);
        List<ComponentSubtabBarPanel> recycled = key == null ? null : recycledPanelsByKey.get(key);
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

    void recyclePanel(@NotNull VirtualFile file, @NotNull ComponentSubtabBarPanel panel) {
        activePanels.remove(panel);
        String key = keyFor(file);
        if (key == null) {
            return;
        }
        recycledPanelsByKey.computeIfAbsent(key, unused -> new ArrayList<>()).add(panel);
    }

    void onFilePossiblyClosed(@NotNull VirtualFile file) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        String key = match != null ? match.key() : keyForClosedFile(file);
        if (key == null) {
            return;
        }

        if (hasOpenGroupFile(key, file)) {
            return;
        }

        groupsByKey.remove(key);
        recycledPanelsByKey.remove(key);
    }

    void clearGroups() {
        groupsByKey.clear();
        recycledPanelsByKey.clear();
    }

    private @Nullable ComponentSubtabBarPanel takePendingTransfer() {
        ComponentSubtabBarPanel panel = pendingTransfer;
        pendingTransfer = null;
        return panel;
    }

    private boolean hasOpenGroupFile(@NotNull String key, @NotNull VirtualFile closingFile) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (VirtualFile openFile : manager.getOpenFiles()) {
            if (openFile.equals(closingFile)) {
                continue;
            }
            if (key.equals(keyFor(openFile))) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable String keyFor(@NotNull VirtualFile file) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        return match == null ? null : match.key();
    }

    private static @Nullable String keyForClosedFile(@NotNull VirtualFile file) {
        VirtualFile parent = file.getParent();
        String baseName = ComponentFileNaming.componentBaseName(file.getName());
        if (parent == null || baseName == null) {
            return null;
        }
        return componentKey(parent, baseName);
    }

    static @NotNull String componentKey(@NotNull VirtualFile parent, @NotNull String baseName) {
        return parent.getPath().replace('\\', '/') + "|" + baseName;
    }
}
