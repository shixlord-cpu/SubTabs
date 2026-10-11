package com.zayax.tabz;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.util.ui.JBUI;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service(Service.Level.PROJECT)
@State(
        name = "ComponentSubtabScopedVisibility",
        storages = @Storage(StoragePathMacros.PROJECT_FILE)
)
final class ComponentSubtabScopedVisibility implements PersistentStateComponent<ComponentSubtabScopedVisibility.State> {
    static final class State {
        public Set<String> collapsedSubtabGroupKeys = new LinkedHashSet<>();
        public Set<String> collapsedSidetabFilePaths = new LinkedHashSet<>();
        public Set<String> collapsedSplittabPairIds = new LinkedHashSet<>();
        public Map<String, Integer> sidetabBesideColumnWidthByFilePath = new LinkedHashMap<>();
    }

    private State state = new State();

    static @NotNull ComponentSubtabScopedVisibility getInstance(@NotNull Project project) {
        return project.getService(ComponentSubtabScopedVisibility.class);
    }

    boolean isSubtabGroupCollapsed(@NotNull VirtualFile file) {
        String key = tabzGroupKey(file);
        return key != null && state.collapsedSubtabGroupKeys.contains(key);
    }

    void setSubtabGroupCollapsed(@NotNull Project project, @NotNull VirtualFile file, boolean collapsed) {
        String key = tabzGroupKey(file);
        if (key == null) {
            return;
        }
        if (collapsed) {
            state.collapsedSubtabGroupKeys.add(key);
        } else {
            state.collapsedSubtabGroupKeys.remove(key);
        }
        refreshSubtabGroup(project, key);
    }

    void toggleSubtabGroup(@NotNull Project project, @NotNull VirtualFile file) {
        setSubtabGroupCollapsed(project, file, !isSubtabGroupCollapsed(file));
    }

    boolean isSidetabFileCollapsed(@NotNull VirtualFile file) {
        return state.collapsedSidetabFilePaths.contains(file.getPath());
    }

    void setSidetabFileCollapsed(@NotNull Project project, @NotNull VirtualFile file, boolean collapsed) {
        if (collapsed) {
            state.collapsedSidetabFilePaths.add(file.getPath());
        } else {
            state.collapsedSidetabFilePaths.remove(file.getPath());
        }
        refreshSidetabFile(project, file);
    }

    void toggleSidetabFile(@NotNull Project project, @NotNull VirtualFile file) {
        setSidetabFileCollapsed(project, file, !isSidetabFileCollapsed(file));
    }

    static int sidetabBesideColumnWidthForFile(@NotNull Project project, @NotNull VirtualFile file) {
        Integer override = getInstance(project).state.sidetabBesideColumnWidthByFilePath.get(file.getPath());
        if (override != null) {
            return clampSidetabBesideColumnWidth(override);
        }
        return TabzSettings.getInstance().getSidetabBesideColumnWidth();
    }

    static void storeSidetabBesideColumnWidthForFile(
            @NotNull Project project,
            @NotNull VirtualFile file,
            int width
    ) {
        getInstance(project).state.sidetabBesideColumnWidthByFilePath.put(
                file.getPath(),
                clampSidetabBesideColumnWidth(width)
        );
    }

    static void clearSidetabBesideColumnWidthForFile(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        getInstance(project).state.sidetabBesideColumnWidthByFilePath.remove(file.getPath());
    }

    static void setSidetabBesideColumnWidthForFile(
            @NotNull Project project,
            @NotNull VirtualFile file,
            int width
    ) {
        storeSidetabBesideColumnWidthForFile(project, file, width);
        refreshSidetabFile(project, file);
    }

    static int clampSidetabBesideColumnWidth(int width) {
        int min = JBUI.scale(48);
        int max = JBUI.scale(480);
        return Math.max(min, Math.min(max, width));
    }

    static boolean tabzVisibleForFile(@NotNull Project project, @NotNull VirtualFile file) {
        if (!TabzSettings.getInstance().isSubtabsActive()) {
            return false;
        }
        return !getInstance(project).isSubtabGroupCollapsed(file);
    }

    boolean isSplittabPairTabzCollapsed(@NotNull String pairId) {
        return state.collapsedSplittabPairIds.contains(pairId);
    }

    void setSplittabPairTabzCollapsed(
            @NotNull Project project,
            @NotNull String pairId,
            boolean collapsed
    ) {
        if (collapsed) {
            state.collapsedSplittabPairIds.add(pairId);
        } else {
            state.collapsedSplittabPairIds.remove(pairId);
        }
        refreshSplittabPair(project, pairId);
    }

    void toggleSplittabPairTabzCollapsed(@NotNull Project project, @NotNull String pairId) {
        setSplittabPairTabzCollapsed(project, pairId, !isSplittabPairTabzCollapsed(pairId));
    }

    static boolean splittabTabzVisibleForPair(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        if (!TabzSettings.getInstance().isSubtabsActive()) {
            return false;
        }
        if (getInstance(project).isSplittabPairTabzCollapsed(pair.id())) {
            return false;
        }
        return !getInstance(project).isSubtabGroupCollapsed(pair.leftFile())
                && !getInstance(project).isSubtabGroupCollapsed(pair.rightFile());
    }

    static boolean splittabPairLinkTabzCollapsedOnly(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        if (!TabzSettings.getInstance().isSubtabsActive()) {
            return false;
        }
        ComponentSubtabScopedVisibility visibility = getInstance(project);
        if (!visibility.isSplittabPairTabzCollapsed(pair.id())) {
            return false;
        }
        return !visibility.isSubtabGroupCollapsed(pair.leftFile())
                && !visibility.isSubtabGroupCollapsed(pair.rightFile());
    }

    static boolean splittabTabzHiddenByGroupOrGlobal(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        if (!TabzSettings.getInstance().isSubtabsActive()) {
            return true;
        }
        ComponentSubtabScopedVisibility visibility = getInstance(project);
        return visibility.isSubtabGroupCollapsed(pair.leftFile())
                || visibility.isSubtabGroupCollapsed(pair.rightFile());
    }

    static boolean sidetabsExpandedForFile(@NotNull Project project, @NotNull VirtualFile file) {
        if (!TabzSettings.getInstance().isSidetabsExpanded()) {
            return false;
        }
        return !getInstance(project).isSidetabFileCollapsed(file);
    }

    /**
     * Left-click on the tabz expand/collapse icon: wenn global schon aktiv ist, nur die Gruppe
     * einblenden statt global umzuschalten.
     */
    static void handleTabzIconLeftClick(@NotNull Project project, @NotNull FileEditor editor) {
        ComponentSubtabEditorSplitRegistry.SplittabPair splittabPair =
                ComponentSubtabSplittabUi.activeSplittabPairForEditor(project, editor);
        if (splittabPair != null && TabzSettings.getInstance().isSubtabsActive()) {
            ComponentSubtabScopedVisibility visibility = getInstance(project);
            if (visibility.isSplittabPairTabzCollapsed(splittabPair.id())) {
                visibility.setSplittabPairTabzCollapsed(project, splittabPair.id(), false);
            } else {
                visibility.setSplittabPairTabzCollapsed(project, splittabPair.id(), true);
            }
            return;
        }
        VirtualFile file = editor.getFile();
        if (file != null
                && TabzSettings.getInstance().isSubtabsActive()
                && getInstance(project).isSubtabGroupCollapsed(file)) {
            getInstance(project).setSubtabGroupCollapsed(project, file, false);
            return;
        }
        SubtabsCollapseState.getInstance(project).toggle(project);
    }

    /**
     * Left-click on the sidetab icon: wenn global schon ausgeklappt ist, nur diese Datei
     * einblenden statt global umzuschalten.
     */
    static void handleSidetabIconLeftClick(@NotNull Project project, @NotNull FileEditor editor) {
        VirtualFile file = editor.getFile();
        if (file != null
                && TabzSettings.getInstance().isSidetabsExpanded()
                && getInstance(project).isSidetabFileCollapsed(file)) {
            getInstance(project).setSidetabFileCollapsed(project, file, false);
            return;
        }
        SidetabsCollapseState.getInstance(project).toggle(project);
    }

    @Override
    public @Nullable State getState() {
        State copy = new State();
        copy.collapsedSubtabGroupKeys.addAll(state.collapsedSubtabGroupKeys);
        copy.collapsedSidetabFilePaths.addAll(state.collapsedSidetabFilePaths);
        copy.collapsedSplittabPairIds.addAll(state.collapsedSplittabPairIds);
        copy.sidetabBesideColumnWidthByFilePath.putAll(state.sidetabBesideColumnWidthByFilePath);
        return copy;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
        if (this.state.collapsedSubtabGroupKeys == null) {
            this.state.collapsedSubtabGroupKeys = new LinkedHashSet<>();
        }
        if (this.state.collapsedSidetabFilePaths == null) {
            this.state.collapsedSidetabFilePaths = new LinkedHashSet<>();
        }
        if (this.state.collapsedSplittabPairIds == null) {
            this.state.collapsedSplittabPairIds = new LinkedHashSet<>();
        }
        if (this.state.sidetabBesideColumnWidthByFilePath == null) {
            this.state.sidetabBesideColumnWidthByFilePath = new LinkedHashMap<>();
        }
        pruneMissingPaths();
    }

    void clear() {
        state.collapsedSubtabGroupKeys.clear();
        state.collapsedSidetabFilePaths.clear();
        state.collapsedSplittabPairIds.clear();
        state.sidetabBesideColumnWidthByFilePath.clear();
    }

    private void pruneMissingPaths() {
        LocalFileSystem fileSystem = LocalFileSystem.getInstance();
        state.collapsedSidetabFilePaths.removeIf(path -> fileSystem.findFileByPath(path) == null);
        state.sidetabBesideColumnWidthByFilePath.keySet().removeIf(path -> fileSystem.findFileByPath(path) == null);
    }

    private static @Nullable String tabzGroupKey(@NotNull VirtualFile file) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        return match == null ? null : match.key();
    }

    private static void refreshSubtabGroup(@NotNull Project project, @NotNull String groupKey) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (VirtualFile file : manager.getOpenFiles()) {
            ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
            if (match != null && groupKey.equals(match.key())) {
                ComponentSubtabsManager.attachIfNeeded(project, file);
            }
        }
        ComponentSubtabsManager.applyPresentationState(project);
    }

    private static void refreshSidetabFile(@NotNull Project project, @NotNull VirtualFile file) {
        SidetabsManager.attachIfNeeded(project, file);
        ComponentSubtabsManager.applyPresentationState(project);
    }

    private static void refreshSplittabPair(@NotNull Project project, @NotNull String pairId) {
        ComponentSubtabSplittabUi.refreshPairChrome(project, pairId);
        ComponentSubtabsManager.applyPresentationState(project);
    }
}
