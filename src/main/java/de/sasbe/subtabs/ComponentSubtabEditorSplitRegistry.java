package de.sasbe.subtabs;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service(Service.Level.PROJECT)
@State(
        name = "ComponentSubtabEditorSplitState",
        storages = @Storage(StoragePathMacros.PROJECT_FILE)
)
final class ComponentSubtabEditorSplitRegistry implements PersistentStateComponent<ComponentSubtabEditorSplitRegistry.SerializedState> {
    record SplittabPair(
            @NotNull String id,
            @NotNull VirtualFile leftFile,
            @NotNull VirtualFile rightFile,
            long createdAt,
            @Nullable String linkName,
            @Nullable String headerLabel
    ) {
        boolean covers(@NotNull VirtualFile file) {
            return leftFile.equals(file) || rightFile.equals(file);
        }

        @NotNull VirtualFile partnerOf(@NotNull VirtualFile file) {
            return leftFile.equals(file) ? rightFile : leftFile;
        }

        @NotNull SplittabPair withLinkName(@Nullable String name) {
            return new SplittabPair(id, leftFile, rightFile, createdAt, name, headerLabel);
        }

        @NotNull SplittabPair withHeaderLabel(@Nullable String label) {
            return new SplittabPair(id, leftFile, rightFile, createdAt, linkName, label);
        }
    }

    static final class SerializedState {
        public List<StoredPair> pairs = new ArrayList<>();
        public @Nullable String lastPresentedPairId;
        public @Nullable String activePairId;
        public boolean switchDedicatedSessionAtSave;
        public @Nullable StoredNormalEditorLayout normalLayoutBeforeSwitch;

        static final class StoredNormalEditorLayout {
            public int formatVersion = SplittabEditorLayoutSnapshot.FORMAT_VERSION;
            public @Nullable Element layoutElement;
            public int editorWindowCount;
            public @Nullable String selectedFilePath;
            public int currentWindowIndex;
        }

        static final class StoredPair {
            public @Nullable String leftPath;
            public @Nullable String rightPath;
            public long createdAt;
            public @Nullable String linkName;
            public @Nullable String headerLabel;
        }
    }

    private final @NotNull Project project;
    private final Map<String, SplittabPair> pairsById = new LinkedHashMap<>();
    private @Nullable String activePairId;
    private @Nullable String lastPresentedPairId;
    private @Nullable VirtualFile backgroundAnchorFile;
    private @Nullable String pendingExternalOpenPairId;
    private boolean switchDedicatedSessionAtLastSave;
    private @Nullable SplittabEditorLayoutSnapshot.State normalLayoutBeforeSwitch;

    ComponentSubtabEditorSplitRegistry(@NotNull Project project) {
        this.project = project;
    }

    static @NotNull ComponentSubtabEditorSplitRegistry getInstance(@NotNull Project project) {
        return project.getService(ComponentSubtabEditorSplitRegistry.class);
    }

    @Override
    public @Nullable SerializedState getState() {
        SerializedState state = new SerializedState();
        state.lastPresentedPairId = lastPresentedPairId;
        state.activePairId = activePairId;
        for (SplittabPair pair : pairsById.values()) {
            SerializedState.StoredPair stored = new SerializedState.StoredPair();
            stored.leftPath = pair.leftFile().getPath();
            stored.rightPath = pair.rightFile().getPath();
            stored.createdAt = pair.createdAt();
            stored.linkName = pair.linkName();
            stored.headerLabel = pair.headerLabel();
            state.pairs.add(stored);
        }
        SplittabDedicatedViewService dedicated = SplittabDedicatedViewService.getInstance(project);
        if (SplittabDedicatedViewService.usesDedicatedBehavior(project) && dedicated.isDedicatedViewActive()) {
            state.switchDedicatedSessionAtSave = true;
            SplittabEditorLayoutSnapshot.State snapshot = dedicated.normalViewSnapshotForPersistence();
            if (snapshot != null) {
                state.normalLayoutBeforeSwitch = toStoredLayout(snapshot);
            }
        } else {
            state.switchDedicatedSessionAtSave = false;
            state.normalLayoutBeforeSwitch = null;
        }
        return state;
    }

    @Override
    public void loadState(@NotNull SerializedState state) {
        pairsById.clear();
        lastPresentedPairId = state.lastPresentedPairId;
        activePairId = null;
        LocalFileSystem fileSystem = LocalFileSystem.getInstance();
        for (SerializedState.StoredPair stored : state.pairs) {
            if (stored.leftPath == null || stored.rightPath == null) {
                continue;
            }
            VirtualFile left = fileSystem.findFileByPath(stored.leftPath);
            VirtualFile right = fileSystem.findFileByPath(stored.rightPath);
            if (left == null || right == null) {
                continue;
            }
            String id = pairId(left, right);
            SplittabPair pair = new SplittabPair(
                    id,
                    left,
                    right,
                    stored.createdAt > 0 ? stored.createdAt : System.currentTimeMillis(),
                    blankToNull(stored.linkName),
                    blankToNull(stored.headerLabel)
            );
            pairsById.put(id, pair);
        }
        if (lastPresentedPairId != null && !pairsById.containsKey(lastPresentedPairId)) {
            lastPresentedPairId = pairsById.isEmpty() ? null : pairsById.keySet().iterator().next();
        }
        if (state.activePairId != null && pairsById.containsKey(state.activePairId)) {
            activePairId = state.activePairId;
        }
        switchDedicatedSessionAtLastSave = state.switchDedicatedSessionAtSave;
        normalLayoutBeforeSwitch = fromStoredLayout(state.normalLayoutBeforeSwitch);
    }

    boolean switchDedicatedSessionAtLastSave() {
        return switchDedicatedSessionAtLastSave;
    }

    @Nullable SplittabEditorLayoutSnapshot.State normalLayoutBeforeSwitch() {
        return normalLayoutBeforeSwitch == null ? null : SplittabEditorLayoutSnapshot.copy(normalLayoutBeforeSwitch);
    }

    void clearSwitchSessionRestoreHints() {
        switchDedicatedSessionAtLastSave = false;
        normalLayoutBeforeSwitch = null;
    }

    private static @Nullable SerializedState.StoredNormalEditorLayout toStoredLayout(
            @NotNull SplittabEditorLayoutSnapshot.State snapshot
    ) {
        SerializedState.StoredNormalEditorLayout stored = new SerializedState.StoredNormalEditorLayout();
        stored.formatVersion = snapshot.formatVersion;
        stored.layoutElement = snapshot.layoutElement == null ? null : (Element) snapshot.layoutElement.clone();
        stored.editorWindowCount = snapshot.editorWindowCount;
        stored.selectedFilePath = snapshot.selectedFilePath;
        stored.currentWindowIndex = snapshot.currentWindowIndex;
        return stored;
    }

    private static @Nullable SplittabEditorLayoutSnapshot.State fromStoredLayout(
            @Nullable SerializedState.StoredNormalEditorLayout stored
    ) {
        if (stored == null) {
            return null;
        }
        SplittabEditorLayoutSnapshot.State snapshot = new SplittabEditorLayoutSnapshot.State();
        snapshot.formatVersion = stored.formatVersion;
        snapshot.layoutElement = stored.layoutElement == null ? null : (Element) stored.layoutElement.clone();
        snapshot.editorWindowCount = stored.editorWindowCount;
        snapshot.selectedFilePath = stored.selectedFilePath;
        snapshot.currentWindowIndex = stored.currentWindowIndex;
        return snapshot;
    }

    @NotNull SplittabPair register(
            @NotNull VirtualFile leftFile,
            @NotNull VirtualFile rightFile
    ) {
        SplittabPair existing = findPairUnordered(leftFile, rightFile);
        if (existing != null) {
            setActive(existing.id());
            return existing;
        }
        String id = pairId(leftFile, rightFile);
        SplittabPair pair = new SplittabPair(id, leftFile, rightFile, System.currentTimeMillis(), null, null);
        pairsById.put(id, pair);
        setActive(id);
        return pair;
    }

    void setLinkName(@NotNull String pairId, @Nullable String linkName) {
        SplittabPair pair = pairsById.get(pairId);
        if (pair == null) {
            return;
        }
        pairsById.put(pairId, pair.withLinkName(blankToNull(linkName)));
    }

    void setHeaderLabel(@NotNull String pairId, @Nullable String headerLabel) {
        SplittabPair pair = pairsById.get(pairId);
        if (pair == null) {
            return;
        }
        pairsById.put(pairId, pair.withHeaderLabel(blankToNull(headerLabel)));
    }

    @Nullable SplittabPair findById(@NotNull String id) {
        return pairsById.get(id);
    }

    @Nullable SplittabPair findByFile(@NotNull VirtualFile file) {
        SplittabPair active = activePair();
        if (active != null && active.covers(file)) {
            return active;
        }
        for (SplittabPair pair : pairsById.values()) {
            if (pair.covers(file)) {
                return pair;
            }
        }
        return null;
    }

    @Nullable SplittabPair findByFiles(
            @NotNull VirtualFile leftFile,
            @NotNull VirtualFile rightFile
    ) {
        for (SplittabPair pair : pairsById.values()) {
            if (pair.leftFile().equals(leftFile) && pair.rightFile().equals(rightFile)) {
                return pair;
            }
        }
        return null;
    }

    @Nullable SplittabPair findPairUnordered(
            @NotNull VirtualFile firstFile,
            @NotNull VirtualFile secondFile
    ) {
        SplittabPair direct = findByFiles(firstFile, secondFile);
        if (direct != null) {
            return direct;
        }
        return findByFiles(secondFile, firstFile);
    }

    @Nullable SplittabPair activePair() {
        return activePairId == null ? null : pairsById.get(activePairId);
    }

    void setActive(@NotNull String pairId) {
        if (pairsById.containsKey(pairId)) {
            activePairId = pairId;
            rememberPresentedPair(pairId);
        }
    }

    void clearActivePair() {
        activePairId = null;
    }

    /** Familia off: hide splittab session UI but keep saved pairs for later. */
    void pauseForFamiliaShutdown() {
        clearActivePair();
        clearBackgroundAnchor();
        clearPendingExternalOpenPairId();
    }

    void restoreActivePairIfMissing() {
        if (activePairId != null && pairsById.containsKey(activePairId)) {
            return;
        }
        if (lastPresentedPairId != null && pairsById.containsKey(lastPresentedPairId)) {
            activePairId = lastPresentedPairId;
            return;
        }
        for (String id : pairsById.keySet()) {
            activePairId = id;
            return;
        }
    }

    void rememberPresentedPair(@NotNull String pairId) {
        if (pairsById.containsKey(pairId)) {
            lastPresentedPairId = pairId;
        }
    }

    @Nullable String lastPresentedPairId() {
        return lastPresentedPairId;
    }

    boolean hasSavedSplittabs() {
        return !pairsById.isEmpty();
    }

    @Nullable SplittabPair lastSavedPairForQuickOpen() {
        if (lastPresentedPairId != null) {
            SplittabPair presented = pairsById.get(lastPresentedPairId);
            if (presented != null) {
                return presented;
            }
        }
        if (pairsById.isEmpty()) {
            return null;
        }
        String lastId = null;
        for (String id : pairsById.keySet()) {
            lastId = id;
        }
        return lastId == null ? null : pairsById.get(lastId);
    }

    void setBackgroundAnchor(@NotNull VirtualFile file) {
        backgroundAnchorFile = file;
    }

    void clearBackgroundAnchor() {
        backgroundAnchorFile = null;
    }

    @Nullable VirtualFile backgroundAnchorFile() {
        return backgroundAnchorFile;
    }

    void setPendingExternalOpenPairId(@NotNull String pairId) {
        if (pairsById.containsKey(pairId)) {
            pendingExternalOpenPairId = pairId;
        }
    }

    @Nullable String pendingExternalOpenPairId() {
        return pendingExternalOpenPairId;
    }

    void clearPendingExternalOpenPairId() {
        pendingExternalOpenPairId = null;
    }

    @NotNull List<SplittabPair> all() {
        return new ArrayList<>(pairsById.values());
    }

    void reorder(@NotNull String pairId, int targetIndex) {
        if (!pairsById.containsKey(pairId)) {
            return;
        }
        List<String> order = new ArrayList<>(pairsById.keySet());
        int fromIndex = order.indexOf(pairId);
        if (fromIndex < 0) {
            return;
        }
        int clamped = Math.max(0, Math.min(targetIndex, order.size() - 1));
        if (fromIndex == clamped) {
            return;
        }
        order.remove(fromIndex);
        order.add(clamped, pairId);
        LinkedHashMap<String, SplittabPair> reordered = new LinkedHashMap<>();
        for (String id : order) {
            SplittabPair pair = pairsById.get(id);
            if (pair != null) {
                reordered.put(id, pair);
            }
        }
        pairsById.clear();
        pairsById.putAll(reordered);
    }

    void unregister(@NotNull String pairId) {
        pairsById.remove(pairId);
        if (pairId.equals(activePairId)) {
            activePairId = null;
        }
        if (pairId.equals(lastPresentedPairId)) {
            lastPresentedPairId = pairsById.isEmpty() ? null : pairsById.keySet().iterator().next();
        }
    }

    void clear() {
        pairsById.clear();
        activePairId = null;
        lastPresentedPairId = null;
        backgroundAnchorFile = null;
        pendingExternalOpenPairId = null;
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static @NotNull String pairId(
            @NotNull VirtualFile leftFile,
            @NotNull VirtualFile rightFile
    ) {
        return leftFile.getPath() + "|" + rightFile.getPath();
    }
}
