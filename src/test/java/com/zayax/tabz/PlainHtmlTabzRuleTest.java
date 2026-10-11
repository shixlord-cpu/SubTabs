package com.zayax.tabz;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;

import java.util.List;

public class PlainHtmlTabzRuleTest extends HeavyPlatformTestCase {
    private static final int HTML_RULE_INDEX =
            SubtabRulesDefaults.indexOfRule(SubtabRulesDefaults.createDefaults(), "HTML");
    private static final int COMPONENT_RULE_INDEX =
            SubtabRulesDefaults.indexOfRule(SubtabRulesDefaults.createDefaults(), "Komponente");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TabzSettings.getInstance().resetToDefaults();
    }

    public void testPlainHtmlGroupsWithCompanionAssets() throws Exception {
        TabzSettings.getInstance().setSubtabsActive(true);

        VirtualFile dir = getVirtualFile(createTempDir("pages"));
        WriteAction.runAndWait(() -> {
            dir.createChildData(this, "catalog-page.html");
            dir.createChildData(this, "catalog-page.css");
        });
        VirtualFile html = WriteAction.computeAndWait(() -> dir.findChild("catalog-page.html"));
        assertNotNull(html);

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(html);
        assertNotNull(match);
        assertEquals("rule:" + HTML_RULE_INDEX + ":catalog-page", match.baseName());
        assertEquals("catalog-page", ComponentFileNaming.displayName(match.baseName()));
        assertEquals(2, match.relatedFiles().size());
    }

    public void testComponentHtmlStaysOnComponentRule() throws Exception {
        TabzSettings.getInstance().setSubtabsActive(true);

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
        assertEquals("rule:" + COMPONENT_RULE_INDEX + ":header#header.component", match.baseName());
        assertEquals("header-component", ComponentFileNaming.displayName(match.baseName()));
        assertEquals(3, match.relatedFiles().size());
    }

    public void testComponentHtmlUsesComponentRuleEvenWithoutPartners() throws Exception {
        assertEquals(
                "rule:" + COMPONENT_RULE_INDEX + ":header#header.component",
                ComponentFileNaming.componentBaseName("header.component.html")
        );
        assertNull(CustomSubtabRuleMatcher.match("header.component.html", List.of(SubtabRulesDefaults.htmlRule())));
    }
}
