package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertTrue(groupKeys.get(0).endsWith(":user-card.component"));
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
}
