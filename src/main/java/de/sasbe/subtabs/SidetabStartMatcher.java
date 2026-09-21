package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SidetabStartMatcher {
    private static final ConcurrentHashMap<String, Pattern> TAG_OPEN_PATTERNS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Pattern> TAG_CLOSE_PATTERNS = new ConcurrentHashMap<>();
    private static final Pattern CLASS_DECLARATION = Pattern.compile(
            "(?m)^\\s*(?:(?:public|private|protected|internal|export|default|abstract"
                    + "|final|sealed|open|partial|static|data)\\s+)*class\\s+\\w+"
    );
    private static final Pattern CLASS_METHOD = Pattern.compile(
            "(?m)^[ \\t]+(?:(?:public|private|protected|readonly|async|static|override|abstract)\\s+)*"
                    + "(?:(?:get|set)\\s+)?(?:constructor|[A-Za-z_$][\\w$]*)\\s*(?:<[^\\n>]*>)?\\s*\\("
    );

    private SidetabStartMatcher() {
    }

    static boolean isFileStart(@NotNull String expression) {
        String trimmed = expression.trim();
        return trimmed.isEmpty() || trimmed.equalsIgnoreCase("@start");
    }

    static int find(@NotNull String text, @NotNull String expression, int from) {
        if (from > text.length()) {
            return -1;
        }
        if (isFileStart(expression)) {
            return Math.max(0, from);
        }
        int best = -1;
        for (String rawAlternative : expression.split("\\|\\|")) {
            int found = evaluate(text, rawAlternative.trim(), from);
            if (found >= from && (best < 0 || found < best)) {
                best = found;
            }
        }
        return best;
    }

    private static int evaluate(@NotNull String text, @NotNull String alternative, int from) {
        if (alternative.isEmpty() || alternative.equalsIgnoreCase("@start")) {
            return Math.max(0, from);
        }
        String[] parts = alternative.split("\\s+", 2);
        String command = parts[0];
        String argument = parts.length > 1 ? parts[1] : "";
        if (!command.startsWith("@")) {
            return indexOfIgnoreCase(text, alternative, from);
        }
        return switch (command.toLowerCase(Locale.ROOT)) {
            case "@tag" -> findOpeningTag(text, argument.trim(), from);
            case "@close-tag" -> findClosingTag(text, argument.trim(), from);
            case "@after-tag" -> afterClosingTag(text, argument.trim(), from);
            case "@text" -> indexOfIgnoreCase(text, argument, from);
            case "@after" -> after(text, argument, from);
            case "@regex" -> findRegex(text, argument, from);
            case "@media" -> findRegex(text, "@media\\b", from);
            case "@class-open" -> classOpen(text, from, false);
            case "@after-class-open" -> classOpen(text, from, true);
            case "@first-method" -> firstClassMethod(text, from);
            case "@eof" -> text.length();
            default -> indexOfIgnoreCase(text, alternative, from);
        };
    }

    static int resolveEndOffset(@NotNull String text, int matchStart, boolean includeMarker) {
        if (matchStart < 0) {
            return -1;
        }
        if (!includeMarker) {
            return matchStart;
        }
        if (matchStart + 1 < text.length()
                && text.charAt(matchStart) == '<'
                && text.charAt(matchStart + 1) == '/') {
            int close = text.indexOf('>', matchStart);
            return close < 0 ? text.length() : close + 1;
        }
        int lineEnd = text.indexOf('\n', matchStart);
        return lineEnd < 0 ? text.length() : lineEnd + 1;
    }

    private static int after(@NotNull String text, @NotNull String needle, int from) {
        int index = indexOfIgnoreCase(text, needle, from);
        return index < 0 ? -1 : index + needle.length();
    }

    private static int findOpeningTag(@NotNull String text, @NotNull String tagName, int from) {
        if (tagName.isEmpty()) {
            return -1;
        }
        Pattern pattern = TAG_OPEN_PATTERNS.computeIfAbsent(tagName, name -> Pattern.compile(
                "<" + Pattern.quote(name) + "(?:\\s|>|/)",
                Pattern.CASE_INSENSITIVE
        ));
        Matcher matcher = pattern.matcher(text);
        if (from > 0 && from < text.length()) {
            matcher.region(from, text.length());
        } else if (from >= text.length()) {
            return -1;
        }
        return matcher.find() ? matcher.start() : -1;
    }

    private static int findClosingTag(@NotNull String text, @NotNull String tagName, int from) {
        if (tagName.isEmpty()) {
            return -1;
        }
        Pattern pattern = TAG_CLOSE_PATTERNS.computeIfAbsent(tagName, name -> Pattern.compile(
                "</" + Pattern.quote(name) + "\\s*>",
                Pattern.CASE_INSENSITIVE
        ));
        Matcher matcher = pattern.matcher(text);
        if (from > 0 && from < text.length()) {
            matcher.region(from, text.length());
        } else if (from >= text.length()) {
            return -1;
        }
        return matcher.find() ? matcher.start() : -1;
    }

    private static int afterClosingTag(@NotNull String text, @NotNull String tagName, int from) {
        int start = findClosingTag(text, tagName, from);
        if (start < 0) {
            return -1;
        }
        Pattern pattern = TAG_CLOSE_PATTERNS.computeIfAbsent(tagName, name -> Pattern.compile(
                "</" + Pattern.quote(name) + "\\s*>",
                Pattern.CASE_INSENSITIVE
        ));
        Matcher matcher = pattern.matcher(text);
        matcher.region(start, text.length());
        return matcher.find() ? matcher.end() : -1;
    }

    private static int classOpen(@NotNull String text, int from, boolean afterBrace) {
        Matcher matcher = CLASS_DECLARATION.matcher(text);
        while (matcher.find()) {
            int brace = text.indexOf('{', matcher.end());
            if (brace < 0) {
                return -1;
            }
            int result = afterBrace ? brace + 1 : brace;
            if (result >= from) {
                return result;
            }
        }
        return -1;
    }

    private static int firstClassMethod(@NotNull String text, int from) {
        int classOpen = classOpen(text, 0, false);
        if (classOpen < 0) {
            return -1;
        }
        int classClose = matchingBrace(text, classOpen);
        int limit = classClose >= 0 ? classClose : text.length();
        int searchFrom = Math.max(from, classOpen + 1);
        Matcher matcher = CLASS_METHOD.matcher(text);
        if (searchFrom >= limit) {
            return -1;
        }
        matcher.region(searchFrom, limit);
        while (matcher.find()) {
            if (braceDepth(text, classOpen, matcher.start()) == 1) {
                return matcher.start();
            }
        }
        return -1;
    }

    private static int matchingBrace(@NotNull String text, int openIndex) {
        int depth = 0;
        for (int index = openIndex; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static int braceDepth(@NotNull String text, int from, int to) {
        int depth = 0;
        for (int index = from; index < to && index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
            }
        }
        return depth;
    }

    private static int findRegex(@NotNull String text, @NotNull String regex, int from) {
        if (regex.isBlank()) {
            return -1;
        }
        try {
            Matcher matcher = Pattern.compile(regex, Pattern.MULTILINE).matcher(text);
            if (from > 0 && from < text.length()) {
                matcher.region(from, text.length());
            } else if (from >= text.length()) {
                return -1;
            }
            return matcher.find() ? matcher.start() : -1;
        } catch (RuntimeException ignored) {
            return -1;
        }
    }

    private static int indexOfIgnoreCase(@NotNull String text, @NotNull String needle, int from) {
        if (needle.isEmpty()) {
            return from;
        }
        int max = text.length() - needle.length();
        for (int index = Math.max(0, from); index <= max; index++) {
            if (text.regionMatches(true, index, needle, 0, needle.length())) {
                return index;
            }
        }
        return -1;
    }
}
