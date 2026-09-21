package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

public final class SidetabSectionSpec {
    public String name = "";
    public String start = "";
    public String end = "";
    public boolean endIncludesMarker = false;
    public boolean foldable = true;

    public SidetabSectionSpec() {
    }

    public SidetabSectionSpec(@NotNull String name, @NotNull String start) {
        this(name, start, "", false, true);
    }

    public SidetabSectionSpec(@NotNull String name, @NotNull String start, @NotNull String end) {
        this(name, start, end, false, true);
    }

    public SidetabSectionSpec(
            @NotNull String name,
            @NotNull String start,
            @NotNull String end,
            boolean endIncludesMarker
    ) {
        this(name, start, end, endIncludesMarker, true);
    }

    public SidetabSectionSpec(
            @NotNull String name,
            @NotNull String start,
            @NotNull String end,
            boolean endIncludesMarker,
            boolean foldable
    ) {
        this.name = name;
        this.start = start;
        this.end = end;
        this.endIncludesMarker = endIncludesMarker;
        this.foldable = foldable;
    }

    public @NotNull SidetabSectionSpec copy() {
        return new SidetabSectionSpec(
                name == null ? "" : name,
                start == null ? "" : start,
                end == null ? "" : end,
                endIncludesMarker,
                foldable
        );
    }

    public boolean sameAs(@NotNull SidetabSectionSpec other) {
        return endIncludesMarker == other.endIncludesMarker
                && foldable == other.foldable
                && String.valueOf(name).equals(String.valueOf(other.name))
                && String.valueOf(start).equals(String.valueOf(other.start))
                && String.valueOf(end).equals(String.valueOf(other.end));
    }
}