package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.tabs.TabInfo;

public class ComponentSubtabMainTabTitleTest extends RealEditorWindowTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setSubtabsActive(true);
        settings.setShowSubtabNameInMainTab(false);
    }

    public void testSingleFileInGroupUsesComponentNameInMainTab() throws Exception {
        VirtualFile html = createSourceFile("header.component.html");

        openAndSettle(html);
        PlatformTestUtil.waitWithEventsDispatching(
                "main tab title should use grouped component name",
                () -> {
                    TabInfo tabInfo = tabInfoOf(html);
                    return tabInfo != null && "header".equals(tabInfo.getText());
                },
                30
        );

        TabInfo tabInfo = tabInfoOf(html);
        assertNotNull(tabInfo);
        assertEquals("header", tabInfo.getText());
    }

    public void testLastOpenFileKeepsGroupedTitleAfterSiblingsClose() throws Exception {
        VirtualFile html = createSourceFile("header.component.html");
        VirtualFile ts = createSourceFile("header.component.ts");
        VirtualFile scss = createSourceFile("header.component.scss");

        openAndSettle(html);
        openAndSettle(ts);
        openAndSettle(scss);

        manager.closeFile(html);
        manager.closeFile(ts);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        PlatformTestUtil.waitWithEventsDispatching(
                "surviving tab should keep grouped component name",
                () -> {
                    TabInfo tabInfo = tabInfoOf(scss);
                    return tabInfo != null && "header".equals(tabInfo.getText());
                },
                30
        );

        TabInfo tabInfo = tabInfoOf(scss);
        assertNotNull(tabInfo);
        assertEquals("header", tabInfo.getText());
    }

    public void testOnlyOpenedMemberUsesComponentNameInMainTab() throws Exception {
        createSourceFile("header.component.ts");
        createSourceFile("header.component.scss");
        VirtualFile html = createSourceFile("header.component.html");

        openAndSettle(html);

        TabInfo tabInfo = tabInfoOf(html);
        assertNotNull(tabInfo);
        assertEquals("header", tabInfo.getText());
    }
}
