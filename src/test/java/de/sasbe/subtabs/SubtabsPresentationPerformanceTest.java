package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;

/**
 * Verifies that grouping toggles and subtab attachment avoid redundant full-project work.
 */
public class SubtabsPresentationPerformanceTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile tsFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings.getInstance().setSubtabsActive(true);
        htmlFile = createSourceFile("product-list.component.html");
        tsFile = createSourceFile("product-list.component.ts");
        createSourceFile("product-list.component.scss");
    }

    public void testGroupingToggleKeepsLoadedSubtabGroups() {
        openAndSettle(htmlFile);

        ComponentSubtabGroupRegistry registry = ComponentSubtabGroupRegistry.getInstance(getProject());
        ComponentSubtabGroup groupBefore = registry.getOrCreateGroup(htmlFile);
        assertNotNull(groupBefore);

        SubtabsProjectViewGroupingState.getInstance(getProject()).toggle(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        SubtabsProjectViewGroupingState.getInstance(getProject()).toggle(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        ComponentSubtabGroup groupAfter = registry.getOrCreateGroup(htmlFile);
        assertSame("group registry should survive grouping toggles", groupBefore, groupAfter);
    }

    public void testSecondGroupFileReusesExistingGroup() {
        openAndSettle(htmlFile);
        ComponentSubtabGroupRegistry registry = ComponentSubtabGroupRegistry.getInstance(getProject());
        ComponentSubtabGroup firstGroup = registry.getOrCreateGroup(htmlFile);
        assertNotNull(firstGroup);

        openAndSettle(tsFile);

        ComponentSubtabGroup secondGroup = registry.getOrCreateGroup(tsFile);
        assertSame("opening another file in the same group must reuse the registry entry", firstGroup, secondGroup);
    }

    public void testRelatedFilesLookupUsesCacheAcrossRepeatedOpens() {
        ComponentRelatedFilesCache cache = ComponentRelatedFilesCache.getInstance(getProject());
        cache.clear();

        openAndSettle(htmlFile);
        ComponentRelatedFiles.Match firstLookup = ComponentRelatedFiles.find(htmlFile);
        assertNotNull(firstLookup);

        long cachedNs = measureLookups(htmlFile, 200);
        cache.clear();
        long uncachedNs = measureUncachedLookups(htmlFile, 200);

        assertTrue(
                "repeated opens should benefit from cached related-file lookup (cached="
                        + cachedNs / 1_000_000 + "ms, uncached=" + uncachedNs / 1_000_000 + "ms)",
                cachedNs * 5 < uncachedNs
        );
    }

    private long measureLookups(VirtualFile file, int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            ComponentRelatedFiles.find(file);
        }
        return System.nanoTime() - start;
    }

    private long measureUncachedLookups(VirtualFile file, int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            ComponentRelatedFiles.findUncached(file);
        }
        return System.nanoTime() - start;
    }
}
