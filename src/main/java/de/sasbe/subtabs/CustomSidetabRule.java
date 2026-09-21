package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class CustomSidetabRule {
    public static final String TOP_RULE_NAME = "TOP-Kommentare";
    public static final String LEGACY_FAMILY_RULE_NAME = "FAMILY-Kommentare";

    public String name = "";
    public String filePatterns = "";
    public boolean enabled = true;
    public List<SidetabSectionSpec> sectionSpecs = new ArrayList<>();
    public Type type = Type.NORMAL;
    public TopCommentMode familyMode = TopCommentMode.OVERRIDE;

    public Kind kind = Kind.REGEX;
    public String sections = "";
    public String patterns = "";
    public boolean builtin = false;
    public boolean respectOrder = false;

    public enum Type {
        NORMAL,
        TOP,
        /** @deprecated use {@link #TOP}; kept for persisted settings */
        @Deprecated
        FAMILY
    }

    public enum Kind {
        HTML,
        TS_COMPONENT,
        CSS,
        REGEX
    }

    public boolean isTopRule() {
        return type == Type.TOP || type == Type.FAMILY;
    }

    public @NotNull CustomSidetabRule copy() {
        CustomSidetabRule copy = new CustomSidetabRule();
        copy.name = name;
        copy.filePatterns = filePatterns;
        copy.enabled = enabled;
        copy.type = type == Type.FAMILY ? Type.TOP : type;
        copy.familyMode = familyMode;
        copy.kind = kind;
        copy.sections = sections;
        copy.patterns = patterns;
        copy.builtin = builtin;
        copy.respectOrder = respectOrder;
        copy.sectionSpecs = copySpecs();
        return copy;
    }

    public @NotNull List<SidetabSectionSpec> copySpecs() {
        List<SidetabSectionSpec> copy = new ArrayList<>();
        if (sectionSpecs == null) {
            return copy;
        }
        for (SidetabSectionSpec spec : sectionSpecs) {
            copy.add(spec.copy());
        }
        return copy;
    }

    public @NotNull String sectionsSummary() {
        if (isTopRule()) {
            return familyMode.summary();
        }
        if (sectionSpecs == null || sectionSpecs.isEmpty()) {
            return sections == null ? "" : sections;
        }
        return sectionSpecs.stream()
                .map(spec -> spec.name == null ? "" : spec.name)
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(", "));
    }

    public boolean sameAs(@NotNull CustomSidetabRule other) {
        if (enabled != other.enabled
                || familyMode != other.familyMode
                || respectOrder != other.respectOrder
                || !String.valueOf(name).equals(String.valueOf(other.name))
                || !String.valueOf(filePatterns).equals(String.valueOf(other.filePatterns))) {
            return false;
        }
        if (isTopRule() || other.isTopRule()) {
            return isTopRule() && other.isTopRule();
        }
        List<SidetabSectionSpec> left = sectionSpecs == null ? List.of() : sectionSpecs;
        List<SidetabSectionSpec> right = other.sectionSpecs == null ? List.of() : other.sectionSpecs;
        if (left.size() != right.size()) {
            return false;
        }
        for (int index = 0; index < left.size(); index++) {
            if (!left.get(index).sameAs(right.get(index))) {
                return false;
            }
        }
        return true;
    }
}