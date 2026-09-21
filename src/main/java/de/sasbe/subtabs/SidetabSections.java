package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SidetabSections {
    private static final Pattern TOP_END_MARKER = Pattern.compile(
            "(?:TOP|FAMILY)(?:-END|END)(?:-(?:\\{\\{([^}]+)\\}\\}|([^\\s*/<]+)))?"
    );
    private static final Pattern TOP_MARKER = Pattern.compile(
            "(?:TOP|FAMILY)-(?:\\{\\{([^}]+)\\}\\}|([^\\s*/<]+))"
    );
    private static final Pattern SUB_END_MARKER = Pattern.compile(
            "(\\+*)(?:SUB)(?:-END|END)(?:-(?:\\{\\{([^}]+)\\}\\}|([^\\s*/<]+)))?"
    );
    private static final Pattern SUB_MARKER = Pattern.compile(
            "(\\+*)(?:SUB)-(?:\\{\\{([^}]+)\\}\\}|([^\\s*/<]+))"
    );

    private SidetabSections() {
    }

    static @NotNull List<SidetabSection> split(
            @NotNull String fileName,
            @NotNull String text,
            @NotNull List<CustomSidetabRule> rules
    ) {
        CustomSidetabRule topRule = SidetabRulesDefaults.findTopRule(rules);
        boolean topEnabled = topRule != null && topRule.enabled;
        boolean combineComments = topRule != null && topRule.familyMode == TopCommentMode.COMBINE;
        List<SidetabSection> topSections = topEnabled ? parseTopComments(text) : List.of();
        List<SidetabSection> fromRules = splitByMatchingRule(
                fileName,
                text,
                SidetabRulesDefaults.normalRules(rules)
        );
        if (!topEnabled) {
            return fromRules;
        }
        if (!combineComments) {
            if (topSections.isEmpty()) {
                return fromRules;
            }
            if (hasTopLevelCommentSections(topSections)) {
                return topSections;
            }
            if (fromRules.isEmpty()) {
                return topSections;
            }
            return mergeSections(fromRules, topSections, text);
        }
        if (topSections.isEmpty()) {
            return fromRules;
        }
        if (fromRules.isEmpty()) {
            return topSections;
        }
        return mergeSections(fromRules, topSections, text);
    }

    private static boolean hasTopLevelCommentSections(@NotNull List<SidetabSection> commentSections) {
        for (SidetabSection section : commentSections) {
            if (section.depth() == 0 && section.fixedDepth()) {
                return true;
            }
        }
        return false;
    }

    static @NotNull List<SidetabSection> split(
            @NotNull String fileName,
            @NotNull String text,
            @NotNull List<CustomSidetabRule> rules,
            boolean combineComments
    ) {
        if (!combineComments) {
            return split(fileName, text, rules);
        }
        CustomSidetabRule topRule = SidetabRulesDefaults.findTopRule(rules);
        if (topRule == null) {
            topRule = SidetabRulesDefaults.topCommentsRule();
            topRule.enabled = true;
            topRule.familyMode = TopCommentMode.COMBINE;
            List<CustomSidetabRule> withTop = new ArrayList<>(rules);
            withTop.add(0, topRule);
            return split(fileName, text, withTop);
        }
        TopCommentMode previous = topRule.familyMode;
        boolean previousEnabled = topRule.enabled;
        topRule.enabled = true;
        topRule.familyMode = TopCommentMode.COMBINE;
        try {
            return split(fileName, text, rules);
        } finally {
            topRule.familyMode = previous;
            topRule.enabled = previousEnabled;
        }
    }

    static @NotNull List<SidetabSection> parseTopComments(@NotNull String text) {
        List<CommentMarker> markers = new ArrayList<>();
        int lineStart = 0;
        while (lineStart <= text.length()) {
            int lineEnd = text.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                lineEnd = text.length();
            }
            String line = text.substring(lineStart, lineEnd);
            if (isCommentLine(line)) {
                CommentMarker marker = parseCommentMarker(line, lineStart);
                if (marker != null) {
                    markers.add(marker);
                }
            }
            if (lineEnd == text.length()) {
                break;
            }
            lineStart = lineEnd + 1;
        }
        if (markers.isEmpty()) {
            return List.of();
        }
        return dropBlank(applyExplicitCommentEnds(text, buildTopCommentSections(markers, text)), text);
    }

    private static @NotNull List<SidetabSection> applyExplicitCommentEnds(
            @NotNull String text,
            @NotNull List<SidetabSection> sections
    ) {
        if (sections.isEmpty()) {
            return sections;
        }
        List<SidetabSection> adjusted = new ArrayList<>(sections.size());
        for (SidetabSection section : sections) {
            int explicitEnd = explicitCommentEnd(text, section);
            if (explicitEnd >= 0 && explicitEnd < section.endOffset()) {
                adjusted.add(new SidetabSection(
                        section.name(),
                        section.startOffset(),
                        explicitEnd,
                        section.depth(),
                        section.fixedDepth(),
                        section.foldable()
                ));
            } else {
                adjusted.add(section);
            }
        }
        return adjusted;
    }

    private static int explicitCommentEnd(@NotNull String text, @NotNull SidetabSection section) {
        if (!section.fixedDepth()) {
            return -1;
        }
        int searchFrom = section.startOffset();
        int best = -1;
        int lineStart = 0;
        while (lineStart <= text.length()) {
            int lineEnd = text.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                lineEnd = text.length();
            }
            if (lineStart >= searchFrom && lineStart < section.endOffset()) {
                String line = text.substring(lineStart, lineEnd);
                CommentMarker marker = parseCommentMarker(line, lineStart);
                if (marker != null && marker.end() && marker.depth() == section.depth()) {
                    if (marker.sectionName().isEmpty() || marker.sectionName().equals(section.name())) {
                        best = marker.offset();
                    }
                }
            }
            if (lineEnd == text.length()) {
                break;
            }
            lineStart = lineEnd + 1;
        }
        return best;
    }

    static @NotNull List<SidetabSection> splitByRule(@NotNull String text, @NotNull CustomSidetabRule rule) {
        List<SidetabSectionSpec> specs = rule.sectionSpecs;
        if (specs == null || specs.isEmpty()) {
            specs = SidetabRulesDefaults.specsFromLegacy(rule);
        }
        if (specs.isEmpty()) {
            return List.of();
        }

        List<LocatedSpec> located = rule.respectOrder
                ? locateSpecsInOrder(text, specs)
                : locateSpecsAnyOrder(text, specs);
        if (located == null) {
            return List.of();
        }
        return assignNestedDepths(sectionsFromLocated(located, text));
    }

    private static @Nullable List<LocatedSpec> locateSpecsInOrder(
            @NotNull String text,
            @NotNull List<SidetabSectionSpec> specs
    ) {
        List<LocatedSpec> located = new ArrayList<>();
        int searchFrom = 0;
        for (SidetabSectionSpec spec : specs) {
            String start = spec.start == null ? "" : spec.start;
            int found = SidetabStartMatcher.find(text, start, searchFrom);
            if (found < 0) {
                continue;
            }
            located.add(new LocatedSpec(spec, found));
            searchFrom = Math.min(text.length(), found + (SidetabStartMatcher.isFileStart(start) ? 0 : 1));
            if (SidetabStartMatcher.isFileStart(start)) {
                searchFrom = found;
            }
        }
        return located;
    }

    private static @Nullable List<LocatedSpec> locateSpecsAnyOrder(
            @NotNull String text,
            @NotNull List<SidetabSectionSpec> specs
    ) {
        List<LocatedSpec> located = new ArrayList<>();
        for (SidetabSectionSpec spec : specs) {
            String start = spec.start == null ? "" : spec.start;
            int found = SidetabStartMatcher.find(text, start, 0);
            if (found < 0) {
                continue;
            }
            located.add(new LocatedSpec(spec, found));
        }
        located.sort(Comparator.comparingInt(entry -> entry.offset));
        return located;
    }

    static @Nullable CommentMarker parseCommentMarkerForTest(@NotNull String line, int offset) {
        return parseCommentMarker(line, offset);
    }

    private static @Nullable CommentMarker parseCommentMarker(@NotNull String line, int offset) {
        Matcher subEnd = SUB_END_MARKER.matcher(line);
        if (subEnd.find()) {
            String name = firstNonBlank(subEnd.group(2), subEnd.group(3));
            return new CommentMarker(offset, subDepth(subEnd.group(1)), true, orEmpty(trimOrNull(name)));
        }
        Matcher subStart = SUB_MARKER.matcher(line);
        if (subStart.find()) {
            String name = firstNonBlank(subStart.group(2), subStart.group(3));
            if (name != null && !name.isBlank() && !"END".equalsIgnoreCase(name.trim())) {
                return new CommentMarker(offset, subDepth(subStart.group(1)), false, name.trim());
            }
        }
        Matcher topEnd = TOP_END_MARKER.matcher(line);
        if (topEnd.find()) {
            String name = firstNonBlank(topEnd.group(1), topEnd.group(2));
            return new CommentMarker(offset, 0, true, orEmpty(trimOrNull(name)));
        }
        Matcher topStart = TOP_MARKER.matcher(line);
        if (topStart.find()) {
            String name = firstNonBlank(topStart.group(1), topStart.group(2));
            if (name != null && !name.isBlank() && !"END".equalsIgnoreCase(name.trim())) {
                return new CommentMarker(offset, 0, false, name.trim());
            }
        }
        return null;
    }

    private static int subDepth(@Nullable String plusPrefix) {
        return plusPrefix == null ? 1 : plusPrefix.length() + 1;
    }

    private static @NotNull List<SidetabSection> buildTopCommentSections(
            @NotNull List<CommentMarker> markers,
            @NotNull String text
    ) {
        Deque<OpenSection> open = new ArrayDeque<>();
        List<SidetabSection> sections = new ArrayList<>();
        for (CommentMarker marker : markers) {
            if (marker.end) {
                if (marker.depth == 0) {
                    closeAllAtOrAbove(open, sections, 0, marker.offset);
                } else {
                    closeToDepth(open, sections, marker.depth, marker.sectionName, marker.offset);
                }
            } else {
                while (!open.isEmpty() && open.peek().depth >= marker.depth) {
                    closeOne(open, sections, marker.offset);
                }
                open.push(new OpenSection(marker.sectionName, marker.offset, marker.depth));
            }
        }
        while (!open.isEmpty()) {
            closeOne(open, sections, text.length());
        }
        sections.sort(Comparator.comparingInt(SidetabSection::startOffset));
        return sections;
    }

    private static void closeAllAtOrAbove(
            @NotNull Deque<OpenSection> open,
            @NotNull List<SidetabSection> sections,
            int depth,
            int offset
    ) {
        while (!open.isEmpty() && open.peek().depth >= depth) {
            closeOne(open, sections, offset);
        }
    }

    private static void closeToDepth(
            @NotNull Deque<OpenSection> open,
            @NotNull List<SidetabSection> sections,
            int depth,
            @NotNull String name,
            int offset
    ) {
        while (!open.isEmpty()) {
            OpenSection top = open.peek();
            if (top.depth < depth) {
                return;
            }
            if (top.depth == depth && (name.isEmpty() || name.equals(top.name))) {
                closeOne(open, sections, offset);
                return;
            }
            if (top.depth > depth) {
                closeOne(open, sections, offset);
                continue;
            }
            return;
        }
    }

    private static void closeOne(
            @NotNull Deque<OpenSection> open,
            @NotNull List<SidetabSection> sections,
            int offset
    ) {
        OpenSection section = open.pop();
        sections.add(new SidetabSection(section.name, section.start, offset, section.depth, true));
    }

    private static @NotNull List<SidetabSection> sectionsFromLocated(
            @NotNull List<LocatedSpec> located,
            @NotNull String text
    ) {
        if (located.isEmpty()) {
            return List.of();
        }
        List<SidetabSection> sections = new ArrayList<>();
        for (int index = 0; index < located.size(); index++) {
            SidetabSectionSpec spec = located.get(index).spec;
            String name = spec.name == null ? "" : spec.name.trim();
            if (name.isEmpty()) {
                continue;
            }
            int start = located.get(index).offset;
            int nextStart = index + 1 < located.size() ? located.get(index + 1).offset : text.length();
            int end = sectionEnd(text, spec, start, nextStart);
            if (end <= start) {
                end = nextStart;
            }
            sections.add(new SidetabSection(name, start, end, 0, false, spec.foldable));
        }
        return dropBlank(sections, text);
    }

    private static int sectionEnd(
            @NotNull String text,
            @NotNull SidetabSectionSpec spec,
            int start,
            int nextStart
    ) {
        String endExpr = spec.end == null ? "" : spec.end.trim();
        if (endExpr.isEmpty()) {
            return nextStart;
        }
        int found = SidetabStartMatcher.find(text, endExpr, start + 1);
        if (found < 0 || found <= start) {
            return nextStart;
        }
        int resolved = SidetabStartMatcher.resolveEndOffset(text, found, spec.endIncludesMarker);
        if (resolved < 0) {
            return nextStart;
        }
        return resolved;
    }

    private static @NotNull List<SidetabSection> splitByMatchingRule(
            @NotNull String fileName,
            @NotNull String text,
            @NotNull List<CustomSidetabRule> rules
    ) {
        for (CustomSidetabRule rule : rules) {
            if (!rule.enabled || !SidetabFilePatterns.matches(fileName, rule.filePatterns)) {
                continue;
            }
            List<SidetabSection> sections = splitByRule(text, rule);
            if (!sections.isEmpty()) {
                return sections;
            }
        }
        return List.of();
    }

    private static @NotNull List<SidetabSection> mergeSections(
            @NotNull List<SidetabSection> left,
            @NotNull List<SidetabSection> right,
            @NotNull String text
    ) {
        Map<Integer, SidetabSection> byStart = new LinkedHashMap<>();
        for (SidetabSection section : left) {
            byStart.put(section.startOffset(), section);
        }
        for (SidetabSection section : right) {
            byStart.put(section.startOffset(), section);
        }
        List<SidetabSection> sorted = byStart.values().stream()
                .sorted(Comparator.comparingInt(SidetabSection::startOffset))
                .toList();
        if (sorted.isEmpty()) {
            return List.of();
        }
        List<SidetabSection> merged = new ArrayList<>(sorted.size());
        for (int index = 0; index < sorted.size(); index++) {
            SidetabSection current = sorted.get(index);
            int end = current.endOffset();
            for (int nextIndex = index + 1; nextIndex < sorted.size(); nextIndex++) {
                SidetabSection next = sorted.get(nextIndex);
                if (isNestedWithin(next, current)) {
                    continue;
                }
                end = Math.min(end, next.startOffset());
                break;
            }
            if (end <= current.startOffset()) {
                end = index + 1 < sorted.size()
                        ? sorted.get(index + 1).startOffset()
                        : text.length();
            }
            merged.add(new SidetabSection(
                    current.name(),
                    current.startOffset(),
                    end,
                    current.depth(),
                    current.fixedDepth(),
                    current.foldable()
            ));
        }
        return assignNestedDepths(merged);
    }

    private static boolean isNestedWithin(@NotNull SidetabSection inner, @NotNull SidetabSection outer) {
        return inner.startOffset() >= outer.startOffset()
                && inner.endOffset() <= outer.endOffset()
                && inner.startOffset() < outer.endOffset();
    }

    static @NotNull List<SidetabSection> assignNestedDepths(@NotNull List<SidetabSection> sections) {
        if (sections.isEmpty()) {
            return sections;
        }
        List<SidetabSection> sorted = sections.stream()
                .sorted(Comparator.comparingInt(SidetabSection::startOffset))
                .toList();
        List<SidetabSection> result = new ArrayList<>(sorted.size());
        for (int index = 0; index < sorted.size(); index++) {
            SidetabSection section = sorted.get(index);
            if (section.fixedDepth()) {
                result.add(section);
                continue;
            }
            int depth = nestedDepth(sorted, index);
            result.add(section.withDepth(depth, false));
        }
        return result;
    }

    private static int nestedDepth(@NotNull List<SidetabSection> sorted, int index) {
        SidetabSection section = sorted.get(index);
        int depth = 0;
        for (int candidateIndex = 0; candidateIndex < index; candidateIndex++) {
            SidetabSection candidate = sorted.get(candidateIndex);
            if (candidate.startOffset() <= section.startOffset()
                    && candidate.endOffset() >= section.endOffset()) {
                depth = Math.max(depth, candidate.depth() + 1);
            }
        }
        return depth;
    }

    private static @NotNull List<SidetabSection> dropBlank(
            @NotNull List<SidetabSection> sections,
            @NotNull String text
    ) {
        List<SidetabSection> kept = new ArrayList<>(sections.size());
        for (SidetabSection section : sections) {
            if (section.endOffset() > section.startOffset() && !section.isBlank(text)) {
                kept.add(section);
            }
        }
        return kept;
    }

    private static boolean isCommentLine(@NotNull String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("//")
                || trimmed.startsWith("/*")
                || trimmed.startsWith("*")
                || trimmed.startsWith("<!--")
                || trimmed.startsWith("#")
                || trimmed.startsWith("{#")
                || trimmed.startsWith("--");
    }

    private static @NotNull String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    private static @Nullable String trimOrNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    record CommentMarker(int offset, int depth, boolean end, @NotNull String sectionName) {
        CommentMarker {
            sectionName = sectionName == null ? "" : sectionName;
        }
    }

    private record OpenSection(@NotNull String name, int start, int depth) {
    }

    private record LocatedSpec(@NotNull SidetabSectionSpec spec, int offset) {
    }
}