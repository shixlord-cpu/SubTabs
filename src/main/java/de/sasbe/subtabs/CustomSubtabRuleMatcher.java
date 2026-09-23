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
    private static final String FOLDER_GROUP_MARKER = "@folder";
    static final String NESTING_GROUP_MARKER = "@nesting:";
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

    record ParsedGroupKey(int ruleIndex, @NotNull String groupName, @NotNull String matchPrefix) {
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
        for (Match match : matchAll(fileName, rules)) {
            return match;
        }
        return null;
    }

    static int firstMatchingRuleIndex(@NotNull String fileName, @NotNull List<CustomSubtabRule> rules) {
        for (int index = 0; index < rules.size(); index++) {
            CustomSubtabRule rule = rules.get(index);
            if (!rule.enabled) {
                continue;
            }
            if (matchRule(fileName, rule, index) != null) {
                return index;
            }
        }
        return -1;
    }

    static @NotNull List<Match> matchAll(@NotNull String fileName, @NotNull List<CustomSubtabRule> rules) {
        List<Match> matches = new ArrayList<>();
        for (int index = 0; index < rules.size(); index++) {
            Match match = matchAtIndex(fileName, rules, index);
            if (match != null) {
                matches.add(match);
            }
        }
        return List.copyOf(matches);
    }

    static @Nullable Match matchAtIndex(
            @NotNull String fileName,
            @NotNull List<CustomSubtabRule> rules,
            int index
    ) {
        if (index < 0 || index >= rules.size()) {
            return null;
        }
        CustomSubtabRule rule = rules.get(index);
        if (!rule.enabled) {
            return null;
        }
        return matchRule(fileName, rule, index);
    }

    static boolean matchesRuleIndex(
            @NotNull String fileName,
            @NotNull List<CustomSubtabRule> rules,
            int index
    ) {
        return matchAtIndex(fileName, rules, index) != null;
    }

    static @Nullable ParsedGroupKey parseGroupKey(@NotNull String groupKey) {
        Matcher matcher = GROUP_KEY.matcher(groupKey);
        if (!matcher.matches()) {
            return null;
        }
        return parseGroupKeySuffix(Integer.parseInt(matcher.group(1)), matcher.group(2));
    }

    private static @NotNull ParsedGroupKey parseGroupKeySuffix(int ruleIndex, @NotNull String suffix) {
        int separator = suffix.indexOf('#');
        if (separator < 0) {
            return new ParsedGroupKey(ruleIndex, suffix, suffix);
        }
        return new ParsedGroupKey(
                ruleIndex,
                suffix.substring(0, separator),
                suffix.substring(separator + 1)
        );
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
        if (rule.isSpecial()) {
            return switch (rule.type) {
                case USER_GROUPS -> buildUserGroupMatch(rule, parsed.ruleIndex(), parsed.matchPrefix());
                case FOLDER -> buildFolderMatch(rule, parsed.ruleIndex());
                default -> null;
            };
        }

        List<String> patterns = parseCsv(rule.patterns);
        if (isExactNameOnlyRule(patterns)) {
            return buildExactNameMatch(rule, parsed.ruleIndex(), parsed.groupName());
        }
        return buildSuffixPatternMatch(
                rule,
                parsed.ruleIndex(),
                parsed.matchPrefix(),
                fileNameForParsedGroup(rule, parsed)
        );
    }

    static boolean isExtensionFolderGroupKey(@NotNull String groupKey) {
        return EXTENSION_GROUP_KEY.matcher(groupKey).matches();
    }

    static boolean isFolderGroupKey(@NotNull String groupKey) {
        ParsedGroupKey parsed = parseGroupKey(groupKey);
        return parsed != null && FOLDER_GROUP_MARKER.equals(parsed.matchPrefix());
    }

    static boolean isUserGroupKey(@NotNull String groupKey) {
        ParsedGroupKey parsed = parseGroupKey(groupKey);
        return parsed != null && parsed.matchPrefix().startsWith(NESTING_GROUP_MARKER);
    }

    static boolean usesSuffixMatching(@NotNull String pattern) {
        return pattern.startsWith(".") && !isStandaloneDotFile(pattern);
    }

    private static @Nullable Match matchRule(
            @NotNull String fileName,
            @NotNull CustomSubtabRule rule,
            int index
    ) {
        if (rule.isSpecial()) {
            return switch (rule.type) {
                case USER_GROUPS -> matchUserGroups(fileName, rule, index);
                case FOLDER -> buildFolderMatch(rule, index);
                default -> null;
            };
        }
        return matchPatternRule(fileName, rule, index);
    }

    private static @Nullable Match matchUserGroups(
            @NotNull String fileName,
            @NotNull CustomSubtabRule rule,
            int index
    ) {
        SubtabFileNestingGroups.Group group = SubtabFileNestingGroups.findGroup(fileName);
        if (group == null) {
            return null;
        }
        return buildUserGroupMatch(rule, index, group.groupStem());
    }

    private static @NotNull Match buildUserGroupMatch(
            @NotNull CustomSubtabRule rule,
            int index,
            @NotNull String nestingKey
    ) {
        String parentFileName = SubtabFileNestingGroups.parentFileName(nestingKey);
        String displayName = parentFileName != null && !parentFileName.isBlank()
                ? parentFileName
                : (!rule.name.isBlank() ? rule.name : "Eigene Gruppen");
        return new Match(
                groupKey(index, nestingKey),
                displayName,
                List.of(),
                false
        );
    }

    private static @Nullable Match matchPatternRule(
            @NotNull String fileName,
            @NotNull CustomSubtabRule rule,
            int index
    ) {
        if (isFileExcluded(fileName, rule)) {
            return null;
        }

        List<String> patterns = parseCsv(rule.patterns);
        if (patterns.isEmpty()) {
            return null;
        }

        if (isExtensionFolderRule(patterns)) {
            return matchExtensionFolder(fileName, rule, index, patterns);
        }

        Match suffixMatch = matchSuffixPattern(fileName, rule, index, patterns);
        if (suffixMatch != null) {
            return suffixMatch;
        }

        if (matchesExactFilePattern(fileName, patterns)) {
            return buildExactNameMatch(rule, index, fileName);
        }
        return null;
    }

    private static @NotNull Match buildFolderMatch(@NotNull CustomSubtabRule rule, int index) {
        String displayName = !rule.name.isBlank() ? rule.name : "Ordner";
        return new Match(
                groupKey(index, FOLDER_GROUP_MARKER),
                displayName,
                List.of(),
                false
        );
    }

    private static @Nullable Match matchSuffixPattern(
            @NotNull String fileName,
            @NotNull CustomSubtabRule rule,
            int index,
            @NotNull List<String> patterns
    ) {
        List<String> suffixes = patterns.stream()
                .filter(CustomSubtabRuleMatcher::usesSuffixMatching)
                .map(CustomSubtabRuleMatcher::normalizeSuffix)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();

        for (String suffix : suffixes) {
            if (!fileName.endsWith(suffix) || fileName.length() <= suffix.length()) {
                continue;
            }

            String matchPrefix = fileName.substring(0, fileName.length() - suffix.length());
            if (matchPrefix.isEmpty()) {
                continue;
            }
            return buildSuffixPatternMatch(rule, index, matchPrefix, fileName);
        }
        return null;
    }

    private static @NotNull Match buildSuffixPatternMatch(
            @NotNull CustomSubtabRule rule,
            int index,
            @NotNull String matchPrefix,
            @NotNull String fileName
    ) {
        String groupName = resolveGroupName(rule, fileName);
        return new Match(
                groupKey(index, encodeGroupKeySuffix(groupName, matchPrefix)),
                groupName,
                buildPatternSlots(rule, matchPrefix),
                rule.searchNeighbors
        );
    }

    private static @NotNull String probeFileNameForPrefix(
            @NotNull CustomSubtabRule rule,
            @NotNull String matchPrefix
    ) {
        List<String> patterns = parseCsv(rule.patterns);
        for (String pattern : patterns) {
            if (usesSuffixMatching(pattern)) {
                return matchPrefix + normalizeSuffix(pattern);
            }
        }
        return matchPrefix;
    }

    private static @NotNull String fileNameForParsedGroup(
            @NotNull CustomSubtabRule rule,
            @NotNull ParsedGroupKey parsed
    ) {
        if (parsed.groupName().equals(parsed.matchPrefix())) {
            return probeFileNameForPrefix(rule, parsed.matchPrefix());
        }

        List<String> patterns = parseCsv(rule.patterns);
        for (String pattern : patterns) {
            if (!usesSuffixMatching(pattern)) {
                continue;
            }
            String fileName = parsed.matchPrefix() + normalizeSuffix(pattern);
            if (parsed.groupName().equals(resolveGroupName(rule, fileName))) {
                return fileName;
            }
        }
        return probeFileNameForPrefix(rule, parsed.matchPrefix());
    }

    private static @NotNull String encodeGroupKeySuffix(@NotNull String groupName, @NotNull String matchPrefix) {
        return groupName.equals(matchPrefix) ? groupName : groupName + "#" + matchPrefix;
    }

    private static @Nullable Match matchExtensionFolder(
            @NotNull String fileName,
            @NotNull CustomSubtabRule rule,
            int index,
            @NotNull List<String> patterns
    ) {
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

    private static boolean matchesExactFilePattern(
            @NotNull String fileName,
            @NotNull List<String> patterns
    ) {
        return patterns.contains(fileName);
    }

    private static boolean isExactNameOnlyRule(@NotNull List<String> patterns) {
        return !patterns.isEmpty() && patterns.stream().noneMatch(CustomSubtabRuleMatcher::usesSuffixMatching);
    }

    private static @NotNull Match buildExactNameMatch(
            @NotNull CustomSubtabRule rule,
            int index,
            @NotNull String fileName
    ) {
        String groupName = resolveGroupName(rule, fileName);
        return new Match(
                groupKey(index, groupName),
                groupName,
                buildPatternSlots(rule, null),
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

    static boolean sameFolderGroupIdentity(@NotNull String leftGroupKey, @NotNull String rightGroupKey) {
        if (leftGroupKey.equals(rightGroupKey)) {
            return true;
        }

        ParsedGroupKey left = parseGroupKey(leftGroupKey);
        ParsedGroupKey right = parseGroupKey(rightGroupKey);
        if (left == null || right == null || left.ruleIndex() != right.ruleIndex()) {
            return false;
        }
        return left.groupName().equals(right.groupName());
    }

    private static @NotNull String groupKey(int index, @NotNull String suffix) {
        return GROUP_PREFIX + index + ":" + suffix;
    }

    private static boolean isExtensionFolderRule(@NotNull List<String> patterns) {
        return patterns.size() == 1 && isExtensionOnlyPattern(patterns.get(0));
    }

    private static boolean isExtensionOnlyPattern(@NotNull String pattern) {
        if (!pattern.startsWith(".")) {
            return false;
        }
        return pattern.indexOf('.', 1) < 0;
    }

    static boolean isFileExcluded(@NotNull String fileName, @NotNull CustomSubtabRule rule) {
        if (rule.excludePatterns == null || rule.excludePatterns.isBlank()) {
            return false;
        }
        for (String pattern : parseCsv(rule.excludePatterns)) {
            if (matchesExcludePattern(fileName, pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesExcludePattern(@NotNull String fileName, @NotNull String pattern) {
        if (pattern.startsWith(".")) {
            String normalized = normalizeSuffix(pattern);
            return fileName.endsWith(normalized) && fileName.length() > normalized.length();
        }
        return fileName.equals(pattern);
    }

    static @NotNull String resolveTabName(@NotNull CustomSubtabRule rule, @NotNull String fileName) {
        if (rule.isSpecial()) {
            return SubtabNameSegment.resolve(fileName, defaultNameSegment(rule));
        }
        int patternIndex = matchingPatternIndex(rule, fileName);
        return SubtabNameSegment.resolve(fileName, nameSegmentAt(rule, patternIndex));
    }

    private static int defaultNameSegment(@NotNull CustomSubtabRule rule) {
        List<Integer> segments = parseNameSegments(rule.nameSegments);
        return segments.isEmpty() ? 1 : segments.get(0);
    }

    private static int nameSegmentAt(@NotNull CustomSubtabRule rule, int patternIndex) {
        List<String> patterns = parseCsv(rule.patterns);
        int fallback = 1;
        if (patternIndex >= 0 && patternIndex < patterns.size()) {
            fallback = usesSuffixMatching(patterns.get(patternIndex)) ? 2 : 1;
        }
        return segmentAt(rule.nameSegments, patternIndex, fallback);
    }

    static int groupSegmentAt(@NotNull CustomSubtabRule rule, int patternIndex) {
        return segmentAt(rule.groupNameSegments, rule, patternIndex, 1);
    }

    static @NotNull String resolveGroupName(@NotNull CustomSubtabRule rule, @NotNull String fileName) {
        List<String> patterns = parseCsv(rule.patterns);
        if (isExactNameOnlyRule(patterns) && !rule.name.isBlank()) {
            return rule.name.trim();
        }
        int patternIndex = matchingPatternIndex(rule, fileName);
        return SubtabNameSegment.resolve(fileName, groupSegmentAt(rule, patternIndex));
    }

    private static boolean isStandaloneDotFile(@NotNull String pattern) {
        if (pattern.startsWith(".env")) {
            return true;
        }
        return pattern.equals(".npmrc") || pattern.equals(".nvmrc") || pattern.equals(".node-version");
    }

    private static int segmentAt(
            @NotNull String raw,
            int patternIndex,
            int fallback
    ) {
        List<Integer> segments = parseNameSegments(raw);
        if (segments.isEmpty()) {
            return fallback;
        }
        if (segments.size() == 1) {
            return segments.get(0);
        }
        if (patternIndex >= 0 && patternIndex < segments.size()) {
            return segments.get(patternIndex);
        }
        return fallback;
    }

    private static int segmentAt(
            @NotNull String raw,
            @NotNull CustomSubtabRule rule,
            int patternIndex,
            int fallback
    ) {
        return segmentAt(raw, patternIndex, fallback);
    }

    static int matchingPatternIndex(@NotNull CustomSubtabRule rule, @NotNull String fileName) {
        List<String> patterns = parseCsv(rule.patterns);
        if (patterns.isEmpty()) {
            return 0;
        }

        if (isExtensionFolderRule(patterns)) {
            List<String> extensions = patterns.stream()
                    .map(CustomSubtabRuleMatcher::normalizeSuffix)
                    .toList();
            for (int index = 0; index < extensions.size(); index++) {
                String extension = extensions.get(index);
                if (fileName.endsWith(extension) && fileName.length() >= extension.length()) {
                    return index;
                }
            }
            return 0;
        }

        List<String> suffixes = patterns.stream()
                .filter(CustomSubtabRuleMatcher::usesSuffixMatching)
                .map(CustomSubtabRuleMatcher::normalizeSuffix)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
        for (String suffix : suffixes) {
            if (!fileName.endsWith(suffix) || fileName.length() <= suffix.length()) {
                continue;
            }
            for (int index = 0; index < patterns.size(); index++) {
                if (usesSuffixMatching(patterns.get(index))
                        && normalizeSuffix(patterns.get(index)).equals(suffix)) {
                    return index;
                }
            }
        }

        for (int index = 0; index < patterns.size(); index++) {
            if (fileName.equals(patterns.get(index))) {
                return index;
            }
        }
        return 0;
    }

    private static @NotNull List<SubtabCandidate> buildPatternSlots(
            @NotNull CustomSubtabRule rule,
            @Nullable String matchPrefix
    ) {
        List<String> patterns = parseCsv(rule.patterns);
        List<String> slotKeys = parseCsv(rule.slotKeys);
        List<SubtabCandidate> candidates = new ArrayList<>();

        for (int index = 0; index < patterns.size(); index++) {
            String pattern = patterns.get(index);
            String fileName;
            if (usesSuffixMatching(pattern)) {
                if (matchPrefix == null || matchPrefix.isBlank()) {
                    continue;
                }
                fileName = matchPrefix + normalizeSuffix(pattern);
            } else {
                fileName = pattern;
            }
            String slotId = index < slotKeys.size() && !slotKeys.get(index).isBlank()
                    ? slotKeys.get(index)
                    : (usesSuffixMatching(pattern) ? normalizeSuffix(pattern) : pattern);
            candidates.add(new SubtabCandidate(slotId, nameSegmentAt(rule, index), fileName));
        }
        return List.copyOf(candidates);
    }

    static @NotNull String displayNameWithSuffix(@NotNull String base, @NotNull CustomSubtabRule rule) {
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

    static @NotNull List<Integer> parseNameSegments(@NotNull String raw) {
        if (raw.isBlank()) {
            return List.of();
        }

        List<Integer> values = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                values.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ignored) {
            }
        }
        return List.copyOf(values);
    }
}
