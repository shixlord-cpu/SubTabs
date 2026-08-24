package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

public final class CustomSubtabRule {
    public enum Type {
        STEM,
        FILES
    }

    public String name = "";
    public Type type = Type.STEM;
    public String patterns = "";
    public String labels = "";
    public String slotKeys = "";
    public String groupSuffix = "";
    public boolean searchNeighbors = false;
    public boolean stripComponentSuffix = false;

    public @NotNull CustomSubtabRule copy() {
        CustomSubtabRule copy = new CustomSubtabRule();
        copy.name = name;
        copy.type = type;
        copy.patterns = patterns;
        copy.labels = labels;
        copy.slotKeys = slotKeys;
        copy.groupSuffix = groupSuffix;
        copy.searchNeighbors = searchNeighbors;
        copy.stripComponentSuffix = stripComponentSuffix;
        return copy;
    }
}
