package de.sasbe.subtabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class ComponentSubtabOrder {
    private ComponentSubtabOrder() {
    }

    static boolean canMoveLeft(@NotNull VirtualFile file) {
        return indexInGroup(file) > 0;
    }

    static boolean canMoveRight(@NotNull VirtualFile file) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        if (match == null) {
            return false;
        }
        int index = indexInGroup(match, file);
        return index >= 0 && index < match.relatedFiles().size() - 1;
    }

    static void moveLeft(@NotNull Project project, @NotNull VirtualFile file) {
        moveBy(project, file, -1);
    }

    static void moveRight(@NotNull Project project, @NotNull VirtualFile file) {
        moveBy(project, file, 1);
    }

    static int previewDropIndexForMoveLeft(@NotNull VirtualFile file) {
        int index = indexInGroup(file);
        return index > 0 ? index - 1 : -1;
    }

    static int previewDropIndexForMoveRight(@NotNull VirtualFile file) {
        return canMoveRight(file) ? indexInGroup(file) + 1 : -1;
    }

    static void reorder(@NotNull Project project, @NotNull VirtualFile file, int dropIndex) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        if (match == null) {
            return;
        }

        List<ComponentRelatedFiles.Entry> entries = new ArrayList<>(match.relatedFiles());
        int fromIndex = indexInGroup(match, file);
        if (fromIndex < 0) {
            return;
        }

        int targetIndex = Math.max(0, Math.min(dropIndex, entries.size()));
        if (fromIndex < targetIndex) {
            targetIndex--;
        }
        if (fromIndex == targetIndex) {
            return;
        }

        ComponentRelatedFiles.Entry moved = entries.remove(fromIndex);
        entries.add(targetIndex, moved);
        applyOrder(project, match.key(), entries);
    }

    static @NotNull List<ComponentRelatedFiles.Entry> applyStoredOrder(
            @NotNull String groupKey,
            @NotNull List<ComponentRelatedFiles.Entry> relatedFiles
    ) {
        List<String> stored = SubtabsSettings.getInstance().getSubtabGroupOrder(groupKey);
        if (stored.isEmpty()) {
            return relatedFiles;
        }

        java.util.Map<String, ComponentRelatedFiles.Entry> byPath = new java.util.LinkedHashMap<>();
        for (ComponentRelatedFiles.Entry entry : relatedFiles) {
            byPath.put(normalizePath(entry.file().getPath()), entry);
        }

        List<ComponentRelatedFiles.Entry> ordered = new ArrayList<>(relatedFiles.size());
        for (String path : stored) {
            ComponentRelatedFiles.Entry entry = byPath.remove(normalizePath(path));
            if (entry != null) {
                ordered.add(entry);
            }
        }
        ordered.addAll(byPath.values());
        return List.copyOf(ordered);
    }

    private static void moveBy(@NotNull Project project, @NotNull VirtualFile file, int delta) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        if (match == null) {
            return;
        }
        int index = indexInGroup(match, file);
        if (index < 0) {
            return;
        }
        int target = index + delta;
        if (target < 0 || target >= match.relatedFiles().size()) {
            return;
        }

        List<ComponentRelatedFiles.Entry> entries = new ArrayList<>(match.relatedFiles());
        ComponentRelatedFiles.Entry moved = entries.remove(index);
        entries.add(target, moved);
        applyOrder(project, match.key(), entries);
    }

    private static void applyOrder(
            @NotNull Project project,
            @NotNull String groupKey,
            @NotNull List<ComponentRelatedFiles.Entry> entries
    ) {
        List<String> paths = new ArrayList<>(entries.size());
        for (ComponentRelatedFiles.Entry entry : entries) {
            paths.add(normalizePath(entry.file().getPath()));
        }
        SubtabsSettings.getInstance().setSubtabGroupOrder(groupKey, paths);
        ComponentRelatedFilesCache.getInstance(project).clear();

        ComponentSubtabGroup group = ComponentSubtabGroupRegistry.getInstance(project)
                .updateGroupOrder(groupKey, entries);
        if (group == null) {
            return;
        }

        for (ComponentSubtabBarPanel panel : ComponentSubtabGroupRegistry.getInstance(project).activePanels()) {
            if (group.contains(panel.displayedFile())) {
                panel.refreshRelatedFiles(group, panel.displayedFile());
            }
        }
    }

    private static int indexInGroup(@NotNull VirtualFile file) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        return match == null ? -1 : indexInGroup(match, file);
    }

    private static int indexInGroup(
            @NotNull ComponentRelatedFiles.Match match,
            @NotNull VirtualFile file
    ) {
        String path = normalizePath(file.getPath());
        List<ComponentRelatedFiles.Entry> entries = match.relatedFiles();
        for (int index = 0; index < entries.size(); index++) {
            if (path.equals(normalizePath(entries.get(index).file().getPath()))) {
                return index;
            }
        }
        return -1;
    }

    private static @NotNull String normalizePath(@NotNull String path) {
        return path.replace('\\', '/');
    }
}
