package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.Dispatchers;

/**
 * Base for tests that need real editor windows and tab strips. The light test fixture ships a stub
 * file editor manager without windows, so a real {@link FileEditorManagerImpl} takes its place.
 */
public abstract class RealEditorWindowTestCase extends HeavyPlatformTestCase {
    protected FileEditorManagerEx manager;
    protected VirtualFile sourceDir;
    private CoroutineScope managerScope;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        managerScope = CoroutineScopeKt.CoroutineScope(Dispatchers.getDefault());
        FileEditorManagerImpl real = new FileEditorManagerImpl(getProject(), managerScope);
        ServiceContainerUtil.replaceService(
                getProject(), FileEditorManager.class, real, getTestRootDisposable());
        manager = FileEditorManagerEx.getInstanceEx(getProject());
        sourceDir = getVirtualFile(createTempDir("app"));
        ComponentSubtabsFileEditorListener.attachToAlreadyOpenFiles(getProject());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            ComponentSubtabGroupSplitRegistry.getInstance(getProject()).clear();
            ComponentSubtabEditorSplitRegistry.getInstance(getProject()).clear();
            ComponentSubtabsScopedVisibility.getInstance(getProject()).clear();
            if (manager != null) {
                // Editor panes are built asynchronously, so the manager has to be shut down explicitly
                // or the framework reports the editors it still holds as leaked.
                manager.closeAllFiles();
                ComponentSubtabEditorSplitNavigation.collapseEmptyEditorWindows(getProject());
                for (int attempt = 0; attempt < 30; attempt++) {
                    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
                }
                CoroutineScopeKt.cancel(managerScope, null);
                PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            }
        } finally {
            super.tearDown();
        }
    }

    protected VirtualFile createSourceFile(String name) throws Exception {
        return WriteAction.computeAndWait(() -> sourceDir.createChildData(this, name));
    }

    protected void openAndSettle(VirtualFile file) {
        manager.openFile(file, true);
        drainDeferredEditorEvents();
    }

    protected void drainDeferredEditorEvents() {
        for (int attempt = 0; attempt < 40; attempt++) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        }
        // Plugin opens do not await the editor composite; its editors are built in the background.
        long deadline = System.currentTimeMillis() + 10_000L;
        while (!allOpenFilesHaveEditors() && System.currentTimeMillis() < deadline) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            try {
                Thread.sleep(5);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        for (int attempt = 0; attempt < 40; attempt++) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        }
    }

    private boolean allOpenFilesHaveEditors() {
        for (VirtualFile file : manager.getOpenFiles()) {
            if (manager.getEditors(file).length == 0) {
                return false;
            }
        }
        return true;
    }

    protected EditorWindow windowOf(VirtualFile file) {
        for (EditorWindow window : manager.getWindows()) {
            if (window.isFileOpen(file)) {
                return window;
            }
        }
        return null;
    }

    protected int paneCountOf(VirtualFile file) {
        int count = 0;
        for (EditorWindow window : manager.getWindows()) {
            if (window.isFileOpen(file)) {
                count++;
            }
        }
        return count;
    }

    protected TabInfo tabInfoOf(VirtualFile file) {
        for (EditorWindow window : manager.getWindows()) {
            JBTabs tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }
            for (TabInfo tabInfo : tabsImpl.getTabs()) {
                if (file.equals(tabInfo.getObject())) {
                    return tabInfo;
                }
            }
        }
        return null;
    }

    protected VirtualFile selectedFile() {
        VirtualFile[] selected = manager.getSelectedFiles();
        return selected.length == 0 ? null : selected[0];
    }
}
