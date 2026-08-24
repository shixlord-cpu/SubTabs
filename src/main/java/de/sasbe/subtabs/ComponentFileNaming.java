package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class ComponentFileNaming {
    enum Kind {
        SOURCE("TS"),
        TEST("Test"),
        TEMPLATE("HTML"),
        STYLE("Style");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    record Candidate(@NotNull Kind kind, @NotNull String fileName) {
    }

    private static final List<String> RECOGNIZED_SUFFIXES = List.of(
            ".spec.ts",
            ".test.ts",
            ".ts",
            ".html",
            ".scss",
            ".sass",
            ".css",
            ".less"
    );

    private ComponentFileNaming() {
    }

    static @Nullable String componentBaseName(@NotNull String fileName) {
        for (String suffix : RECOGNIZED_SUFFIXES) {
            if (fileName.endsWith(suffix) && fileName.length() > suffix.length()) {
                return fileName.substring(0, fileName.length() - suffix.length());
            }
        }
        return null;
    }

    static @NotNull String displayName(@NotNull String baseName) {
        if (baseName.endsWith(".component") && baseName.length() > ".component".length()) {
            return baseName.substring(0, baseName.length() - ".component".length());
        }
        return baseName;
    }

    static @NotNull List<Candidate> candidates(@NotNull String baseName) {
        return List.of(
                new Candidate(Kind.SOURCE, baseName + ".ts"),
                new Candidate(Kind.TEST, baseName + ".spec.ts"),
                new Candidate(Kind.TEST, baseName + ".test.ts"),
                new Candidate(Kind.TEMPLATE, baseName + ".html"),
                new Candidate(Kind.STYLE, baseName + ".scss"),
                new Candidate(Kind.STYLE, baseName + ".sass"),
                new Candidate(Kind.STYLE, baseName + ".css"),
                new Candidate(Kind.STYLE, baseName + ".less")
        );
    }
}
