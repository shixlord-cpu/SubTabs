package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SubtabGroupNavigationTest {
    private final VirtualFile ts = new LightVirtualFile("user-card.component.ts");
    private final VirtualFile html = new LightVirtualFile("user-card.component.html");
    private final VirtualFile scss = new LightVirtualFile("user-card.component.scss");

    @Test
    void prefersSelectedOpenFileInGroup() {
        VirtualFile selected = preferred(
                new VirtualFile[]{html},
                List.of(ts, html, scss),
                Set.of(ts, html)
        );

        assertEquals(html, selected);
    }

    @Test
    void fallsBackToFirstOpenFileInGroupOrder() {
        VirtualFile selected = preferred(
                new VirtualFile[]{new LightVirtualFile("readme.md")},
                List.of(ts, html, scss),
                Set.of(html, scss)
        );

        assertEquals(html, selected);
    }

    @Test
    void returnsNullWhenNoGroupFileIsOpen() {
        assertNull(preferred(
                new VirtualFile[]{ts},
                List.of(ts, html),
                Set.of()
        ));
    }

    private VirtualFile preferred(
            VirtualFile[] selectedFiles,
            List<VirtualFile> groupFiles,
            Set<VirtualFile> openFiles
    ) {
        Predicate<VirtualFile> isOpen = openFiles::contains;
        return SubtabGroupNavigation.preferredOpenFile(
                selectedFiles,
                groupFiles,
                isOpen
        );
    }
}
