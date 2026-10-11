package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

public final class SubtabSelectPreviousAction extends SubtabSelectAdjacentAction {
    public SubtabSelectPreviousAction() {
        super("Previous subtab", "Switch to the subtab on the left", -1);
    }
}
