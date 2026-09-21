package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;

public class ComponentExcludePatternsTest extends HeavyPlatformTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings.getInstance().resetToDefaults();
        SubtabsSettings.getInstance().setSubtabsActive(true);
    }

    public void testComponentGroupIgnoresStateFilesInSameFolder() throws Exception {
        VirtualFile dir = getVirtualFile(createTempDir("feature"));
        WriteAction.runAndWait(() -> {
            dir.createChildData(this, "header.component.ts");
            dir.createChildData(this, "header.component.html");
            dir.createChildData(this, "header.actions.ts");
            dir.createChildData(this, "header.reducer.ts");
        });
        VirtualFile component = WriteAction.computeAndWait(() -> dir.findChild("header.component.ts"));
        assertNotNull(component);

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(component);
        assertNotNull(match);
        assertEquals("rule:7:header#header.component", match.baseName());
        assertEquals(2, match.relatedFiles().size());
        for (ComponentRelatedFiles.Entry entry : match.relatedFiles()) {
            assertFalse(entry.file().getName().endsWith(".actions.ts"));
            assertFalse(entry.file().getName().endsWith(".reducer.ts"));
        }
    }

    public void testStateFileDoesNotMatchComponentWhenComponentIsFirstRule() {
        CustomSubtabRule component = SubtabRulesDefaults.createDefaults().stream()
                .filter(rule -> "Komponente".equals(rule.name))
                .findFirst()
                .orElseThrow()
                .copy();
        CustomSubtabRule state = SubtabRulesDefaults.createDefaults().stream()
                .filter(rule -> "State Central".equals(rule.name))
                .findFirst()
                .orElseThrow()
                .copy();
        state.enabled = false;

        SubtabsSettings.getInstance().setRules(java.util.List.of(component, state));
        ComponentFileNaming.invalidateRulesCache();

        assertNull(ComponentFileNaming.componentBaseName("cart.actions.ts"));
        assertEquals(
                "rule:0:header#header.component",
                ComponentFileNaming.componentBaseName("header.component.ts")
        );
    }
}
