package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;

import java.util.List;

public class ComponentSubtabOrderTest extends HeavyPlatformTestCase {
    private VirtualFile htmlFile;
    private VirtualFile scssFile;
    private VirtualFile tsFile;
    private String groupKey;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        VirtualFile dir = getVirtualFile(createTempDir("order"));
        WriteAction.runAndWait(() -> {
            tsFile = dir.createChildData(this, "product-list.component.ts");
            scssFile = dir.createChildData(this, "product-list.component.scss");
            htmlFile = dir.createChildData(this, "product-list.component.html");
        });

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(htmlFile);
        assertNotNull(match);
        groupKey = match.key();
    }

    public void testStoredOrderOverridesDefaultFileNameSort() {
        SubtabsSettings.getInstance().setSubtabGroupOrder(
                groupKey,
                List.of(
                        htmlFile.getPath().replace('\\', '/'),
                        scssFile.getPath().replace('\\', '/'),
                        tsFile.getPath().replace('\\', '/')
                )
        );
        ComponentRelatedFilesCache.getInstance(getProject()).clear();

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(htmlFile);
        assertNotNull(match);
        assertEquals(htmlFile, match.relatedFiles().get(0).file());
        assertEquals(scssFile, match.relatedFiles().get(1).file());
        assertEquals(tsFile, match.relatedFiles().get(2).file());
    }

    public void testMoveRightPersistsOrder() {
        ComponentSubtabOrder.moveRight(getProject(), htmlFile);

        List<String> stored = SubtabsSettings.getInstance().getSubtabGroupOrder(groupKey);
        assertEquals(3, stored.size());
        assertEquals(scssFile.getPath().replace('\\', '/'), stored.get(0));
        assertEquals(htmlFile.getPath().replace('\\', '/'), stored.get(1));
        assertEquals(tsFile.getPath().replace('\\', '/'), stored.get(2));

        ComponentRelatedFilesCache.getInstance(getProject()).clear();
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(htmlFile);
        assertNotNull(match);
        assertEquals(scssFile, match.relatedFiles().get(0).file());
        assertEquals(htmlFile, match.relatedFiles().get(1).file());
    }
}
