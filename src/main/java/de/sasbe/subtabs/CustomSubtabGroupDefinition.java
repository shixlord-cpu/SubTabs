package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

public final class CustomSubtabGroupDefinition {
    public String name = "";
    public String patterns = "";
    public String labels = "";

    public @NotNull CustomSubtabGroupDefinition copy() {
        CustomSubtabGroupDefinition copy = new CustomSubtabGroupDefinition();
        copy.name = name;
        copy.patterns = patterns;
        copy.labels = labels;
        return copy;
    }
}
