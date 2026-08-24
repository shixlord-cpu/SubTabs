package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentSubtabProjectViewHoverTest {
    @Test
    void regularProjectViewNodesCountAsDirectFileNodes() {
        assertTrue(ComponentSubtabProjectViewHover.isDirectFileNode("PsiFileNode"));
    }
}
