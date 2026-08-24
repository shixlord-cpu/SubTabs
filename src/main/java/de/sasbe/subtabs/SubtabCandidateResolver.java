package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SubtabCandidateResolver {
    record Located(
            @NotNull String slotId,
            @NotNull String label,
            @NotNull String directory,
            @NotNull String fileName
    ) {
    }

    private SubtabCandidateResolver() {
    }

    static @NotNull List<Located> resolve(
            @NotNull String baseName,
            @NotNull String currentDirectory,
            @NotNull String currentFileName,
            @NotNull Map<String, ? extends Set<String>> filesByDirectory
    ) {
        List<String> directories = directoriesToSearch(currentDirectory, filesByDirectory.keySet(), baseName);
        Map<String, Located> filesBySlot = new LinkedHashMap<>();

        for (SubtabCandidate candidate : ComponentFileNaming.candidates(baseName)) {
            Located located = findCandidate(
                    candidate,
                    currentDirectory,
                    currentFileName,
                    directories,
                    filesByDirectory
            );
            if (located == null) {
                continue;
            }
            if (located.fileName().equals(currentFileName) && located.directory().equals(currentDirectory)) {
                filesBySlot.put(candidate.slotId(), located);
            } else {
                filesBySlot.putIfAbsent(candidate.slotId(), located);
            }
        }

        List<Located> result = new ArrayList<>();
        LinkedHashSet<String> addedSlots = new LinkedHashSet<>();
        for (SubtabCandidate candidate : ComponentFileNaming.candidates(baseName)) {
            if (!addedSlots.add(candidate.slotId())) {
                continue;
            }
            Located located = filesBySlot.get(candidate.slotId());
            if (located != null) {
                result.add(located);
            }
        }
        return result;
    }

    static @NotNull String commonDirectory(
            @NotNull List<Located> locatedFiles,
            @NotNull String fallbackDirectory
    ) {
        String common = null;
        for (Located located : locatedFiles) {
            if (common == null) {
                common = located.directory();
            } else if (!common.equals(located.directory())) {
                return parentDirectory(common, fallbackDirectory);
            }
        }
        return common != null ? common : fallbackDirectory;
    }

    private static @Nullable Located findCandidate(
            @NotNull SubtabCandidate candidate,
            @NotNull String currentDirectory,
            @NotNull String currentFileName,
            @NotNull List<String> directories,
            @NotNull Map<String, ? extends Set<String>> filesByDirectory
    ) {
        Located currentMatch = null;
        Located sameFolderMatch = null;
        Located otherMatch = null;

        for (String directory : directories) {
            Set<String> files = filesByDirectory.get(directory);
            if (files == null || !files.contains(candidate.fileName())) {
                continue;
            }

            Located located = new Located(
                    candidate.slotId(),
                    candidate.label(),
                    directory,
                    candidate.fileName()
            );
            if (directory.equals(currentDirectory) && candidate.fileName().equals(currentFileName)) {
                currentMatch = located;
            } else if (directory.equals(currentDirectory)) {
                sameFolderMatch = located;
            } else if (otherMatch == null) {
                otherMatch = located;
            }
        }

        if (currentMatch != null) {
            return currentMatch;
        }
        if (sameFolderMatch != null) {
            return sameFolderMatch;
        }
        return otherMatch;
    }

    private static @NotNull List<String> directoriesToSearch(
            @NotNull String currentDirectory,
            @NotNull Set<String> availableDirectories,
            @NotNull String baseName
    ) {
        if (!ComponentFileNaming.searchNeighbors(baseName)) {
            return List.of(currentDirectory);
        }

        LinkedHashSet<String> directories = new LinkedHashSet<>();
        directories.add(currentDirectory);
        for (String directory : availableDirectories) {
            if (directory.equals(currentDirectory)
                    || isChildDirectory(currentDirectory, directory)
                    || isSiblingOrParentDirectory(currentDirectory, directory)) {
                directories.add(directory);
            }
        }
        return List.copyOf(directories);
    }

    private static boolean isChildDirectory(@NotNull String parent, @NotNull String maybeChild) {
        String prefix = parent.endsWith("/") ? parent : parent + "/";
        return maybeChild.startsWith(prefix) && maybeChild.indexOf('/', prefix.length()) < 0;
    }

    private static boolean isSiblingOrParentDirectory(@NotNull String directory, @NotNull String other) {
        String parent = parentDirectory(directory, "");
        if (parent.isEmpty()) {
            return false;
        }
        return other.equals(parent) || isChildDirectory(parent, other);
    }

    private static @NotNull String parentDirectory(@NotNull String directory, @NotNull String fallback) {
        int separator = directory.lastIndexOf('/');
        if (separator <= 0) {
            return fallback;
        }
        return directory.substring(0, separator);
    }
}
