package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;

import java.util.List;

public class PlainHtmlSubtabRuleTest extends HeavyPlatformTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings.getInstance().resetToDefaults();
    }

    public void testPlainHtmlGroupsWithCompanionAssets() throws Exception {
        SubtabsSettings.getInstance().setSubtabsActive(true);

        VirtualFile dir = getVirtualFile(createTempDir("pages"));
        WriteAction.runAndWait(() -> {
            dir.createChildData(this, "catalog-page.html");
            dir.createChildData(this, "catalog-page.css");
        });
        VirtualFile html = WriteAction.computeAndWait(() -> dir.findChild("catalog-page.html"));
        assertNotNull(html);

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(html);
        assertNotNull(match);
        assertEquals("rule:6:catalog-page", match.baseName());
        assertEquals("catalog-page", ComponentFileNaming.displayName(match.baseName()));
        assertEquals(2, match.relatedFiles().size());
    }

    public void testComponentHtmlStaysOnComponentRule() throws Exception {
        SubtabsSettings.getInstance().setSubtabsActive(true);

        VirtualFile dir = getVirtualFile(createTempDir("app"));
        WriteAction.runAndWait(() -> {
            dir.createChildData(this, "header.component.ts");
            dir.createChildData(this, "header.component.html");
            dir.createChildData(this, "header.component.scss");
        });
        VirtualFile html = WriteAction.computeAndWait(() -> dir.findChild("header.component.html"));
        assertNotNull(html);

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(html);
        assertNotNull(match);
        assertEquals("rule:7:header#header.component", match.baseName());
        assertEquals("header-component", ComponentFileNaming.displayName(match.baseName()));
        assertEquals(3, match.relatedFiles().size());
    }

    public void testComponentHtmlUsesComponentRuleEvenWithoutPartners() throws Exception {
        assertEquals(
                "rule:7:header#header.component",
                ComponentFileNaming.componentBaseName("header.component.html")
        );
        assertNull(CustomSubtabRuleMatcher.match("header.component.html", List.of(SubtabRulesDefaults.htmlRule())));
    }
}
