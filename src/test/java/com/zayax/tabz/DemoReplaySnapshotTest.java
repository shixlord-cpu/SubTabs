package com.zayax.tabz;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.testFramework.PlatformTestUtil;

public class DemoReplaySnapshotTest extends RealEditorWindowTestCase {
    public void testSnapshotCapturesOpenTabsAndTabzChrome() throws Exception {
        TabzSettings.getInstance().setTabzEnabled(true);
        var htmlFile = createSourceFile("header.component.html");
        var tsFile = createSourceFile("header.component.ts");

        openAndSettle(htmlFile);
        FileEditorManager.getInstance(getProject()).openFile(tsFile, true);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        drainDeferredEditorEvents();

        String json = DemoReplaySnapshotBuilder.capture(getProject()).build();
        assertTrue(json.contains("\"selectedFile\""));
        assertTrue("expected html path in snapshot: " + json, json.contains("header.component.html"));
        assertTrue("expected editorWindows in snapshot: " + json, json.contains("\"editorWindows\""));
    }

    public void testRecorderWritesLatestJsonOnCapture() throws Exception {
        System.setProperty("tabz.demo.replay", "record");
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("tabz-demo-replay");
        System.setProperty("tabz.demo.replay.dir", tempDir.toString());
        DemoReplayConfig.allowNonDemoProjectForTests = true;
        try {
            TabzSettings.getInstance().setTabzEnabled(true);
            var htmlFile = createSourceFile("product-list.component.html");
            openAndSettle(htmlFile);

            DemoReplayRecorder recorder = DemoReplayRecorder.getInstance(getProject());
            assertNotNull(recorder);
            recorder.captureNow("test");
            recorder.dispose();

            java.nio.file.Path latest = tempDir.resolve("latest.json");
            assertTrue(java.nio.file.Files.exists(latest));
            String contents = java.nio.file.Files.readString(latest);
            assertTrue(contents.contains("product-list.component.html"));
            assertTrue(contents.contains("\"eventCount\""));
            assertTrue(java.nio.file.Files.exists(tempDir.resolve("last-session.txt")));
        } finally {
            DemoReplayConfig.allowNonDemoProjectForTests = false;
            System.clearProperty("tabz.demo.replay");
            System.clearProperty("tabz.demo.replay.dir");
            java.nio.file.Files.walk(tempDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            java.nio.file.Files.deleteIfExists(path);
                        } catch (java.io.IOException ignored) {
                        }
                    });
        }
    }
}
