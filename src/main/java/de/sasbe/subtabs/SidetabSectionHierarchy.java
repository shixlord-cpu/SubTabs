package de.sasbe.subtabs;

import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class SidetabSectionHierarchy {
    private SidetabSectionHierarchy() {
    }

    static int parentIndex(@NotNull List<SidetabSection> sections, int index) {
        if (index < 0 || index >= sections.size()) {
            return -1;
        }
        SidetabSection section = sections.get(index);
        int best = -1;
        int bestDepth = -1;
        for (int candidateIndex = 0; candidateIndex < sections.size(); candidateIndex++) {
            if (candidateIndex == index) {
                continue;
            }
            SidetabSection candidate = sections.get(candidateIndex);
            if (candidate.depth() >= section.depth()) {
                continue;
            }
            if (candidate.startOffset() <= section.startOffset()
                    && candidate.endOffset() >= section.endOffset()
                    && candidate.depth() > bestDepth) {
                best = candidateIndex;
                bestDepth = candidate.depth();
            }
        }
        return best;
    }

    static @NotNull List<Integer> directChildren(@NotNull List<SidetabSection> sections, int parentSectionIndex) {
        List<Integer> children = new ArrayList<>();
        for (int index = 0; index < sections.size(); index++) {
            if (parentIndex(sections, index) == parentSectionIndex) {
                children.add(index);
            }
        }
        return children;
    }

    static boolean isHiddenByFoldedAncestor(
            @NotNull Editor editor,
            @NotNull List<SidetabSection> sections,
            int index
    ) {
        int parent = parentIndex(sections, index);
        while (parent >= 0) {
            if (SidetabSectionFolding.isFolded(editor, sections.get(parent))) {
                return true;
            }
            parent = parentIndex(sections, parent);
        }
        return false;
    }

    static void expandAncestors(@NotNull Editor editor, @NotNull List<SidetabSection> sections, int index) {
        int parent = parentIndex(sections, index);
        while (parent >= 0) {
            SidetabSection parentSection = sections.get(parent);
            if (SidetabSectionFolding.isFolded(editor, parentSection)) {
                SidetabSectionFolding.setExpanded(editor, parentSection, true);
            }
            parent = parentIndex(sections, parent);
        }
    }
}
