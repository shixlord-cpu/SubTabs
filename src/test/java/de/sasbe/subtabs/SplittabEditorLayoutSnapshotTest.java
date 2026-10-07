package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import javax.swing.SwingConstants;

public class SplittabEditorLayoutSnapshotTest extends RealEditorWindowTestCase {
    public void testRestorePreservesVerticalAndHorizontalSplitLayout() throws Exception {
        SubtabsSettings.getInstance().setFamiliaEnabled(true);
        VirtualFile a = createSourceFile("a.component.html");
        VirtualFile b = createSourceFile("b.component.ts");
        VirtualFile c = createSourceFile("c.component.scss");

        openAndSettle(a);
        var rootPane = manager.getCurrentWindow();
        assertNotNull(rootPane);
        rootPane.split(SwingConstants.VERTICAL, true, a, true);
        var bottomPane = rootPane.split(SwingConstants.HORIZONTAL, true, a, true);
        assertNotNull(bottomPane);
        openAndSettle(b);
        openAndSettle(c);
        manager.setCurrentWindow(bottomPane);
        bottomPane.setSelectedComposite(c, true);
        drainEvents();

        int paneCountBefore = manager.getWindows().length;
        assertTrue("expected at least three editor panes: " + paneCountBefore, paneCountBefore >= 3);

        SplittabEditorLayoutSnapshot.State snapshot = SplittabEditorLayoutSnapshot.capture(getProject());
        assertNotNull("platform editor layout must be captured", snapshot.layoutElement);
        manager.closeAllFiles();
        drainEvents();
        assertEquals(0, manager.getOpenFiles().length);

        SplittabEditorLayoutSnapshot.restore(getProject(), snapshot);
        drainEvents();

        assertEquals(paneCountBefore, manager.getWindows().length);
        assertTrue(manager.isFileOpen(a));
        assertTrue(manager.isFileOpen(b));
        assertTrue(manager.isFileOpen(c));
        assertEquals(c, selectedFile());
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
