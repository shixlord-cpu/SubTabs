package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtabProjectViewGroupingTest {
    @Test
    void replacesFolderWhenEveryVisibleFileBelongsToTheSameGroup() {
        assertTrue(SubtabProjectViewGrouping.shouldReplaceFolder(
                List.of(
                        "user-card.component.ts",
                        "user-card.component.html",
                        "user-card.component.scss"
                ),
                0
        ));
    }

    @Test
    void keepsFolderWhenAnyVisibleFileIsNotSubtabbed() {
        assertFalse(SubtabProjectViewGrouping.shouldReplaceFolder(
                List.of(
                        "user-card.component.ts",
                        "user-card.component.html",
                        "README.md"
                ),
                0
        ));
    }

    @Test
    void keepsFolderWhenAVisibleSubdirectoryExists() {
        assertFalse(SubtabProjectViewGrouping.shouldReplaceFolder(
                List.of("user-card.component.ts", "user-card.component.html"),
                1
        ));
    }

    @Test
    void keepsFolderWhenAMatchingFileHasNoPartner() {
        assertFalse(SubtabProjectViewGrouping.shouldReplaceFolder(
                List.of("user-card.component.ts", "package.json"),
                0
        ));
    }

    @Test
    void nestsRelatedFilesInsideMixedFolders() {
        List<String> groupKeys = SubtabProjectViewGrouping.groupKeysForNesting(List.of(
                "user-card.component.ts",
                "user-card.component.html",
                "README.md"
        ));
        assertEquals(1, groupKeys.size());
        assertTrue(groupKeys.get(0).endsWith(":user-card#user-card.component"));
    }

    @Test
    void replacesFolderWithMultipleCompleteGroups() {
        assertEquals(
                2,
                SubtabProjectViewGrouping.groupKeysIfReplaceable(
                        List.of(
                                "user-card.component.ts",
                                "user-card.component.html",
                                "cart.reducer.ts",
                                "cart.actions.ts"
                        ),
                        0
                ).size()
        );
    }

    @Test
    void mergesNeighborStateGroupsAcrossFolders() {
        assertEquals(
                "merge:rule:3:products",
                SubtabProjectViewGrouping.mergeKey("rule:3:products")
        );
    }

    @Test
    void keepsLocalGroupsSeparateWithoutNeighborSearch() {
        assertEquals(
                "rule:7:products#products.component",
                SubtabProjectViewGrouping.mergeKey("rule:7:products#products.component")
        );
    }

    @Test
    void keepsStateFolderGroupsLocalWithoutNeighborMerge() {
        assertEquals(
                "rule:4:cart",
                SubtabProjectViewGrouping.mergeKey("rule:4:cart")
        );
    }

    @Test
    void keepsDistinctMergeKeysForDifferentComponentStems() {
        assertNotEquals(
                SubtabProjectViewGrouping.mergeKey("rule:7:products#products.component"),
                SubtabProjectViewGrouping.mergeKey("rule:7:user-card#user-card.component")
        );
    }

    @Test
    void usesStableMergeKeyForSameComponentGroup() {
        assertEquals(
                SubtabProjectViewGrouping.mergeKey("rule:7:products#products.component"),
                SubtabProjectViewGrouping.mergeKey("rule:7:products#products.component")
        );
    }
}
