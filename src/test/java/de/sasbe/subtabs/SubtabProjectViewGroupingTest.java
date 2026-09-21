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
        assertTrue(groupKeys.get(0).endsWith(":user-card"));
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
    void mergesFolderGroupsByRuleGroupName() {
        assertEquals(
                "merge:rule:7:products",
                SubtabProjectViewGrouping.mergeKey("rule:7:products#products.component")
        );
    }

    @Test
    void mergesStateFeatureGroupsBySegmentName() {
        assertEquals(
                "merge:rule:4:actions",
                SubtabProjectViewGrouping.mergeKey("rule:4:actions#cart")
        );
        assertEquals(
                SubtabProjectViewGrouping.mergeKey("rule:4:actions#cart"),
                SubtabProjectViewGrouping.mergeKey("rule:4:actions#catalog")
        );
    }

    @Test
    void projectViewUsesOnlyFirstMatchingStateRule() {
        List<SubtabProjectViewGrouping.ProjectViewGroup> groups = SubtabProjectViewGrouping.groupsForActiveRules(
                List.of(
                        "cart.actions.ts",
                        "catalog.actions.ts",
                        "cart.reducer.ts",
                        "catalog.reducer.ts"
                )
        );

        assertTrue(groups.stream().anyMatch(group -> "rule:3:cart".equals(group.groupKey())));
        assertTrue(groups.stream().anyMatch(group -> "rule:3:catalog".equals(group.groupKey())));
        assertTrue(groups.stream().noneMatch(group -> group.groupKey().contains("actions#")));
        assertTrue(groups.stream().noneMatch(group -> group.groupKey().contains("reducer#")));
    }

    @Test
    void groupsStateFeatureActionsAcrossEntitiesInFolder() {
        List<SubtabProjectViewGrouping.ProjectViewGroup> groups = SubtabProjectViewGrouping.groupsForRule(
                List.of(
                        "cart.actions.ts",
                        "catalog.actions.ts",
                        "user.actions.ts",
                        "cart.reducer.ts"
                ),
                4
        );

        assertEquals(1, groups.size());
        assertEquals("rule:4:actions#cart", groups.get(0).groupKey());
        assertEquals(3, groups.get(0).fileNames().size());
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
