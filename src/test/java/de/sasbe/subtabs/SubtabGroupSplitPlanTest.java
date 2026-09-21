package de.sasbe.subtabs;

import com.intellij.mock.MockVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtabGroupSplitPlanTest {
    private final VirtualFile anchor = new MockVirtualFile("product-list.component.html");
    private final VirtualFile target = new MockVirtualFile("product-list.component.spec.ts");

    @Test
    void splitRightKeepsAnchorOnTheLeft() {
        SubtabGroupSplitPlan plan = SubtabGroupSplitPlan.of(
                ComponentSubtabGroupSplitNavigation.SplitSide.RIGHT,
                anchor,
                target
        );

        assertEquals(anchor, plan.leftFile());
        assertEquals(target, plan.rightFile());
        assertEquals(anchor, plan.anchorPaneFile());
        assertEquals(target, plan.newPaneFile());
    }

    @Test
    void splitLeftMovesTargetIntoTheAnchorPane() {
        SubtabGroupSplitPlan plan = SubtabGroupSplitPlan.of(
                ComponentSubtabGroupSplitNavigation.SplitSide.LEFT,
                anchor,
                target
        );

        assertEquals(target, plan.leftFile());
        assertEquals(anchor, plan.rightFile());
        assertEquals(target, plan.anchorPaneFile(), "left split must swap the original pane to the target");
        assertEquals(anchor, plan.newPaneFile(), "the platform always creates the new pane on the right");
    }

    @Test
    void coversOnlyTheTwoSplitFiles() {
        SubtabGroupSplitPlan plan = SubtabGroupSplitPlan.of(
                ComponentSubtabGroupSplitNavigation.SplitSide.RIGHT,
                anchor,
                target
        );

        assertTrue(plan.covers(anchor));
        assertTrue(plan.covers(target));
        assertFalse(plan.covers(new MockVirtualFile("product-list.component.scss")));
    }
}
