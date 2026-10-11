package com.zayax.tabz;

import com.intellij.codeInsight.navigation.ItemWithPresentation;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SmartPointerManager;
import com.intellij.testFramework.PlatformTestUtil;

import javax.swing.JList;
import javax.swing.ListModel;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;

/**
 * Exercises Tabz navigation-popup hover without relying on TypeScript Go to Declaration.
 */
public class ComponentSubtabNavigationTargetPopupHoverIntegrationTest extends RealEditorWindowTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings.getInstance().setTabzEnabled(true);
        TabzSettings.getInstance().setHoverViewEnabled(true);
        TabzSettings.getInstance().setSubtabsActive(true);
        ComponentSubtabProjectViewEditorHover.installOn(getProject());
    }

    public void testHoverSyncOnNavigationTargetListHighlightsOpenMainTabFile() throws Exception {
        VirtualFile html = createSourceFile("nav-hover-demo.component.html");
        VirtualFile ts = createSourceFile("nav-hover-demo.component.ts");
        WriteAction.runAndWait(() -> {
            var htmlDoc = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(html);
            var tsDoc = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(ts);
            if (htmlDoc != null) {
                htmlDoc.setText("<p>nav hover demo</p>");
            }
            if (tsDoc != null) {
                tsDoc.setText("export const x = 1;\n");
            }
        });
        openAndSettle(html);
        openAndSettle(ts);

        PsiFile tsPsi = PsiManager.getInstance(getProject()).findFile(ts);
        assertNotNull(tsPsi);

        ItemWithPresentation item = new ItemWithPresentation(
                SmartPointerManager.createPointer(tsPsi),
                TargetPresentation.builder(ts.getName()).presentation()
        );
        JList<Object> list = new JList<>(new Object[] { item });
        list.setSize(400, 80);

        ComponentSubtabNavigationTargetPopupHover.ensureInstalled(list);
        assertTrue(ComponentSubtabNavigationTargetFiles.isNavigationTargetList(list));

        ComponentSubtabNavigationTargetPopupHover.handleMouse(list, new Point(10, 10));
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertEquals(ts, FileEditorManager.getInstance(getProject()).getSelectedFiles()[0]);

        ComponentSubtabNavigationTargetPopupHover.handleMouse(list, new Point(-1, -1));
    }

    public void testPopupListDetectionRequiresPopupWindowType() {
        JList<Object> list = new JList<>(new Object[] { "not-a-target" });
        assertFalse(ComponentSubtabNavigationTargetPopupHover.isPopupNavigationList(list));
    }
}
