package com.zayax.tabz;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.testFramework.PlatformTestUtil;

public class SplittabDedicatedViewTest extends RealEditorWindowTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings.getInstance().setTabzEnabled(true);
        TabzSettings.getInstance().setSplittabsEnabled(true);
        TabzSettings.getInstance().setSplittabBehaviorMode(SplittabBehaviorMode.DEDICATED_VIEW);
    }

    public void testDedicatedCloseOneSideRemovesPairFromRegistry() throws Exception {
        var htmlFile = createSourceFile("header.component.html");
        var specFile = createSourceFile("header.component.spec.ts");
        openAndSettle(htmlFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainEvents();

        SplittabDedicatedViewService dedicated = SplittabDedicatedViewService.getInstance(getProject());
        dedicated.enterDedicatedView();
        assertTrue(dedicated.isDedicatedViewActive());

        manager.closeFile(specFile);
        drainEvents();

        assertNull(ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile));
        assertFalse(dedicated.isDedicatedViewActive());
    }

    public void testDedicatedToggleRestoresNormalLayout() throws Exception {
        var htmlFile = createSourceFile("product-list.component.html");
        var specFile = createSourceFile("product-list.component.spec.ts");
        var extraFile = createSourceFile("product-list.component.ts");
        openAndSettle(htmlFile);
        openAndSettle(extraFile);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, specFile);
        drainEvents();

        SplittabDedicatedViewService dedicated = SplittabDedicatedViewService.getInstance(getProject());
        dedicated.enterDedicatedView();
        assertTrue(manager.isFileOpen(htmlFile));
        assertFalse(manager.isFileOpen(extraFile));

        dedicated.exitDedicatedView(true);
        drainEvents();

        assertFalse(dedicated.isDedicatedViewActive());
        assertTrue(manager.isFileOpen(extraFile));
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
