package de.sasbe.subtabs;

import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.LightVirtualFile;

public class ComponentTabTitlesSuffixTest extends LightPlatformTestCase {
    public void testAppendsSubtabLabelWhenSettingEnabled() {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        boolean previous = settings.isShowSubtabNameInMainTab();
        settings.setShowSubtabNameInMainTab(true);
        try {
            LightVirtualFile file = new LightVirtualFile("header.component.html");
            assertEquals("header (HTML)", ComponentTabTitles.mainTabTitle(false, file));
        } finally {
            settings.setShowSubtabNameInMainTab(previous);
        }
    }
}
