package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ComponentSubtabMainTabHoverTest {
    @Test
    void keepsDistinctMergeKeysForDifferentComponentStems() {
        String products = "rule:5:products.component";
        String userCard = "rule:5:user-card.component";

        assertNotEquals(
                SubtabProjectViewGrouping.mergeKey(products),
                SubtabProjectViewGrouping.mergeKey(userCard)
        );
    }

    @Test
    void usesStableMergeKeyForSameComponentGroup() {
        String first = "rule:5:products.component";
        String second = "rule:5:products.component";

        assertEquals(
                SubtabProjectViewGrouping.mergeKey(first),
                SubtabProjectViewGrouping.mergeKey(second)
        );
    }
}
