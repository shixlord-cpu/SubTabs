package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.JTree;
final class ComponentSubtabsFileEditorListener
        implements FileEditorManagerListener, FileEditorManagerListener.Before, DumbAware {
    private static final Key<Boolean> DEFERRED_SWITCH_CHROME_SCHEDULED =
            Key.create("componentSubtabs.deferredSwitchChromeScheduled");
    private static final Key<Boolean> DEFERRED_SELECTION_CHROME_SCHEDULED =
            Key.create("componentSubtabs.deferredSelectionChromeScheduled");
    private static final Key<String> COALESCED_SUBTAB_PLATFORM_OPEN =
            Key.create("componentSubtabs.coalescedSubtabPlatformOpen");
    private static final Key<String> SKIP_SELECTION_CHROME_FOR_FILE =
            Key.create("componentSubtabs.skipSelectionChromeForFile");
    private static final Key<String> CLOSING_EDITOR_FILE =
            Key.create("componentSubtabs.closingEditorFile");

    @TestOnly
    static int deferredSwitchPresentationRunCount;

    @TestOnly
    static int deferredSelectionChromeRunCount;

    @TestOnly
    static void resetDeferredRunCountersForTests() {
        deferredSwitchPresentationRunCount = 0;
        deferredSelectionChromeRunCount = 0;
    }

    static void markCoalescedSubtabPlatformOpen(
            @NotNull Project project,
            @NotNull VirtualFile targetFile
    ) {
        COALESCED_SUBTAB_PLATFORM_OPEN.set(project, targetFile.getPath());
    }

    private static boolean consumeCoalescedSubtabPlatformOpen(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        String path = COALESCED_SUBTAB_PLATFORM_OPEN.get(project);
        if (path == null || !path.equals(file.getPath())) {
            return false;
        }
        COALESCED_SUBTAB_PLATFORM_OPEN.set(project, null);
        return true;
    }

    static void scheduleDeferredSwitchPresentation(
            @NotNull Project project,
            @NotNull VirtualFile targetFile
    ) {
        if (project.isDisposed() || Boolean.TRUE.equals(DEFERRED_SWITCH_CHROME_SCHEDULED.get(project))) {
            return;
        }
        DEFERRED_SWITCH_CHROME_SCHEDULED.set(project, Boolean.TRUE);
        ApplicationManager.getApplication().invokeLater(
                () -> runDeferredSwitchPresentation(project, targetFile),
                ModalityState.nonModal(),
                project.getDisposed()
        );
    }

    private static void runDeferredSwitchPresentation(
            @NotNull Project project,
            @NotNull VirtualFile targetFile
    ) {
        deferredSwitchPresentationRunCount++;
        if (project.isDisposed()) {
            DEFERRED_SWITCH_CHROME_SCHEDULED.set(project, null);
            return;
        }
        try {
            ComponentSubtabsManager.attachIfNeeded(project, targetFile);
            ComponentSubtabGroupSplitNavigation.reapplyAll(project);
            if (ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(project)) {
                ComponentSubtabEditorSplitNavigation.onFileOpened(project, targetFile);
                ComponentSubtabEditorSplitPresentation.reapplyAll(project);
            }
            ComponentSubtabsManager.refreshMainTabPresentation(project, targetFile);
            refreshOpenPopups(project);
            ComponentSubtabMainTabSelectPopup.installOn(project);
            ComponentSubtabMainTabColors.refreshForFile(project, targetFile);
            ComponentSubtabMainTabIcons.scheduleRefreshAfterPlatformUpdate(project);
            ComponentSubtabsManager.refreshOpenStatesForFile(project, targetFile);
            ComponentSubtabBarHover.refreshAllActiveMainTabSync(project);
            ComponentSubtabMainTabSelectPopup.refreshPopupPresentation(project);
            scheduleDeferredSidetabsAttach(project, targetFile);
        } finally {
            SKIP_SELECTION_CHROME_FOR_FILE.set(project, targetFile.getPath());
            DEFERRED_SWITCH_CHROME_SCHEDULED.set(project, null);
        }
    }

    private static void scheduleDeferredSidetabsAttach(
            @NotNull Project project,
            @NotNull VirtualFile targetFile
    ) {
        ApplicationManager.getApplication().invokeLater(
                () -> {
                    if (!project.isDisposed()) {
                        SidetabsManager.attachIfNeeded(project, targetFile);
                    }
                },
                ModalityState.nonModal(),
                project.getDisposed()
        );
    }

    private static void scheduleDeferredSelectionChrome(
            @NotNull Project project,
            @Nullable VirtualFile newFile,
            @Nullable VirtualFile oldFile,
            boolean selectionUpdated
    ) {
        if (project.isDisposed()) {
            return;
        }
        if (Boolean.TRUE.equals(DEFERRED_SWITCH_CHROME_SCHEDULED.get(project))) {
            return;
        }
        if (Boolean.TRUE.equals(DEFERRED_SELECTION_CHROME_SCHEDULED.get(project))) {
            return;
        }
        if (newFile != null) {
            String skipFor = SKIP_SELECTION_CHROME_FOR_FILE.get(project);
            if (skipFor != null && skipFor.equals(newFile.getPath())) {
                SKIP_SELECTION_CHROME_FOR_FILE.set(project, null);
                return;
            }
        }
        DEFERRED_SELECTION_CHROME_SCHEDULED.set(project, Boolean.TRUE);
        ApplicationManager.getApplication().invokeLater(
                () -> runDeferredSelectionChrome(project, newFile, oldFile, selectionUpdated),
                ModalityState.nonModal(),
                project.getDisposed()
        );
    }

    private static void runDeferredSelectionChrome(
            @NotNull Project project,
            @Nullable VirtualFile newFile,
            @Nullable VirtualFile oldFile,
            boolean selectionUpdated
    ) {
        deferredSelectionChromeRunCount++;
        if (project.isDisposed()) {
            DEFERRED_SELECTION_CHROME_SCHEDULED.set(project, null);
            return;
        }
        try {
            if (newFile != null) {
                ComponentSubtabsManager.refreshMainTabPresentation(project, newFile);
                SidetabsManager.attachIfNeeded(project, newFile);
            }
            if (selectionUpdated) {
                ComponentSubtabsManager.refreshOpenStates(project);
            }
            if (ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(project)) {
                ComponentSubtabEditorSplitPresentation.handleMainTabSelection(project, newFile, oldFile);
                if (newFile != null) {
                    ComponentSubtabEditorSplitNavigation.onFileSelected(project, newFile);
                }
                ComponentSubtabEditorSplitPresentation.reapplyAll(project);
            } else if (newFile != null && oldFile != null) {
                ComponentSubtabEditorSplitNavigation.tryActivateSavedPairFromOrdinaryTwoPaneMainTabSelection(
                        project,
                        newFile,
                        oldFile
                );
            }
            ComponentSubtabMainTabSelectPopup.installOn(project);
            ComponentSubtabGroupSplitNavigation.reapplyAll(project);
            refreshOpenPopups(project);
            if (newFile != null) {
                ComponentSubtabMainTabColors.refreshForFile(project, newFile);
            }
            ComponentSubtabMainTabIcons.scheduleRefreshAfterPlatformUpdate(project);
        } finally {
            DEFERRED_SELECTION_CHROME_SCHEDULED.set(project, null);
        }
    }

    @Override
    public void beforeFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        Project project = source.getProject();
        if (ComponentSubtabNavigation.isSwitchInProgress(project)) {
            return;
        }
        if (ComponentSubtabEditorSplitNavigation.switchToOtherPairForOpenedFile(project, file)) {
            return;
        }
        SplittabDedicatedViewService.getInstance(project).onForeignFileOpened(file);

        ComponentSubtabEditorSplitRegistry.SplittabPair activeSplittab =
                ComponentSubtabEditorSplitRegistry.getInstance(project).activePair();
        if (ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(project)
                && activeSplittab != null
                && ComponentSubtabEditorSplitNavigation.blocksExternalFileOpen(project, activeSplittab, file)) {
            ComponentSubtabProjectViewNavigation.openExternalFileDuringSplittabSession(
                    project,
                    activeSplittab,
                    file,
                    true
            );
            return;
        }

        if (!source.isFileOpen(file)
                && ComponentSubtabProjectViewNavigation.navigateRelatedFileFromProjectView(
                project,
                file,
                true
        )) {
            ApplicationManager.getApplication().invokeLater(
                    () -> ComponentSubtabNavigation.consolidateAfterSubtabGroupNavigation(
                            project,
                            file
                    ),
                    ModalityState.nonModal(),
                    project.getDisposed()
            );
            return;
        }
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        Project project = source.getProject();
        boolean switchInProgress = ComponentSubtabNavigation.isSwitchInProgress(project);
        boolean relocatedByPairSwitch = !switchInProgress
                && ComponentSubtabEditorSplitNavigation.consumeOtherPairRelocatedFile(project, file);
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);
        if (!switchInProgress
                && !relocatedByPairSwitch
                && ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(project)) {
            ComponentSubtabEditorSplitRegistry.SplittabPair activeSplittab = registry.activePair();
            if (activeSplittab != null
                    && ComponentSubtabEditorSplitNavigation.blocksExternalFileOpen(project, activeSplittab, file)) {
                ComponentSubtabEditorSplitMainTab.notePendingExternalFileOpen(project, activeSplittab, file);
            }
        }
        ComponentSubtabsDocumentListener.install(project);
        if (switchInProgress) {
            ComponentSubtabsManager.updateSelectionForFile(project, file);
            return;
        }
        boolean coalescedPlatformOpen = consumeCoalescedSubtabPlatformOpen(project, file);
        if (coalescedPlatformOpen) {
            // Subtab swaps open without awaiting the composite, so the editors may only exist now.
            ComponentSubtabsManager.attachIfNeeded(project, file);
            ComponentSubtabsManager.updateSelectionForFile(project, file);
            ComponentSubtabBarHover.refreshAllActiveMainTabSync(project);
            scheduleDeferredSidetabsAttach(project, file);
            if (!relocatedByPairSwitch) {
                finishPendingExternalOpenIfNeeded(source, registry, file);
            }
            ComponentSubtabNavigation.consolidateAfterSubtabGroupNavigation(project, file);
            return;
        }
        ComponentSubtabsManager.attachIfNeeded(project, file);
        SidetabsManager.attachIfNeeded(project, file);
        ComponentSubtabsManager.syncSelectionForFile(project, file);
        ComponentSubtabGroupSplitNavigation.reapplyAll(project);
        if (ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(project)) {
            ComponentSubtabEditorSplitNavigation.onFileOpened(project, file);
            ComponentSubtabEditorSplitPresentation.reapplyAll(project);
        }
        refreshOpenPopups(project);
        scheduleDeferredFileOpenedChromeRefresh(project, file);
        if (!relocatedByPairSwitch) {
            finishPendingExternalOpenIfNeeded(source, registry, file);
        }
    }

    private static void finishPendingExternalOpenIfNeeded(
            @NotNull FileEditorManager source,
            @NotNull ComponentSubtabEditorSplitRegistry registry,
            @NotNull VirtualFile openedFile
    ) {
        Project project = source.getProject();
        if (ComponentSubtabNavigation.isSwitchInProgress(project)) {
            return;
        }
        boolean deferSplittabTeardown = registry.pendingExternalOpenPairId() != null;
        Runnable finishExternalOpen = () -> {
            if (project.isDisposed()) {
                return;
            }
            ComponentSubtabEditorSplitMainTab.completePendingExternalFileOpen(project, openedFile);
        };
        if (deferSplittabTeardown && !source.isFileOpen(openedFile)) {
            ApplicationManager.getApplication().invokeLater(
                    finishExternalOpen,
                    ModalityState.nonModal(),
                    project.getDisposed()
            );
        } else {
            finishExternalOpen.run();
        }
    }

    @Override
    public void beforeFileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        Project project = source.getProject();
        CLOSING_EDITOR_FILE.set(project, file.getPath());
        if (ComponentSubtabNavigation.isSwitchInProgress(project)) {
            return;
        }
        ComponentSubtabsManager.detachFromFile(project, file);
        SidetabsManager.detachFromFile(project, file);
    }

    static boolean isClosingEditorFile(@NotNull Project project, @NotNull VirtualFile file) {
        String closingPath = CLOSING_EDITOR_FILE.get(project);
        return closingPath != null && closingPath.equals(file.getPath());
    }

    static boolean isAnyEditorFileClosing(@NotNull Project project) {
        return CLOSING_EDITOR_FILE.get(project) != null;
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        Project project = source.getProject();
        if (file.getPath().equals(CLOSING_EDITOR_FILE.get(project))) {
            CLOSING_EDITOR_FILE.set(project, null);
        }
        if (ComponentSubtabNavigation.isSwitchInProgress(project)) {
            return;
        }
        SplittabDedicatedViewService dedicated = SplittabDedicatedViewService.getInstance(project);
        ComponentSubtabEditorSplitRegistry.SplittabPair activePair =
                ComponentSubtabEditorSplitRegistry.getInstance(project).activePair();
        boolean dedicatedPartnerSwap = dedicated.isDedicatedViewActive()
                && activePair != null
                && !activePair.covers(file);

        ComponentSubtabEditorSplitMainTab.onSplittabPairFileClosed(project, file);
        ComponentSubtabGroupSplitNavigation.clearSplitPresentation(project, file);
        ComponentSubtabEditorSplitPresentation.clearSplittabPresentation(project, file);
        if (dedicatedPartnerSwap) {
            if (activePair != null) {
                ComponentSubtabEditorSplitPresentation.refreshDedicatedPairSwitch(project, activePair);
            }
            return;
        }
        // Closing a file detaches the subtab bar from every editor of that file, so a surviving
        // split has to rebuild its presentation afterwards.
        ComponentSubtabGroupSplitNavigation.reapplyAll(source.getProject());
        ComponentSubtabEditorSplitPresentation.reapplyAll(source.getProject());
        ComponentSubtabEditorSplitNavigation.disengageIfPairFileOutsideSplittabWorkspace(project, file);
        ComponentSubtabMainTabSelectPopup.hideAllPopups(source.getProject());
        ComponentSubtabsManager.refreshOpenStates(source.getProject());
        refreshOpenPopups(source.getProject());
        ComponentSubtabMainTabSelectPopup.installOn(source.getProject());
        ComponentSubtabMainTabColors.refresh(source.getProject());
        ComponentSubtabsManager.refreshAllMainTabPresentations(source.getProject());
        ComponentSubtabEditorSplitMainTab.refreshNormalUiForOpenPairSides(project, file);
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        Project project = event.getManager().getProject();
        if (ComponentSubtabNavigation.isSwitchInProgress(project)) {
            VirtualFile newFile = event.getNewFile();
            VirtualFile oldFile = event.getOldFile();
            if (newFile != null) {
                ComponentSubtabsManager.updateSelectionForFile(project, newFile);
            }
            if (oldFile != null && !oldFile.equals(newFile)) {
                ComponentSubtabsManager.updateSelectionForFile(project, oldFile);
            }
            return;
        }
        VirtualFile newFile = event.getNewFile();
        VirtualFile oldFile = event.getOldFile();
        boolean selectionUpdated = false;
        if (newFile != null) {
            selectionUpdated |= ComponentSubtabsManager.updateSelectionForFile(project, newFile);
        }
        if (oldFile != null && !oldFile.equals(newFile)) {
            selectionUpdated |= ComponentSubtabsManager.updateSelectionForFile(project, oldFile);
        }
        scheduleDeferredSelectionChrome(project, newFile, oldFile, selectionUpdated);
    }

    private static void refreshOpenPopups(@NotNull Project project) {
        ComponentSubtabMainTabSelectPopup.refreshPopupPresentation(project);
        var toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (toolWindow != null) {
            for (JTree tree : UIUtil.findComponentsOfType(toolWindow.getComponent(), JTree.class)) {
                SubtabGroupLocationHover.refreshPopupPresentation(project, tree);
            }
        }
    }

    private static void scheduleDeferredFileOpenedChromeRefresh(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        ApplicationManager.getApplication().invokeLater(
                () -> {
                    if (project.isDisposed()) {
                        return;
                    }
                    ComponentSubtabMainTabSelectPopup.installOn(project);
                    ComponentSubtabMainTabColors.refreshForFile(project, file);
                    ComponentSubtabMainTabIcons.scheduleRefreshAfterPlatformUpdate(project);
                },
                ModalityState.nonModal(),
                project.getDisposed()
        );
    }

    static void attachToAlreadyOpenFiles(@NotNull Project project) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        ComponentSubtabsDocumentListener.install(project);
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (VirtualFile file : manager.getOpenFiles()) {
            SidetabsManager.attachIfNeeded(project, file);
            ComponentSubtabsManager.attachIfNeeded(project, file);
            ComponentSubtabsManager.updateSelectionForFile(project, file);
        }
        ComponentSubtabsManager.refreshOpenStates(project);
        ComponentSubtabsManager.applyPresentationState(project);
        ComponentSubtabsManager.scheduleStartupIconRelayout(project);
        ComponentSubtabsManager.refreshAllMainTabPresentations(project);
        ComponentSubtabMainTabColors.refresh(project);
        ComponentSubtabMainTabIcons.scheduleStartupRefresh(project);
    }
}
