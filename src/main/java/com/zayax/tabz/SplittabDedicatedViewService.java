package com.zayax.tabz;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Service(Service.Level.PROJECT)
final class SplittabDedicatedViewService {
    private final Project project;
    private boolean dedicatedViewActive;
    private boolean transitioningDedicatedView;
    private boolean suppressDedicatedPairCloseHandler;
    private @Nullable SplittabEditorLayoutSnapshot.State normalViewSnapshot;

    SplittabDedicatedViewService(@NotNull Project project) {
        this.project = project;
    }

    static @NotNull SplittabDedicatedViewService getInstance(@NotNull Project project) {
        return project.getService(SplittabDedicatedViewService.class);
    }

    static boolean usesDedicatedBehavior(@NotNull Project project) {
        TabzSettings settings = TabzSettings.getInstance();
        return settings.isSplittabsEnabled()
                && settings.getSplittabBehaviorMode() == SplittabBehaviorMode.DEDICATED_VIEW;
    }

    boolean isDedicatedViewActive() {
        return dedicatedViewActive && usesDedicatedBehavior(project);
    }

    /** Snapshot of the editor layout captured when entering Switch mode (for project close / reopen). */
    @Nullable SplittabEditorLayoutSnapshot.State normalViewSnapshotForPersistence() {
        return normalViewSnapshot == null ? null : SplittabEditorLayoutSnapshot.copy(normalViewSnapshot);
    }

    /** Re-enter Switch mode after project reopen without closing or recapturing IDE tabs. */
    void restorePersistedDedicatedSession() {
        if (!usesDedicatedBehavior(project) || project.isDisposed()) {
            return;
        }
        dedicatedViewActive = true;
        SplittabRestoreOverlay.syncProject(project);
    }

    /** Leave Switch mode when reopening with saved normal editor tabs instead of the Switch session. */
    void abandonDedicatedViewForNormalLayoutRestore() {
        dedicatedViewActive = false;
        normalViewSnapshot = null;
        SplittabRestoreOverlay.syncProject(project);
    }

    void toggleDedicatedView() {
        if (!usesDedicatedBehavior(project)) {
            ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                    ComponentSubtabEditorSplitRegistry.getInstance(project).lastSavedPairForQuickOpen();
            if (pair != null) {
                ComponentSubtabEditorSplitNavigation.activatePair(project, pair.id());
            }
            return;
        }
        if (isDedicatedViewActive()) {
            exitDedicatedView(true);
        } else {
            enterDedicatedView();
        }
        SplittabRestoreOverlay.syncProject(project);
    }

    void enterDedicatedView() {
        if (!usesDedicatedBehavior(project) || project.isDisposed()) {
            return;
        }
        if (isDedicatedViewActive()) {
            enforceDedicatedWorkspace();
            SplittabRestoreOverlay.syncProject(project);
            return;
        }
        transitioningDedicatedView = true;
        try {
            normalViewSnapshot = SplittabEditorLayoutSnapshot.capture(project);
            closeAllEditorTabs();
            dedicatedViewActive = true;
            ComponentSubtabEditorSplitRegistry registry =
                    ComponentSubtabEditorSplitRegistry.getInstance(project);
            ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.lastSavedPairForQuickOpen();
            if (pair == null) {
                exitDedicatedView(true);
                return;
            }
            ComponentSubtabEditorSplitNavigation.activatePair(project, pair.id());
            enforceDedicatedWorkspace();
        } finally {
            transitioningDedicatedView = false;
        }
    }

    /** Switch-Modus: Splittab-Icon nach Erstellen einer Verknüpfung aktivieren, ohne Layout neu zu laden. */
    void enterDedicatedViewForNewSplittab() {
        if (!usesDedicatedBehavior(project) || project.isDisposed()) {
            return;
        }
        if (isDedicatedViewActive()) {
            enforceDedicatedWorkspace();
            SplittabRestoreOverlay.syncProject(project);
            return;
        }
        transitioningDedicatedView = true;
        try {
            normalViewSnapshot = SplittabEditorLayoutSnapshot.capture(project);
            dedicatedViewActive = true;
            enforceDedicatedWorkspace();
            SplittabRestoreOverlay.syncProject(project);
        } finally {
            transitioningDedicatedView = false;
        }
    }

    void afterDissolveClosePairInDedicatedView() {
        if (!isDedicatedViewActive()) {
            return;
        }
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        if (!registry.hasSavedSplittabs()) {
            exitDedicatedView(true);
            return;
        }
        ComponentSubtabEditorSplitRegistry.SplittabPair next = registry.lastSavedPairForQuickOpen();
        if (next == null) {
            exitDedicatedView(true);
            return;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        for (VirtualFile open : manager.getOpenFiles()) {
            if (!next.covers(open)) {
                manager.closeFile(open);
            }
        }
        ComponentSubtabEditorSplitNavigation.activatePair(project, next.id());
        enforceDedicatedWorkspace();
    }

    /** Tabz off: drop switch-mode state without closing editors or restoring a captured layout. */
    void resetForTabzShutdown() {
        transitioningDedicatedView = true;
        try {
            dedicatedViewActive = false;
            normalViewSnapshot = null;
            suppressDedicatedPairCloseHandler = false;
        } finally {
            transitioningDedicatedView = false;
        }
    }

    void exitDedicatedView(boolean restoreNormalLayout) {
        exitDedicatedView(restoreNormalLayout, null);
    }

    private void exitDedicatedView(boolean restoreNormalLayout, @Nullable VirtualFile focusAfterRestore) {
        if (!usesDedicatedBehavior(project)) {
            return;
        }
        transitioningDedicatedView = true;
        try {
            dedicatedViewActive = false;
            closeAllSplittabEditorTabs(focusAfterRestore);
            ComponentSubtabEditorSplitRegistry.getInstance(project).clearActivePair();
            SplittabEditorLayoutSnapshot.State snapshot = normalViewSnapshot;
            normalViewSnapshot = null;
            if (restoreNormalLayout && snapshot != null) {
                SplittabEditorLayoutSnapshot.restore(project, snapshot, focusAfterRestore);
            }
            SplittabRestoreOverlay.syncProject(project);
        } finally {
            transitioningDedicatedView = false;
        }
    }

    void enforceDedicatedWorkspace() {
        if (!isDedicatedViewActive()) {
            return;
        }
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        if (active == null) {
            ComponentSubtabEditorSplitRegistry.SplittabPair fallback = registry.lastSavedPairForQuickOpen();
            if (fallback != null) {
                registry.setActive(fallback.id());
                active = fallback;
            }
        }
        if (active == null) {
            exitDedicatedView(true);
            return;
        }
        if (dedicatedWorkspaceMatches(active)) {
            return;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        Set<VirtualFile> allowed = Set.of(active.leftFile(), active.rightFile());
        for (VirtualFile open : manager.getOpenFiles()) {
            if (!allowed.contains(open)) {
                manager.closeFile(open);
            }
        }
        ComponentSubtabEditorSplitPresentation.applySplittabPresentation(project, active);
    }

    private boolean dedicatedWorkspaceMatches(
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair active
    ) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (!manager.isFileOpen(active.leftFile()) || !manager.isFileOpen(active.rightFile())) {
            return false;
        }
        for (VirtualFile open : manager.getOpenFiles()) {
            if (!active.covers(open)) {
                return false;
            }
        }
        return true;
    }

    void scheduleAfterDedicatedPairSwitch(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry.SplittabPair pair
    ) {
        scheduleAfterEditorMutation(project, () -> {
            if (project.isDisposed() || !isDedicatedViewActive()) {
                return;
            }
            ComponentSubtabEditorSplitRegistry.SplittabPair active =
                    ComponentSubtabEditorSplitRegistry.getInstance(project).activePair();
            if (active == null || !active.id().equals(pair.id())) {
                return;
            }
            runSuppressingDedicatedCloseHandler(() -> {
                enforceDedicatedWorkspace();
                ComponentSubtabEditorSplitPresentation.applySplittabPresentation(project, active);
            });
        });
    }

    void runSuppressingDedicatedCloseHandler(@NotNull Runnable task) {
        suppressDedicatedPairCloseHandler = true;
        try {
            task.run();
        } finally {
            suppressDedicatedPairCloseHandler = false;
        }
    }

    void onDedicatedPairSideClosed(@NotNull VirtualFile closedFile) {
        if (transitioningDedicatedView
                || suppressDedicatedPairCloseHandler
                || !isDedicatedViewActive()) {
            return;
        }
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair pair = registry.findByFile(closedFile);
        if (pair == null) {
            return;
        }
        FileEditorManager manager = FileEditorManager.getInstance(project);
        VirtualFile partner = pair.partnerOf(closedFile);
        if (manager.isFileOpen(partner)) {
            manager.closeFile(partner);
        }
        registry.unregister(pair.id());
        if (!registry.hasSavedSplittabs()) {
            exitDedicatedView(true);
            return;
        }
        ComponentSubtabEditorSplitRegistry.SplittabPair next = registry.lastSavedPairForQuickOpen();
        if (next == null) {
            exitDedicatedView(true);
            return;
        }
        ComponentSubtabEditorSplitNavigation.activatePair(project, next.id());
        enforceDedicatedWorkspace();
    }

    void onForeignFileOpened(@NotNull VirtualFile file) {
        if (transitioningDedicatedView
                || suppressDedicatedPairCloseHandler
                || !isDedicatedViewActive()) {
            return;
        }
        if (ComponentSubtabNavigation.isSwitchInProgress(project)) {
            return;
        }
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        if (active == null) {
            active = registry.lastSavedPairForQuickOpen();
        }
        if (active != null && active.covers(file)) {
            enforceDedicatedWorkspace();
            return;
        }
        exitDedicatedView(true, file);
    }

    boolean skipBackgroundRestoreOnCreate() {
        return isDedicatedViewActive();
    }

    private void closeAllEditorTabs() {
        FileEditorManagerEx.getInstanceEx(project).closeAllFiles();
    }

    private void closeAllSplittabEditorTabs(@Nullable VirtualFile keepOpen) {
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        FileEditorManager manager = FileEditorManager.getInstance(project);
        Set<VirtualFile> toClose = new HashSet<>();
        for (ComponentSubtabEditorSplitRegistry.SplittabPair pair : registry.all()) {
            toClose.add(pair.leftFile());
            toClose.add(pair.rightFile());
        }
        if (keepOpen != null) {
            toClose.remove(keepOpen);
        }
        for (VirtualFile file : toClose) {
            if (manager.isFileOpen(file)) {
                manager.closeFile(file);
            }
        }
    }

    static void scheduleAfterEditorMutation(@NotNull Project project, @NotNull Runnable task) {
        ApplicationManager.getApplication().invokeLater(
                task,
                ModalityState.nonModal(),
                project.getDisposed()
        );
    }
}
