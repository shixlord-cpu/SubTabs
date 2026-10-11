package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

public final class SubtabSelectNextAction extends SubtabSelectAdjacentAction {
    public SubtabSelectNextAction() {
        super("Next subtab", "Switch to the subtab on the right", 1);
    }
}
