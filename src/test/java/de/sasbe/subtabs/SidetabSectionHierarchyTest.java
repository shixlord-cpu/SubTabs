package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidetabSectionHierarchyTest {
    @Test
    void findsDirectParentByContainmentAndDepth() {
        List<SidetabSection> sections = List.of(
                new SidetabSection("Header", 0, 100, 0, false),
                new SidetabSection("Nav", 10, 40, 1, false),
                new SidetabSection("Main", 100, 200, 0, false)
        );
        assertEquals(0, SidetabSectionHierarchy.parentIndex(sections, 1));
        assertEquals(-1, SidetabSectionHierarchy.parentIndex(sections, 0));
    }

    @Test
    void listsDirectChildren() {
        List<SidetabSection> sections = List.of(
                new SidetabSection("Header", 0, 100, 0, false),
                new SidetabSection("Nav", 10, 40, 1, false),
                new SidetabSection("Main", 100, 200, 0, false)
        );
        assertEquals(List.of(1), SidetabSectionHierarchy.directChildren(sections, 0));
        assertTrue(SidetabSectionHierarchy.directChildren(sections, 2).isEmpty());
    }
}
