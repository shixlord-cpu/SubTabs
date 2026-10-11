package com.zayax.tabz;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manual scenario: a user-card splittab (html + scss) is showing, then a file of another component is
 * opened. It must be open and focused within a second and must keep the focus afterwards.
 */
public class SplittabForeignFileOpenTimingTest extends RealEditorWindowTestCase {
    private static final long OPEN_BUDGET_MS = 1_000L;
    private static final long FOCUS_HOLD_MS = 2_000L;

    private VirtualFile htmlFile;
    private VirtualFile scssFile;
    private VirtualFile headerHtml;
    private VirtualFile headerTs;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings settings = TabzSettings.getInstance();
        settings.setTabzEnabled(true);
        settings.setSubtabsActive(true);
        settings.setSplittabsEnabled(true);
        settings.setSplittabDissolveMode(SplittabDissolveMode.DISSOLVE);
        settings.setSplittabOtherPairFileMode(SplittabOtherPairFileMode.OPEN_NORMALLY);
        htmlFile = copyDemoComponent("user-card.component.html");
        scssFile = copyDemoComponent("user-card.component.scss");
        copyDemoComponent("user-card.component.ts");
        copyDemoComponent("user-card.component.spec.ts");
        headerHtml = copyDemoComponent("header.component.html");
        headerTs = copyDemoComponent("header.component.ts");
        copyDemoComponent("header.component.scss");
    }

    public void testOpenForeignFileFromMixedSplittabIsFastAndFocused() throws Exception {
        TabzSettings.getInstance().setSplittabBehaviorMode(SplittabBehaviorMode.INTEGRATED);
        openForeignFileFromSplittab("mixed");
    }

    public void testOpenForeignFileFromSwitchSplittabIsFastAndFocused() throws Exception {
        TabzSettings.getInstance().setSplittabBehaviorMode(SplittabBehaviorMode.DEDICATED_VIEW);
        openForeignFileFromSplittab("switch");
    }

    private void openForeignFileFromSplittab(String label) {
        openAndSettle(headerHtml);
        openAndSettle(htmlFile);
        drainDeferredEditorEvents();
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, scssFile);
        drainDeferredEditorEvents();
        waitUntil("splittab chrome", () -> hasChrome(htmlFile, scssFile));
        drainDeferredEditorEvents();

        long start = System.nanoTime();
        manager.openFile(headerTs, true);
        long callMs = elapsedMs(start);
        waitUntil("foreign file focused", () -> isFocused(headerTs));
        long focusedMs = elapsedMs(start);

        long holdUntil = System.currentTimeMillis() + FOCUS_HOLD_MS;
        while (System.currentTimeMillis() < holdUntil) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            assertTrue("[" + label + "] focus was taken away from the opened file: " + state(), isFocused(headerTs));
            sleep(10);
        }
        System.out.println("[" + label + " foreign open] openFile call " + callMs + "ms; focused after "
                + focusedMs + "ms; " + state());

        assertTrue("[" + label + "] opening a file took " + focusedMs + "ms", focusedMs < OPEN_BUDGET_MS);
        assertEquals("[" + label + "] opened file must exist in one pane", 1, paneCountOf(headerTs));
        assertFalse(ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(getProject()));
    }

    private boolean isFocused(VirtualFile file) {
        EditorWindow current = manager.getCurrentWindow();
        return manager.isFileOpen(file)
                && current != null
                && file.equals(current.getSelectedFile())
                && manager.getEditors(file).length > 0;
    }

    private String state() {
        EditorWindow current = manager.getCurrentWindow();
        return "[windows=" + manager.getWindows().length
                + " open=" + java.util.Arrays.toString(manager.getOpenFiles())
                + " selected=" + (current == null ? null : current.getSelectedFile())
                + "]";
    }

    private boolean hasChrome(VirtualFile leftFile, VirtualFile rightFile) {
        var leftEditors = ComponentSubtabsManager.editorsFor(manager, leftFile);
        var rightEditors = ComponentSubtabsManager.editorsFor(manager, rightFile);
        return leftEditors.length > 0
                && rightEditors.length > 0
                && (leftEditors[0].getUserData(ComponentSubtabSplittabUi.SPLITTAB_SWITCH_BAR_KEY) != null
                && rightEditors[0].getUserData(ComponentSubtabSplittabUi.SPLITTAB_HEADER_KEY) != null
                || leftEditors[0].getUserData(ComponentSubtabSplittabUi.SPLITTAB_HEADER_KEY) != null
                && rightEditors[0].getUserData(ComponentSubtabSplittabUi.SPLITTAB_SWITCH_BAR_KEY) != null);
    }

    private void waitUntil(String what, java.util.function.BooleanSupplier condition) {
        PlatformTestUtil.waitWithEventsDispatching(() -> what + " not reached " + state(), condition::getAsBoolean, 20);
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private VirtualFile copyDemoComponent(String fileName) throws Exception {
        String text = Files.readString(Path.of("demo-project/src/app").resolve(fileName), StandardCharsets.UTF_8);
        VirtualFile file = createSourceFile(fileName);
        WriteAction.run(() -> file.setBinaryContent(text.getBytes(StandardCharsets.UTF_8)));
        return file;
    }
}
