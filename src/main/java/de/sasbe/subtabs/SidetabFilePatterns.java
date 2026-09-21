package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

final class SidetabFilePatterns {
    private static final ConcurrentHashMap<String, List<String>> SPLIT_CACHE = new ConcurrentHashMap<>();

    private SidetabFilePatterns() {
    }

    static boolean matches(@NotNull String fileName, @NotNull String patterns) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        for (String pattern : splitCached(patterns)) {
            if (matchesOne(lowerName, pattern.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    static @NotNull List<String> splitCached(@NotNull String patterns) {
        return SPLIT_CACHE.computeIfAbsent(patterns, SidetabFilePatterns::split);
    }

    static @NotNull List<String> split(@NotNull String patterns) {
        List<String> result = new ArrayList<>();
        for (String raw : patterns.split(",")) {
            String pattern = raw.trim();
            if (!pattern.isEmpty()) {
                result.add(pattern);
            }
        }
        return result;
    }

    private static boolean matchesOne(@NotNull String lowerFileName, @NotNull String lowerPattern) {
        if (lowerPattern.startsWith("*.")) {
            return lowerFileName.endsWith(lowerPattern.substring(1));
        }
        if (lowerPattern.startsWith(".")) {
            return lowerFileName.endsWith(lowerPattern);
        }
        if (lowerPattern.contains("*")) {
            return glob(lowerFileName, lowerPattern);
        }
        return lowerFileName.equals(lowerPattern) || lowerFileName.endsWith("." + lowerPattern);
    }

    private static boolean glob(@NotNull String value, @NotNull String pattern) {
        StringBuilder regex = new StringBuilder();
        regex.append('^');
        for (int index = 0; index < pattern.length(); index++) {
            char character = pattern.charAt(index);
            if (character == '*') {
                regex.append(".*");
            } else if ("\\.[]{}()+-^$|?".indexOf(character) >= 0) {
                regex.append('\\').append(character);
            } else {
                regex.append(character);
            }
        }
        regex.append('$');
        return value.matches(regex.toString());
    }
}
