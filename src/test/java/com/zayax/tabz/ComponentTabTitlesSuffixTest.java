package com.zayax.tabz;

import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.LightVirtualFile;

public class ComponentTabTitlesSuffixTest extends LightPlatformTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings.getInstance().resetToDefaults();
    }

    public void testAppendsTabzLabelWhenSettingEnabled() {
        TabzSettings settings = TabzSettings.getInstance();
        boolean previous = settings.isShowSubtabNameInMainTab();
        settings.setShowSubtabNameInMainTab(true);
        try {
            LightVirtualFile file = new LightVirtualFile("header.component.html");
            assertEquals("header-component (html)", ComponentTabTitles.mainTabTitle(false, file));
        } finally {
            settings.setShowSubtabNameInMainTab(previous);
        }
    }
}
