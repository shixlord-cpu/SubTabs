package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;

public class ComponentSubtabEditorSplitNamingTest extends RealEditorWindowTestCase {
    private VirtualFile htmlFile;
    private VirtualFile specFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings.getInstance().setFamiliaEnabled(true);
        htmlFile = createSourceFile("product-list.component.html");
        specFile = createSourceFile("product-list.component.spec.ts");
    }

    public void testCustomLinkAndHeaderNamesPersistInProjectState() {
        var pair = ComponentSubtabEditorSplitRegistry.getInstance(getProject()).register(htmlFile, specFile);

        ComponentSubtabEditorSplitRegistry.getInstance(getProject()).setLinkName(pair.id(), "HTML+Spec");
        ComponentSubtabEditorSplitRegistry.getInstance(getProject()).setHeaderLabel(pair.id(), "Produktliste (html & spec)");

        var updated = ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findById(pair.id());
        assertNotNull(updated);
        assertEquals("HTML+Spec", ComponentSubtabEditorSplitPresentation.linkBarText(updated, 0));
        assertEquals(
                "Produktliste (html & spec)",
                ComponentSubtabEditorSplitPresentation.paneHeaderText(updated)
        );

        ComponentSubtabEditorSplitRegistry.SerializedState exported =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).getState();
        assertNotNull(exported);
        assertEquals(1, exported.pairs.size());
        assertEquals("HTML+Spec", exported.pairs.getFirst().linkName);
        assertEquals("Produktliste (html & spec)", exported.pairs.getFirst().headerLabel);

        ComponentSubtabEditorSplitRegistry.getInstance(getProject()).clear();
        assertTrue(ComponentSubtabEditorSplitRegistry.getInstance(getProject()).all().isEmpty());

        ComponentSubtabEditorSplitRegistry.getInstance(getProject()).loadState(exported);
        var restored = ComponentSubtabEditorSplitRegistry.getInstance(getProject()).findByFiles(htmlFile, specFile);
        assertNotNull(restored);
        assertEquals("HTML+Spec", restored.linkName());
        assertEquals("Produktliste (html & spec)", restored.headerLabel());
    }

    public void testActiveSplittabIdSurvivesProjectStateRoundTrip() {
        var pair = ComponentSubtabEditorSplitRegistry.getInstance(getProject()).register(htmlFile, specFile);
        ComponentSubtabEditorSplitRegistry.getInstance(getProject()).setActive(pair.id());

        ComponentSubtabEditorSplitRegistry.SerializedState exported =
                ComponentSubtabEditorSplitRegistry.getInstance(getProject()).getState();
        assertEquals(pair.id(), exported.activePairId);

        ComponentSubtabEditorSplitRegistry.getInstance(getProject()).clear();
        ComponentSubtabEditorSplitRegistry.getInstance(getProject()).loadState(exported);
        assertEquals(pair.id(), ComponentSubtabEditorSplitRegistry.getInstance(getProject()).activePair().id());
    }
}
