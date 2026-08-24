package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ComponentFileNamingTest {
    @Test
    void findsTheSameBaseForEveryComponentPart() {
        List<String> fileNames = List.of(
                "user-card.component.ts",
                "user-card.component.spec.ts",
                "user-card.component.test.ts",
                "user-card.component.html",
                "user-card.component.scss",
                "user-card.component.css"
        );

        for (String fileName : fileNames) {
            assertEquals(
                    "user-card.component",
                    ComponentFileNaming.componentBaseName(fileName),
                    fileName
            );
        }
    }

    @Test
    void ignoresUnrelatedFiles() {
        assertNull(ComponentFileNaming.componentBaseName("README.md"));
        assertNull(ComponentFileNaming.componentBaseName(".ts"));
    }

    @Test
    void createsCandidatesInVisibleTabOrder() {
        List<ComponentFileNaming.Candidate> candidates =
                ComponentFileNaming.candidates("app.component");

        assertEquals(ComponentFileNaming.Kind.SOURCE, candidates.get(0).kind());
        assertEquals("app.component.ts", candidates.get(0).fileName());
        assertEquals(ComponentFileNaming.Kind.TEST, candidates.get(1).kind());
        assertEquals(ComponentFileNaming.Kind.TEMPLATE, candidates.get(3).kind());
        assertEquals(ComponentFileNaming.Kind.STYLE, candidates.get(4).kind());
    }

    @Test
    void usesShortComponentNameForTabTitle() {
        assertEquals("user-card", ComponentFileNaming.displayName("user-card.component"));
        assertEquals("app", ComponentFileNaming.displayName("app.component"));
        assertEquals("header", ComponentFileNaming.displayName("header"));
    }
}
