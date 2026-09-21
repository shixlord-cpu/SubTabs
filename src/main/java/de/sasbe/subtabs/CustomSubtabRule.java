package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CustomSubtabRule {
    public enum Type {
        /** Legacy XML values only — trigger settings reset; never used in matching. */
        @Deprecated STEM,
        @Deprecated FILES,
        USER_GROUPS,
        FOLDER
    }

    public String name = "";
    public @Nullable Type type = null;
    public String patterns = "";
    public String nameSegments = "";
    public String groupNameSegments = "";
    public String slotKeys = "";
    public String groupSuffix = "";
    public String excludePatterns = "";
    public boolean searchNeighbors = false;
    public boolean enabled = true;
    public boolean builtin = false;

    public @NotNull CustomSubtabRule copy() {
        CustomSubtabRule copy = new CustomSubtabRule();
        copy.name = name;
        copy.type = type;
        copy.patterns = patterns;
        copy.nameSegments = nameSegments;
        copy.groupNameSegments = groupNameSegments;
        copy.slotKeys = slotKeys;
        copy.groupSuffix = groupSuffix;
        copy.excludePatterns = excludePatterns;
        copy.searchNeighbors = searchNeighbors;
        copy.enabled = enabled;
        copy.builtin = builtin;
        return copy;
    }

    public boolean isSpecial() {
        return type == Type.USER_GROUPS || type == Type.FOLDER;
    }
}
