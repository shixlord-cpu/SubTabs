package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CustomSubtabRuleMatcher {
    private static final String GROUP_PREFIX = "rule:";
    private static final String EXTENSION_GROUP_MARKER = "@ext:";
    private static final Pattern GROUP_KEY = Pattern.compile("^rule:(\\d+):(.+)$");
    private static final Pattern EXTENSION_GROUP_KEY =
            Pattern.compile("^rule:(\\d+):@ext:(.+)$");

    private CustomSubtabRuleMatcher() {
    }

    record Match(
            @NotNull String groupKey,
            @NotNull String displayName,
            @NotNull List<SubtabCandidate> candidates,
            boolean searchNeighbors
    ) {
    }

    record ParsedGroupKey(int ruleIndex, @NotNull String stem) {
    }

    record ExtensionGroupSpec(
            int ruleIndex,
            @NotNull CustomSubtabRule rule,
            @NotNull List<String> extensions
    ) {
        boolean matches(@NotNull String fileName) {
            for (String extension : extensions) {
                if (fileName.endsWith(extension) && fileName.length() >= extension.length()) {
                    return true;
                }
            }
            return false;
        }
    }

    static @Nullable Match match(@NotNull String fileName, @NotNull List<CustomSubtabRule> rules) {
        for (int index = 0; index < rules.size(); index++) {
            Match match = matchRule(fileName, rules.get(index), index);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    static @Nullable ParsedGroupKey parseGroupKey(@NotNull String groupKey) {
        Matcher matcher = GROUP_KEY.matcher(groupKey);
        if (!matcher.matches()) {
            return null;
        }
        return new ParsedGroupKey(Integer.parseInt(matcher.group(1)), matcher.group(2));
    }

    static @Nullable ExtensionGroupSpec parseExtensionGroup(
            @NotNull String groupKey,
            @NotNull List<CustomSubtabRule> rules
    ) {
        Matcher matcher = EXTENSION_GROUP_KEY.matcher(groupKey);
        if (!matcher.matches()) {
            return null;
        }

        int ruleIndex = Integer.parseInt(matcher.group(1));
        if (ruleIndex < 0 || ruleIndex >= rules.size()) {
            return null;
        }

        CustomSubtabRule rule = rules.get(ruleIndex);
        List<String> extensions = List.of(matcher.group(2).split("\\|"));
        return new ExtensionGroupSpec(ruleIndex, rule, extensions);
    }

    static @Nullable Match resolveGroup(
            @NotNull String groupKey,
            @NotNull List<CustomSubtabRule> rules
    ) {
        ExtensionGroupSpec extensionGroup = parseExtensionGroup(groupKey, rules);
        if (extensionGroup != null) {
            return buildExtensionFolderMatch(extensionGroup.rule(), extensionGroup.ruleIndex(), extensionGroup.extensions());
        }

        ParsedGroupKey parsed = parseGroupKey(groupKey);
        if (parsed == null || parsed.ruleIndex() < 0 || parsed.ruleIndex() >= rules.size()) {
            return null;
        }

        CustomSubtabRule rule = rules.get(parsed.ruleIndex());
        if (rule.type == CustomSubtabRule.Type.FILES) {
            return buildFilesMatch(rule, parsed.ruleIndex());
        }
        return buildStemMatch(rule, parsed.ruleIndex(), parsed.stem());
    }

    static boolean isExtensionFolderGroupKey(@NotNull String groupKey) {
        return EXTENSION_GROUP_KEY.matcher(groupKey).matches();
    }

    private static @Nullable Match matchRule(
            @NotNull String fileName,
            @NotNull CustomSubtabRule rule,
            int index
    ) {
        List<String> patterns = parseCsv(rule.patterns);
        if (patterns.isEmpty()) {
            return null;
        }

        return switch (rule.type) {
            case STEM -> matchStem(fileName, rule, index, patterns);
            case FILES -> matchFiles(fileName, rule, index, patterns);
        };
    }

    private static @Nullable Match matchStem(
            @NotNull String fileName,
            @NotNull CustomSubtabRule rule,
            int index,
            @NotNull List<String> patterns
    ) {
        List<String> suffixes = patterns.stream()
                .map(CustomSubtabRuleMatcher::normalizeSuffix)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();

        for (String suffix : suffixes) {
            if (!fileName.endsWith(suffix) || fileName.length() <= suffix.length()) {
                continue;
            }

            String stem = fileName.substring(0, fileName.length() - suffix.length());
            if (stem.isEmpty()) {
                continue;
            }
            return buildStemMatch(rule, index, stem);
        }
        return null;
    }

    private static @NotNull Match buildStemMatch(
            @NotNull CustomSubtabRule rule,
            int index,
            @NotNull String stem
    ) {
        return new Match(
                groupKey(index, stem),
                displayNameForStem(rule, stem),
                buildCandidates(rule, stem),
                rule.searchNeighbors
        );
    }

    private static @Nullable Match matchFiles(
            @NotNull String fileName,
            @NotNull CustomSubtabRule rule,
            int index,
            @NotNull List<String> patterns
    ) {
        if (isExtensionFolderRule(patterns)) {
            List<String> extensions = patterns.stream()
                    .map(CustomSubtabRuleMatcher::normalizeSuffix)
                    .toList();
            for (String extension : extensions) {
                if (fileName.endsWith(extension) && fileName.length() >= extension.length()) {
                    return buildExtensionFolderMatch(rule, index, extensions);
                }
            }
            return null;
        }

        if (!patterns.contains(fileName)) {
            return null;
        }
        return buildFilesMatch(rule, index);
    }

    private static @NotNull Match buildFilesMatch(@NotNull CustomSubtabRule rule, int index) {
        return new Match(
                groupKey(index, "@files"),
                !rule.name.isBlank() ? rule.name : "Dateien",
                buildCandidates(rule, ""),
                rule.searchNeighbors
        );
    }

    private static @NotNull Match buildExtensionFolderMatch(
            @NotNull CustomSubtabRule rule,
            int index,
            @NotNull List<String> extensions
    ) {
        String extensionKey = String.join("|", extensions);
        String displayName = !rule.name.isBlank() ? rule.name : extensionKey;
        return new Match(
                groupKey(index, EXTENSION_GROUP_MARKER + extensionKey),
                displayName,
                List.of(),
                rule.searchNeighbors
        );
    }

    static boolean isRuleGroupKey(@NotNull String groupKey) {
        return groupKey.startsWith(GROUP_PREFIX);
    }

    private static @NotNull String groupKey(int index, @NotNull String suffix) {
        return GROUP_PREFIX + index + ":" + suffix;
    }

    private static boolean isExtensionFolderRule(@NotNull List<String> patterns) {
        return !patterns.isEmpty() && patterns.stream().allMatch(CustomSubtabRuleMatcher::isExtensionOnlyPattern);
    }

    private static boolean isExtensionOnlyPattern(@NotNull String pattern) {
        if (!pattern.startsWith(".")) {
            return false;
        }
        return pattern.indexOf('.', 1) < 0;
    }

    private static @NotNull List<SubtabCandidate> buildCandidates(
            @NotNull CustomSubtabRule rule,
            @NotNull String stem
    ) {
        List<String> patterns = parseCsv(rule.patterns);
        List<String> labels = parseCsv(rule.labels);
        List<String> slotKeys = parseCsv(rule.slotKeys);
        List<SubtabCandidate> candidates = new ArrayList<>();

        for (int index = 0; index < patterns.size(); index++) {
            String pattern = patterns.get(index);
            String normalized = rule.type == CustomSubtabRule.Type.STEM
                    ? normalizeSuffix(pattern)
                    : pattern;
            String fileName = rule.type == CustomSubtabRule.Type.STEM
                    ? stem + normalized
                    : pattern;
            String slotId = index < slotKeys.size() && !slotKeys.get(index).isBlank()
                    ? slotKeys.get(index)
                    : normalized;
            String label = index < labels.size() && !labels.get(index).isBlank()
                    ? labels.get(index)
                    : rule.type == CustomSubtabRule.Type.STEM
                            ? labelFromPattern(normalized)
                            : labelFromFileName(pattern);
            candidates.add(new SubtabCandidate(slotId, label, fileName));
        }
        return List.copyOf(candidates);
    }

    private static @NotNull String displayNameForStem(@NotNull CustomSubtabRule rule, @NotNull String stem) {
        if (rule.stripComponentSuffix && stem.endsWith(".component") && stem.length() > ".component".length()) {
            return stem.substring(0, stem.length() - ".component".length());
        }
        return stem;
    }

    static @NotNull String projectViewDisplayName(@NotNull String base, @NotNull CustomSubtabRule rule) {
        String suffix = rule.groupSuffix == null ? "" : rule.groupSuffix.trim();
        if (suffix.isEmpty()) {
            return base;
        }
        if (suffix.startsWith("-") || suffix.startsWith("_")) {
            return base + suffix;
        }
        return base + "-" + suffix;
    }

    private static @NotNull List<String> parseCsv(@NotNull String raw) {
        if (raw.isBlank()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private static @NotNull String normalizeSuffix(@NotNull String pattern) {
        return pattern.startsWith(".") ? pattern : "." + pattern;
    }

    static @NotNull String labelFromPattern(@NotNull String pattern) {
        String normalized = pattern.startsWith(".") ? pattern.substring(1) : pattern;
        int lastDot = normalized.lastIndexOf('.');
        String core = lastDot > 0 ? normalized.substring(0, lastDot) : normalized;
        return capitalize(core);
    }

    static @NotNull String labelFromFileName(@NotNull String fileName) {
        if (fileName.startsWith(".")) {
            int nextDot = fileName.indexOf('.', 1);
            if (nextDot > 0) {
                return fileName.substring(1, nextDot);
            }
            return fileName.substring(1);
        }
        int dot = fileName.indexOf('.');
        if (dot > 0) {
            return capitalize(fileName.substring(0, dot));
        }
        return fileName;
    }

    private static @NotNull String capitalize(@NotNull String value) {
        if (value.isEmpty()) {
            return value;
        }
        if (value.length() == 1) {
            return value.toUpperCase();
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
