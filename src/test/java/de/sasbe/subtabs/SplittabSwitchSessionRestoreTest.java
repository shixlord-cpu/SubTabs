package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.testFramework.PlatformTestUtil;

public class SplittabSwitchSessionRestoreTest extends RealEditorWindowTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setFamiliaEnabled(true);
        settings.setSplittabsEnabled(true);
        settings.setSplittabBehaviorMode(SplittabBehaviorMode.DEDICATED_VIEW);
        settings.setRestoreSwitchSplittabSessionOnProjectOpen(false);
    }

    public void testSwitchSessionFlagOffRestoresNormalLayoutFromSavedState() throws Exception {
        var htmlFile = createSourceFile("restore.component.html");
        var specFile = createSourceFile("restore.component.spec.ts");
        var extraFile = createSourceFile("restore.component.ts");
        openAndSettle(htmlFile);
        openAndSettle(extraFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainEvents();

        SplittabDedicatedViewService dedicated = SplittabDedicatedViewService.getInstance(getProject());
        dedicated.enterDedicatedView();
        assertTrue(dedicated.isDedicatedViewActive());
        assertFalse(manager.isFileOpen(extraFile));

        ComponentSubtabEditorSplitRegistry.SerializedState exported =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).getState();
        assertTrue(exported.switchDedicatedSessionAtSave);
        assertNotNull(exported.normalLayoutBeforeSwitch);

        ComponentSubtabEditorSplitRegistry.getInstance(getProject()).loadState(exported);
        ComponentSubtabEditorSplitNavigation.restorePersistedActiveSplittab(getProject());
        drainEvents();

        assertNull(ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair());
        assertFalse(dedicated.isDedicatedViewActive());
        assertTrue(manager.isFileOpen(extraFile));
    }

    public void testSwitchSessionFlagOnRestoresDedicatedView() throws Exception {
        SubtabsSettings.getInstance().setRestoreSwitchSplittabSessionOnProjectOpen(true);

        var htmlFile = createSourceFile("switch-restore.component.html");
        var specFile = createSourceFile("switch-restore.component.spec.ts");
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainEvents();

        SplittabDedicatedViewService dedicated = SplittabDedicatedViewService.getInstance(getProject());
        assertTrue(dedicated.isDedicatedViewActive());

        ComponentSubtabEditorSplitRegistry.SerializedState exported =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).getState();
        assertTrue(exported.switchDedicatedSessionAtSave);

        dedicated.exitDedicatedView(false);
        drainEvents();
        assertFalse(dedicated.isDedicatedViewActive());

        ComponentSubtabEditorSplitRegistry.getInstance(getProject()).loadState(exported);
        ComponentSubtabEditorSplitNavigation.restorePersistedActiveSplittab(getProject());
        drainEvents();

        assertTrue(dedicated.isDedicatedViewActive());
        assertNotNull(ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair());
        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));
    }

    private void drainEvents() {
        for (int attempt = 0; attempt < 240; attempt++) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            try {
                Thread.sleep(5);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
