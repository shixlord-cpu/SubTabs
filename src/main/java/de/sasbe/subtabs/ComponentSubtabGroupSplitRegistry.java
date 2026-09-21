package de.sasbe.subtabs;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service(Service.Level.PROJECT)
final class ComponentSubtabGroupSplitRegistry {
    /**
     * A group split is identified by its two files only. Editor windows are resolved on demand,
     * because window instances become stale as soon as the user rearranges or closes panes.
     */
    record SplitState(
            @NotNull String groupKey,
            @NotNull VirtualFile leftFile,
            @NotNull VirtualFile rightFile
    ) {
        boolean covers(@NotNull VirtualFile file) {
            return leftFile.equals(file) || rightFile.equals(file);
        }

        @NotNull VirtualFile partnerOf(@NotNull VirtualFile file) {
            return leftFile.equals(file) ? rightFile : leftFile;
        }
    }

    private final Map<String, SplitState> splitsByGroupKey = new HashMap<>();

    static @NotNull ComponentSubtabGroupSplitRegistry getInstance(@NotNull Project project) {
        return project.getService(ComponentSubtabGroupSplitRegistry.class);
    }

    @Nullable SplitState findByGroupKey(@NotNull String groupKey) {
        return splitsByGroupKey.get(groupKey);
    }

    @Nullable SplitState findByFile(@NotNull VirtualFile file) {
        for (SplitState state : splitsByGroupKey.values()) {
            if (state.covers(file)) {
                return state;
            }
        }
        return null;
    }

    @NotNull List<SplitState> all() {
        return new ArrayList<>(splitsByGroupKey.values());
    }

    void register(@NotNull SplitState state) {
        splitsByGroupKey.put(state.groupKey(), state);
    }

    void unregisterGroup(@NotNull String groupKey) {
        splitsByGroupKey.remove(groupKey);
    }

    void clear() {
        splitsByGroupKey.clear();
    }
}
