package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.ui.tabs.impl.TabLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.JToggleButton;
import java.awt.event.MouseEvent;

/**
 * TESTE: hovering a main tab must highlight its current subtab file like the hover select box.
 */
public class ComponentSubtabMainTabHoverSyncTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile scssFile;
    private VirtualFile tsFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setSubtabsActive(true);
        settings.setHoverViewEnabled(true);
        settings.setFamiliaEnabled(true);
        settings.setGroupColorsEnabled(false);
        settings.setRules(SubtabRulesDefaults.createDefaults());
        ComponentSubtabsDocumentListener.install(getProject());

        htmlFile = createSourceFile("product-list.component.html");
        scssFile = createSourceFile("product-list.component.scss");
        tsFile = createSourceFile("product-list.component.ts");
    }

    public void testMainTabHoverHighlightsCurrentSubtabLikeSelectBox() {
        openAndSettle(htmlFile);
        ComponentSubtabsManager.attachIfNeeded(getProject(), htmlFile);
        ComponentSubtabMainTabSelectPopup.installOn(getProject());

        ComponentSubtabBarPanel panel = barFor(htmlFile);
        assertNotNull(panel);
        JToggleButton htmlButton = panel.buttonFor(htmlFile);
        assertNotNull(htmlButton);

        TabLabel tabLabel = tabLabelFor(htmlFile);
        assertNotNull(tabLabel);
        ComponentSubtabBarHover.onEnterMainTab(getProject(), htmlFile, tabLabel);
        hoverMainTab(tabLabel);

        assertTrue(ComponentSubtabUi.isMainTabSyncHighlight(htmlButton));
        assertEquals(ComponentSubtabUi.popupHighlightedBackground(), htmlButton.getBackground());

        ComponentSubtabBarHover.onExitMainTab(tabLabel);
        assertFalse(ComponentSubtabUi.isMainTabSyncHighlight(htmlButton));
    }

    public void testMainTabHoverSyncFollowsFileAfterSelectBoxSwitch() {
        openAndSettle(htmlFile);
        ComponentSubtabsManager.attachIfNeeded(getProject(), htmlFile);
        ComponentSubtabMainTabSelectPopup.installOn(getProject());

        ComponentSubtabBarPanel panel = barFor(htmlFile);
        assertNotNull(panel);
        JToggleButton htmlButton = panel.buttonFor(htmlFile);
        JToggleButton scssButton = panel.buttonFor(scssFile);
        assertNotNull(htmlButton);
        assertNotNull(scssButton);

        TabLabel tabLabel = tabLabelFor(htmlFile);
        assertNotNull(tabLabel);
        hoverMainTab(tabLabel);
        assertTrue(ComponentSubtabUi.isMainTabSyncHighlight(htmlButton));

        ComponentSubtabMainTabSelectPopup.openFromPopup(getProject(), htmlFile, scssFile);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        panel = barFor(scssFile);
        assertNotNull(panel);
        htmlButton = panel.buttonFor(htmlFile);
        scssButton = panel.buttonFor(scssFile);
        assertNotNull(htmlButton);
        assertNotNull(scssButton);

        assertFalse("the previous subtab must not keep hover sync", ComponentSubtabUi.isMainTabSyncHighlight(htmlButton));
        assertTrue("the new subtab must receive hover sync while the main tab is still hovered",
                ComponentSubtabUi.isMainTabSyncHighlight(scssButton));
        assertTrue(scssButton.isSelected());
        assertFalse(htmlButton.isSelected());
    }

    public void testInstalledMainTabHoverListenerTriggersSubtabSync() {
        openAndSettle(htmlFile);
        ComponentSubtabsManager.attachIfNeeded(getProject(), htmlFile);
        ComponentSubtabMainTabSelectPopup.installOn(getProject());

        JToggleButton htmlButton = barFor(htmlFile).buttonFor(htmlFile);
        assertNotNull(htmlButton);
        TabLabel tabLabel = tabLabelFor(htmlFile);
        assertNotNull(tabLabel);

        hoverMainTab(tabLabel);

        assertTrue(ComponentSubtabUi.isMainTabSyncHighlight(htmlButton));
        assertEquals(ComponentSubtabUi.popupHighlightedBackground(), htmlButton.getBackground());
    }

    private static void hoverMainTab(@NotNull TabLabel tabLabel) {
        tabLabel.dispatchEvent(new MouseEvent(
                tabLabel,
                MouseEvent.MOUSE_ENTERED,
                System.currentTimeMillis(),
                0,
                Math.max(1, tabLabel.getWidth() / 2),
                Math.max(1, tabLabel.getHeight() / 2),
                0,
                false
        ));
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    }

    private ComponentSubtabBarPanel barFor(@NotNull VirtualFile file) {
        for (FileEditor editor : FileEditorManager.getInstance(getProject()).getAllEditors()) {
            if (!file.equals(editor.getFile())) {
                continue;
            }
            ComponentSubtabBarPanel panel = editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY);
            if (panel != null) {
                return panel;
            }
        }
        return null;
    }

    private TabLabel tabLabelFor(@NotNull VirtualFile file) {
        var tabInfo = tabInfoOf(file);
        if (tabInfo == null) {
            return null;
        }
        for (var window : manager.getWindows()) {
            var tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }
            TabLabel label = tabsImpl.getTabLabel(tabInfo);
            if (label != null) {
                return label;
            }
        }
        return null;
    }
}
