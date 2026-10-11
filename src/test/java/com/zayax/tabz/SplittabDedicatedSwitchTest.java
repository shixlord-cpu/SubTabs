package com.zayax.tabz;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.testFramework.PlatformTestUtil;

public class SplittabDedicatedSwitchTest extends RealEditorWindowTestCase {
    private static final long PAIR_SWITCH_BUDGET_MS = 1_000L;

    private com.intellij.openapi.vfs.VirtualFile htmlFile;
    private com.intellij.openapi.vfs.VirtualFile specFile;
    private com.intellij.openapi.vfs.VirtualFile scssFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings.getInstance().setTabzEnabled(true);
        TabzSettings.getInstance().setSplittabsEnabled(true);
        TabzSettings.getInstance().setSplittabBehaviorMode(SplittabBehaviorMode.DEDICATED_VIEW);
        TabzSettings.getInstance().setSplittabDissolveMode(SplittabDissolveMode.DISSOLVE);
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
        scssFile = createSourceFile("product-list.component.scss");
    }

    public void testCreateSplitAutoEntersDedicatedWithTwoOpenFiles() throws Exception {
        openAndSettle(htmlFile);
        openAndSettle(scssFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainEvents();

        SplittabDedicatedViewService dedicated = SplittabDedicatedViewService.getInstance(getProject());
        assertTrue(dedicated.isDedicatedViewActive());
        assertEquals(2, manager.getOpenFiles().length);
        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));
        assertFalse(manager.isFileOpen(scssFile));
    }

    public void testDissolveInDedicatedViewExitsNormalMode() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainEvents();
        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        assertNotNull(pair);

        TabzSettings.getInstance().setSplittabDissolveMode(SplittabDissolveMode.DISSOLVE);
        ComponentSubtabEditorSplitPresentation.dissolvePair(getProject(), pair.id());
        drainEvents();

        assertFalse(SplittabDedicatedViewService.getInstance(getProject()).isDedicatedViewActive());
        assertTrue(manager.isFileOpen(htmlFile));
        assertTrue(manager.isFileOpen(specFile));
    }

    public void testSharedLeftFilePairSwitchUnderOneSecond() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainEvents();
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, scssFile);
        drainEvents();
        ComponentSubtabEditorSplitRegistry.SplittabPair pairSpec =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        ComponentSubtabEditorSplitRegistry.SplittabPair pairScss =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, scssFile);
        assertNotNull(pairSpec);
        assertNotNull(pairScss);

        long elapsedMs = activatePairAndDrain(pairScss.id());
        assertTrue(manager.isFileOpen(scssFile));
        assertSplittabChrome(htmlFile, scssFile);
        assertTrue(
                "scss pair activation took " + elapsedMs + "ms",
                elapsedMs < PAIR_SWITCH_BUDGET_MS
        );

        elapsedMs = activatePairAndDrain(pairSpec.id());
        assertTrue(manager.isFileOpen(specFile));
        assertFalse(manager.isFileOpen(scssFile));
        assertSplittabChrome(htmlFile, specFile);
        assertTrue(
                "spec pair activation took " + elapsedMs + "ms",
                elapsedMs < PAIR_SWITCH_BUDGET_MS
        );

        elapsedMs = activatePairAndDrain(pairScss.id());
        assertTrue(manager.isFileOpen(scssFile));
        assertSplittabChrome(htmlFile, scssFile);
        assertTrue(
                "scss pair re-activation took " + elapsedMs + "ms",
                elapsedMs < PAIR_SWITCH_BUDGET_MS
        );
    }

    public void testSharedLeftFileSwitchKeepsSplittabChrome() throws Exception {
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainEvents();
        ComponentSubtabEditorSplitRegistry.SplittabPair pairSpec =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        assertNotNull(pairSpec);

        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, scssFile);
        drainEvents();
        ComponentSubtabEditorSplitRegistry.SplittabPair pairScss =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, scssFile);
        assertNotNull(pairScss);

        assertTrue(
                SplittabDedicatedViewService.getInstance(getProject()).isDedicatedViewActive()
        );
        assertNotNull(ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair());
        assertEquals(pairScss.id(), ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair().id());
        ComponentSubtabEditorSplitNavigation.activatePair(getProject(), pairSpec.id());
        drainEvents();
        assertEquals(pairSpec.id(), ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair().id());
        assertTrue("spec must be open after switch", manager.isFileOpen(specFile));
        assertFalse("previous partner must close", manager.isFileOpen(scssFile));
        assertSplittabChrome(htmlFile, specFile);

        ComponentSubtabEditorSplitNavigation.activatePair(getProject(), pairScss.id());
        drainEvents();
        assertTrue(manager.isFileOpen(scssFile));
        assertFalse(manager.isFileOpen(specFile));
        assertSplittabChrome(htmlFile, scssFile);

        ComponentSubtabEditorSplitNavigation.activatePair(getProject(), pairSpec.id());
        drainEvents();
        assertTrue(manager.isFileOpen(specFile));
        assertFalse(manager.isFileOpen(scssFile));
        assertSplittabChrome(htmlFile, specFile);
    }

    private void assertSplittabChrome(
            com.intellij.openapi.vfs.VirtualFile leftFile,
            com.intellij.openapi.vfs.VirtualFile rightFile
    ) {
        FileEditor[] leftEditors = ComponentSubtabsManager.editorsFor(manager, leftFile);
        FileEditor[] rightEditors = ComponentSubtabsManager.editorsFor(manager, rightFile);
        assertTrue(leftEditors.length > 0);
        assertTrue(rightEditors.length > 0);
        assertNotNull(leftEditors[0].getUserData(ComponentSubtabSplittabUi.SPLITTAB_SWITCH_BAR_KEY));
        assertNotNull(rightEditors[0].getUserData(ComponentSubtabSplittabUi.SPLITTAB_HEADER_KEY));
    }

    public void testClosePairDissolveClosesBothTabsInMixedMode() throws Exception {
        TabzSettings.getInstance().setSplittabBehaviorMode(SplittabBehaviorMode.INTEGRATED);
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainEvents();
        ComponentSubtabEditorSplitRegistry.SplittabPair pair =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        assertNotNull(pair);

        TabzSettings.getInstance().setSplittabDissolveMode(SplittabDissolveMode.CLOSE_PAIR);
        ComponentSubtabEditorSplitPresentation.dissolvePair(getProject(), pair.id());
        drainEvents();

        assertFalse(manager.isFileOpen(htmlFile));
        assertFalse(manager.isFileOpen(specFile));
        assertNull(ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile));
    }

    private long activatePairAndDrain(String pairId) {
        long start = System.nanoTime();
        ComponentSubtabEditorSplitNavigation.activatePair(getProject(), pairId);
        drainDeferredEditorEvents();
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private void drainEvents() {
        for (int attempt = 0; attempt < 120; attempt++) {
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
