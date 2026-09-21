package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Describes the target layout of a group split.
 *
 * <p>The platform always creates the new editor pane on the right side of the anchor pane, so
 * {@link #newPaneFile()} names the file that has to be handed to {@code EditorWindow.split(...)} and
 * {@link #anchorPaneFile()} names the file the original pane has to end up with.
 */
record SubtabGroupSplitPlan(
        @NotNull VirtualFile leftFile,
        @NotNull VirtualFile rightFile
) {
    static @NotNull SubtabGroupSplitPlan of(
            @NotNull ComponentSubtabGroupSplitNavigation.SplitSide side,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile targetFile
    ) {
        return side == ComponentSubtabGroupSplitNavigation.SplitSide.RIGHT
                ? new SubtabGroupSplitPlan(anchorFile, targetFile)
                : new SubtabGroupSplitPlan(targetFile, anchorFile);
    }

    @NotNull VirtualFile anchorPaneFile() {
        return leftFile;
    }

    @NotNull VirtualFile newPaneFile() {
        return rightFile;
    }

    boolean covers(@NotNull VirtualFile file) {
        return leftFile.equals(file) || rightFile.equals(file);
    }
}
