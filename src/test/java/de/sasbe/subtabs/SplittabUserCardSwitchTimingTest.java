package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manual scenario: two splittabs inside user-card (html + scss, html + component.ts) in switch behavior,
 * then switching between both links via the left switch bar.
 */
public class SplittabUserCardSwitchTimingTest extends RealEditorWindowTestCase {
    private static final long PAIR_SWITCH_BUDGET_MS = 1_000L;

    private VirtualFile htmlFile;
    private VirtualFile scssFile;
    private VirtualFile tsFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings.getInstance().setFamiliaEnabled(true);
        SubtabsSettings.getInstance().setSubtabsActive(true);
        SubtabsSettings.getInstance().setSplittabsEnabled(true);
        SubtabsSettings.getInstance().setSplittabBehaviorMode(SplittabBehaviorMode.DEDICATED_VIEW);
        SubtabsSettings.getInstance().setSplittabDissolveMode(SplittabDissolveMode.DISSOLVE);
        htmlFile = copyDemoComponent("user-card.component.html");
        scssFile = copyDemoComponent("user-card.component.scss");
        tsFile = copyDemoComponent("user-card.component.ts");
        copyDemoComponent("user-card.component.spec.ts");
    }

    public void testSwitchBetweenUserCardSplittabsUnderOneSecond() throws Exception {
        openAndSettle(htmlFile);
        drainDeferredEditorEvents();
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, scssFile);
        drainDeferredEditorEvents();
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), htmlFile, tsFile);
        drainDeferredEditorEvents();

        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(getProject());
        ComponentSubtabEditorSplitRegistry.SplittabPair pairScss = registry.findByFiles(htmlFile, scssFile);
        ComponentSubtabEditorSplitRegistry.SplittabPair pairTs = registry.findByFiles(htmlFile, tsFile);
        assertNotNull(pairScss);
        assertNotNull(pairTs);
        assertTrue(SplittabDedicatedViewService.getInstance(getProject()).isDedicatedViewActive());

        long worstMs = 0;
        StringBuilder timings = new StringBuilder();
        for (int round = 0; round < 4; round++) {
            boolean toScss = round % 2 == 0;
            ComponentSubtabEditorSplitRegistry.SplittabPair target = toScss ? pairScss : pairTs;
            VirtualFile expectedRight = toScss ? scssFile : tsFile;
            VirtualFile expectedClosed = toScss ? tsFile : scssFile;

            long elapsedMs = clickSwitchBarAndWaitForChrome(target.id(), expectedRight);
            timings.append(toScss ? "->scss " : "->ts ").append(elapsedMs).append("ms; ");
            worstMs = Math.max(worstMs, elapsedMs);

            assertEquals(target.id(), registry.activePair().id());
            assertTrue(manager.isFileOpen(htmlFile));
            assertTrue(manager.isFileOpen(expectedRight));
            assertFalse(manager.isFileOpen(expectedClosed));
            assertEquals(2, manager.getOpenFiles().length);
            assertSplittabChrome(htmlFile, expectedRight);
        }
        System.out.println("[user-card splittab switch] " + timings);
        assertTrue(
                "user-card splittab switch too slow: " + timings,
                worstMs < PAIR_SWITCH_BUDGET_MS
        );
    }

    /**
     * The sandbox IDE builds ts/scss editors with a slow TextMate highlighter. A blocking open in the
     * switch-bar click starved that build and froze the UI for ~6s (see freeze dumps).
     */
    public void testSwitchWithSlowEditorBuildDoesNotBlockTheClick() throws Exception {
        long editorBuildMs = 2_500L;
        com.intellij.openapi.fileEditor.FileEditorProvider.EP_FILE_EDITOR_PROVIDER.getPoint().registerExtension(
                new SlowLoadingFileEditorProvider(editorBuildMs),
                getTestRootDisposable()
        );
        VirtualFile slowHtml = createSourceFile(SlowLoadingFileEditorProvider.FILE_PREFIX + "html");
        VirtualFile slowScss = createSourceFile(SlowLoadingFileEditorProvider.FILE_PREFIX + "scss");
        VirtualFile slowTs = createSourceFile(SlowLoadingFileEditorProvider.FILE_PREFIX + "ts");
        htmlFile = slowHtml;

        openAndSettle(slowHtml);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), slowHtml, slowScss);
        waitForChrome(slowHtml, slowScss);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), slowHtml, slowTs);
        waitForChrome(slowHtml, slowTs);

        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(getProject());
        ComponentSubtabEditorSplitRegistry.SplittabPair pairScss = registry.findByFiles(slowHtml, slowScss);
        ComponentSubtabEditorSplitRegistry.SplittabPair pairTs = registry.findByFiles(slowHtml, slowTs);
        assertNotNull(pairScss);
        assertNotNull(pairTs);

        StringBuilder timings = new StringBuilder();
        for (int round = 0; round < 2; round++) {
            boolean toScss = round % 2 == 0;
            VirtualFile right = toScss ? slowScss : slowTs;
            VirtualFile closed = toScss ? slowTs : slowScss;
            SplittabSwitchBarPanel bar = switchBar();
            assertNotNull(bar);

            long start = System.nanoTime();
            bar.clickPairForTests((toScss ? pairScss : pairTs).id());
            long clickMs = (System.nanoTime() - start) / 1_000_000L;
            timings.append(toScss ? "->scss click " : "->ts click ").append(clickMs).append("ms; ");

            assertTrue("switch-bar click blocked the EDT: " + timings, clickMs < PAIR_SWITCH_BUDGET_MS);
            assertTrue(manager.isFileOpen(right));
            assertSame(windowOf(right), dedicatedRightWindow(slowHtml));
            assertTrue(manager.isFileOpen(slowHtml));

            waitForChrome(slowHtml, right);
            assertFalse(manager.isFileOpen(closed));
            assertEquals(2, manager.getOpenFiles().length);
        }
        System.out.println("[user-card slow-editor splittab switch] " + timings);
    }

    public void testBackgroundOpenOptionsNeitherAwaitNorSelect() {
        var options = ComponentSubtabNavigation.nonBlockingOpenOptions(false, false);
        assertFalse(options.waitForCompositeOpen);
        assertFalse(options.selectAsCurrent);
        assertFalse(options.requestFocus);
        assertTrue(ComponentSubtabNavigation.nonBlockingOpenOptions(true, true).selectAsCurrent);
    }

    public void testCreateSplitWithSlowEditorBuildDoesNotBlock() throws Exception {
        registerSlowEditorProvider();
        VirtualFile slowHtml = createSourceFile(SlowLoadingFileEditorProvider.FILE_PREFIX + "html");
        VirtualFile slowScss = createSourceFile(SlowLoadingFileEditorProvider.FILE_PREFIX + "scss");
        VirtualFile slowTs = createSourceFile(SlowLoadingFileEditorProvider.FILE_PREFIX + "ts");
        openAndSettle(slowHtml);

        long firstMs = timed(() -> ComponentSubtabEditorSplitNavigation.createSplit(getProject(), slowHtml, slowScss));
        assertTrue("first createSplit blocked the EDT for " + firstMs + "ms", firstMs < PAIR_SWITCH_BUDGET_MS);
        waitForChrome(slowHtml, slowScss);
        assertEquals(2, manager.getOpenFiles().length);

        long secondMs = timed(() -> ComponentSubtabEditorSplitNavigation.createSplit(getProject(), slowHtml, slowTs));
        assertTrue("second createSplit blocked the EDT for " + secondMs + "ms", secondMs < PAIR_SWITCH_BUDGET_MS);
        waitForChrome(slowHtml, slowTs);
        drainDeferredEditorEvents();
        assertTrue(manager.isFileOpen(slowHtml));
        assertTrue(manager.isFileOpen(slowTs));
        assertFalse(manager.isFileOpen(slowScss));
        assertEquals(2, manager.getOpenFiles().length);
        System.out.println("[user-card slow-editor createSplit] first " + firstMs + "ms; second " + secondMs + "ms");
    }

    public void testSwitchToPairWithDifferentLeftFileDoesNotBlock() throws Exception {
        registerSlowEditorProvider();
        VirtualFile slowHtml = createSourceFile(SlowLoadingFileEditorProvider.FILE_PREFIX + "html");
        VirtualFile slowScss = createSourceFile(SlowLoadingFileEditorProvider.FILE_PREFIX + "scss");
        VirtualFile slowTs = createSourceFile(SlowLoadingFileEditorProvider.FILE_PREFIX + "ts");
        VirtualFile slowSpec = createSourceFile(SlowLoadingFileEditorProvider.FILE_PREFIX + "spec.ts");
        openAndSettle(slowHtml);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), slowHtml, slowScss);
        waitForChrome(slowHtml, slowScss);
        ComponentSubtabEditorSplitNavigation.createSplit(getProject(), slowTs, slowSpec);
        ComponentSubtabEditorSplitRegistry registry = ComponentSubtabEditorSplitRegistry.getInstance(getProject());
        ComponentSubtabEditorSplitRegistry.SplittabPair htmlPair = registry.findPairUnordered(slowHtml, slowScss);
        ComponentSubtabEditorSplitRegistry.SplittabPair tsPair = registry.findPairUnordered(slowTs, slowSpec);
        assertNotNull(htmlPair);
        assertNotNull(tsPair);
        waitForChrome(tsPair.leftFile(), tsPair.rightFile());

        StringBuilder timings = new StringBuilder();
        ComponentSubtabEditorSplitRegistry.SplittabPair current = tsPair;
        for (int round = 0; round < 3; round++) {
            ComponentSubtabEditorSplitRegistry.SplittabPair target = current == tsPair ? htmlPair : tsPair;
            SplittabSwitchBarPanel bar = switchBarOn(current.leftFile());
            assertNotNull("switch bar on " + current.leftFile().getName(), bar);

            long clickMs = timed(() -> bar.clickPairForTests(target.id()));
            timings.append("->").append(target.rightFile().getName()).append(' ').append(clickMs).append("ms; ");
            assertTrue("switch to other left file blocked the EDT: " + timings, clickMs < PAIR_SWITCH_BUDGET_MS);

            waitForChrome(target.leftFile(), target.rightFile());
            drainDeferredEditorEvents();
            assertEquals(target.id(), registry.activePair().id());
            assertFalse(manager.isFileOpen(current.leftFile()));
            assertFalse(manager.isFileOpen(current.rightFile()));
            assertEquals(2, manager.getOpenFiles().length);
            current = target;
        }
        System.out.println("[user-card slow-editor other-left switch] " + timings);
    }

    private void registerSlowEditorProvider() {
        com.intellij.openapi.fileEditor.FileEditorProvider.EP_FILE_EDITOR_PROVIDER.getPoint().registerExtension(
                new SlowLoadingFileEditorProvider(2_500L),
                getTestRootDisposable()
        );
    }

    private static long timed(Runnable action) {
        long start = System.nanoTime();
        action.run();
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private SplittabSwitchBarPanel switchBarOn(VirtualFile leftFile) {
        for (var editor : ComponentSubtabsManager.editorsFor(manager, leftFile)) {
            SplittabSwitchBarPanel bar = editor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY);
            if (bar != null) {
                return bar;
            }
        }
        return null;
    }

    private com.intellij.openapi.fileEditor.impl.EditorWindow dedicatedRightWindow(VirtualFile leftFile) {
        var left = windowOf(leftFile);
        for (var window : manager.getWindows()) {
            if (window != left) {
                return window;
            }
        }
        return null;
    }

    private void waitForChrome(VirtualFile leftFile, VirtualFile rightFile) {
        PlatformTestUtil.waitWithEventsDispatching(
                () -> "splittab chrome was not attached to " + leftFile.getName() + " / " + rightFile.getName()
                        + " " + chromeState(leftFile, rightFile),
                () -> hasChrome(leftFile, rightFile),
                20
        );
    }

    private String chromeState(VirtualFile leftFile, VirtualFile rightFile) {
        var leftEditors = ComponentSubtabsManager.editorsFor(manager, leftFile);
        var rightEditors = ComponentSubtabsManager.editorsFor(manager, rightFile);
        var active = ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair();
        return "[windows=" + manager.getWindows().length
                + " open=" + java.util.Arrays.toString(manager.getOpenFiles())
                + " leftEditors=" + leftEditors.length
                + " rightEditors=" + rightEditors.length
                + " leftBar=" + (leftEditors.length > 0
                && leftEditors[0].getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY) != null)
                + " rightHeader=" + (rightEditors.length > 0
                && rightEditors[0].getUserData(ComponentSubtabsSplittabUi.SPLITTAB_HEADER_KEY) != null)
                + " sameWindow=" + (windowOf(leftFile) == windowOf(rightFile))
                + " active=" + (active == null ? null : active.rightFile().getName())
                + " dedicated=" + SplittabDedicatedViewService.getInstance(getProject()).isDedicatedViewActive()
                + "]";
    }

    private boolean hasChrome(VirtualFile leftFile, VirtualFile rightFile) {
        var leftEditors = ComponentSubtabsManager.editorsFor(manager, leftFile);
        var rightEditors = ComponentSubtabsManager.editorsFor(manager, rightFile);
        return leftEditors.length > 0
                && rightEditors.length > 0
                && leftEditors[0].getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY) != null
                && rightEditors[0].getUserData(ComponentSubtabsSplittabUi.SPLITTAB_HEADER_KEY) != null
                && windowOf(leftFile) != windowOf(rightFile);
    }

    /** Measures from the click until the new partner is shown with its splittab chrome. */
    private long clickSwitchBarAndWaitForChrome(String pairId, VirtualFile expectedRight) {
        SplittabSwitchBarPanel bar = switchBar();
        assertNotNull("switch bar must be mounted on the left editor", bar);
        long start = System.nanoTime();
        bar.clickPairForTests(pairId);
        waitForChrome(htmlFile, expectedRight);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        drainDeferredEditorEvents();
        return elapsedMs;
    }

    private SplittabSwitchBarPanel switchBar() {
        for (var editor : ComponentSubtabsManager.editorsFor(manager, htmlFile)) {
            SplittabSwitchBarPanel bar = editor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY);
            if (bar != null) {
                return bar;
            }
        }
        return null;
    }

    private void assertSplittabChrome(VirtualFile leftFile, VirtualFile rightFile) {
        var leftEditors = ComponentSubtabsManager.editorsFor(manager, leftFile);
        var rightEditors = ComponentSubtabsManager.editorsFor(manager, rightFile);
        assertTrue(leftEditors.length > 0);
        assertTrue(rightEditors.length > 0);
        assertNotNull(leftEditors[0].getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY));
        assertNotNull(rightEditors[0].getUserData(ComponentSubtabsSplittabUi.SPLITTAB_HEADER_KEY));
        assertNotSame(windowOf(leftFile), windowOf(rightFile));
    }

    private VirtualFile copyDemoComponent(String fileName) throws Exception {
        String text = Files.readString(Path.of("demo-project/src/app").resolve(fileName), StandardCharsets.UTF_8);
        VirtualFile file = createSourceFile(fileName);
        WriteAction.run(() -> file.setBinaryContent(text.getBytes(StandardCharsets.UTF_8)));
        return file;
    }
}
