package de.sasbe.subtabs;

import com.intellij.testFramework.LightVirtualFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ComponentSubtabNavigationTest {
    @Test
    void selectsAdjacentSubtabToTheRight() {
        LightVirtualFile ts = new LightVirtualFile("app.component.ts");
        LightVirtualFile html = new LightVirtualFile("app.component.html");
        LightVirtualFile scss = new LightVirtualFile("app.component.scss");
        List<ComponentRelatedFiles.Entry> related = List.of(
                new ComponentRelatedFiles.Entry("TS", ts),
                new ComponentRelatedFiles.Entry("HTML", html),
                new ComponentRelatedFiles.Entry("SCSS", scss)
        );

        assertEquals(html, ComponentSubtabNavigation.adjacentInList(ts, related, 1));
        assertEquals(scss, ComponentSubtabNavigation.adjacentInList(html, related, 1));
        assertNull(ComponentSubtabNavigation.adjacentInList(scss, related, 1));
    }

    @Test
    void selectsAdjacentSubtabToTheLeft() {
        LightVirtualFile ts = new LightVirtualFile("app.component.ts");
        LightVirtualFile html = new LightVirtualFile("app.component.html");
        List<ComponentRelatedFiles.Entry> related = List.of(
                new ComponentRelatedFiles.Entry("TS", ts),
                new ComponentRelatedFiles.Entry("HTML", html)
        );

        assertNull(ComponentSubtabNavigation.adjacentInList(ts, related, -1));
        assertEquals(ts, ComponentSubtabNavigation.adjacentInList(html, related, -1));
    }
}
