package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;

/**
 * Measures the speedup from caching {@link ComponentRelatedFiles#find(VirtualFile)}.
 */
public class ComponentRelatedFilesCachePerformanceTest extends HeavyPlatformTestCase {
    private static final int ITERATIONS = 500;
    private static final int MIN_SPEEDUP = 5;

    private VirtualFile htmlFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        VirtualFile dir = getVirtualFile(createTempDir("component"));
        WriteAction.runAndWait(() -> {
            dir.createChildData(this, "header.component.ts");
            dir.createChildData(this, "header.component.scss");
        });
        htmlFile = WriteAction.computeAndWait(() -> dir.createChildData(this, "header.component.html"));
        ComponentRelatedFiles.find(htmlFile);
    }

    public void testCachedFindIsMuchFasterThanUncached() {
        long cachedNs = measure(() -> ComponentRelatedFiles.find(htmlFile));
        ComponentRelatedFilesCache.getInstance(getProject()).clear();
        long uncachedNs = measure(() -> ComponentRelatedFiles.findUncached(htmlFile));

        assertTrue(
                "cached find should be at least " + MIN_SPEEDUP + "x faster (cached="
                        + cachedNs / 1_000_000 + "ms, uncached=" + uncachedNs / 1_000_000 + "ms)",
                cachedNs * MIN_SPEEDUP < uncachedNs
        );
    }

    public void testCacheInvalidatesAfterSiblingChange() throws Exception {
        ComponentRelatedFiles.Match before = ComponentRelatedFiles.find(htmlFile);
        assertNotNull(before);
        assertEquals(3, before.relatedFiles().size());

        WriteAction.runAndWait(() -> htmlFile.getParent().createChildData(this, "header.component.spec.ts"));

        ComponentRelatedFiles.Match after = ComponentRelatedFiles.find(htmlFile);
        assertNotNull(after);
        assertEquals(4, after.relatedFiles().size());
    }

    private long measure(TimedAction action) {
        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            action.run();
        }
        return System.nanoTime() - start;
    }

    @FunctionalInterface
    private interface TimedAction {
        void run();
    }
}
