package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ComponentRelatedFiles {
    record Entry(
            @NotNull String label,
            @NotNull VirtualFile file
    ) {
    }

    record Match(
            @NotNull VirtualFile anchor,
            @NotNull String baseName,
            @NotNull List<Entry> relatedFiles
    ) {
        @NotNull String key() {
            return ComponentSubtabGroupRegistry.componentKey(anchor, baseName);
        }
    }

    private ComponentRelatedFiles() {
    }

    static @Nullable Match find(@NotNull VirtualFile currentFile) {
        VirtualFile parent = currentFile.getParent();
        if (parent == null) {
            return null;
        }

        List<CustomSubtabRule> rules = ComponentFileNaming.rules();
        CustomSubtabRuleMatcher.Match ruleMatch = CustomSubtabRuleMatcher.match(
                currentFile.getName(),
                rules
        );
        if (ruleMatch == null) {
            return null;
        }

        String baseName = ruleMatch.groupKey();
        if (CustomSubtabRuleMatcher.isFolderGroupKey(baseName)) {
            return findFolderGroup(parent, baseName, rules);
        }
        if (CustomSubtabRuleMatcher.isUserGroupKey(baseName)) {
            return findUserGroup(parent, baseName);
        }

        if (CustomSubtabRuleMatcher.isExtensionFolderGroupKey(baseName)) {
            return findExtensionFolderGroup(currentFile, parent, baseName);
        }

        Map<String, VirtualFile> foldersByPath = indexSearchFolders(parent, baseName);
        Map<String, Set<String>> filesByDirectory = new LinkedHashMap<>();
        Map<String, VirtualFile> filesByLocation = new LinkedHashMap<>();

        for (Map.Entry<String, VirtualFile> folder : foldersByPath.entrySet()) {
            Set<String> names = new LinkedHashSet<>();
            for (VirtualFile child : folder.getValue().getChildren()) {
                if (child.isDirectory()) {
                    continue;
                }
                names.add(child.getName());
                filesByLocation.put(locationKey(folder.getKey(), child.getName()), child);
            }
            filesByDirectory.put(folder.getKey(), names);
        }

        List<SubtabCandidateResolver.Located> locatedFiles = SubtabCandidateResolver.resolve(
                baseName,
                pathKey(parent),
                currentFile.getName(),
                filesByDirectory
        );
        if (locatedFiles.size() < 2) {
            return null;
        }

        List<Entry> relatedFiles = new ArrayList<>();
        for (SubtabCandidateResolver.Located located : locatedFiles) {
            VirtualFile file = filesByLocation.get(locationKey(located.directory(), located.fileName()));
            if (file != null) {
                String label = "Style".equalsIgnoreCase(located.label())
                        ? new SubtabCandidate(located.slotId(), located.label(), located.fileName())
                                .displayLabel(located.fileName())
                        : located.label();
                relatedFiles.add(new Entry(label, file));
            }
        }
        if (relatedFiles.size() < 2) {
            return null;
        }

        sortByFileName(relatedFiles);

        String anchorPath = SubtabCandidateResolver.commonDirectory(locatedFiles, pathKey(parent));
        VirtualFile anchor = foldersByPath.get(anchorPath);
        if (anchor == null) {
            anchor = parent;
        }
        return new Match(anchor, baseName, List.copyOf(relatedFiles));
    }

    private static @Nullable Match findFolderGroup(
            @NotNull VirtualFile parent,
            @NotNull String groupKey,
            @NotNull List<CustomSubtabRule> rules
    ) {
        List<Entry> relatedFiles = new ArrayList<>();
        for (VirtualFile child : parent.getChildren()) {
            if (child.isDirectory()) {
                continue;
            }
            CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.match(child.getName(), rules);
            if (match != null && groupKey.equals(match.groupKey())) {
                relatedFiles.add(new Entry(
                        CustomSubtabRuleMatcher.labelFromFileName(child.getName()),
                        child
                ));
            }
        }

        relatedFiles.sort(FILE_NAME_ORDER);
        if (relatedFiles.size() < 2) {
            return null;
        }
        return new Match(parent, groupKey, List.copyOf(relatedFiles));
    }

    private static final Comparator<Entry> FILE_NAME_ORDER =
            Comparator.comparing(entry -> entry.file().getName());

    private static void sortByFileName(@NotNull List<Entry> relatedFiles) {
        relatedFiles.sort(FILE_NAME_ORDER);
    }

    private static @Nullable Match findUserGroup(
            @NotNull VirtualFile parent,
            @NotNull String groupKey
    ) {
        String parentFileName = SubtabFileNestingGroups.parentFileName(
                parsedStem(groupKey)
        );
        if (parentFileName == null || parentFileName.isBlank()) {
            return null;
        }

        List<String> siblingNames = new ArrayList<>();
        for (VirtualFile child : parent.getChildren()) {
            if (!child.isDirectory()) {
                siblingNames.add(child.getName());
            }
        }

        List<String> memberNames = SubtabFileNestingGroups.groupFileNames(parentFileName, siblingNames);
        if (memberNames.size() < 2) {
            return null;
        }

        List<Entry> relatedFiles = new ArrayList<>();
        for (String fileName : memberNames) {
            VirtualFile file = parent.findChild(fileName);
            if (file != null && !file.isDirectory()) {
                relatedFiles.add(new Entry(
                        CustomSubtabRuleMatcher.labelFromFileName(fileName),
                        file
                ));
            }
        }
        if (relatedFiles.size() < 2) {
            return null;
        }
        sortByFileName(relatedFiles);
        return new Match(parent, groupKey, List.copyOf(relatedFiles));
    }

    private static @NotNull String parsedStem(@NotNull String groupKey) {
        CustomSubtabRuleMatcher.ParsedGroupKey parsed = CustomSubtabRuleMatcher.parseGroupKey(groupKey);
        return parsed == null ? "" : parsed.stem();
    }

    private static @Nullable Match findExtensionFolderGroup(
            @NotNull VirtualFile currentFile,
            @NotNull VirtualFile parent,
            @NotNull String baseName
    ) {
        CustomSubtabRuleMatcher.ExtensionGroupSpec spec = CustomSubtabRuleMatcher.parseExtensionGroup(
                baseName,
                ComponentFileNaming.rules()
        );
        if (spec == null || !spec.matches(currentFile.getName())) {
            return null;
        }

        List<Entry> relatedFiles = new ArrayList<>();
        for (VirtualFile child : parent.getChildren()) {
            if (child.isDirectory() || !spec.matches(child.getName())) {
                continue;
            }
            relatedFiles.add(new Entry(
                    CustomSubtabRuleMatcher.labelFromFileName(child.getName()),
                    child
            ));
        }

        sortByFileName(relatedFiles);
        if (relatedFiles.size() < 2) {
            return null;
        }
        return new Match(parent, baseName, List.copyOf(relatedFiles));
    }

    private static @NotNull Map<String, VirtualFile> indexSearchFolders(
            @NotNull VirtualFile parent,
            @NotNull String baseName
    ) {
        Map<String, VirtualFile> folders = new LinkedHashMap<>();
        folders.put(pathKey(parent), parent);
        if (!ComponentFileNaming.searchNeighbors(baseName)) {
            return folders;
        }

        for (VirtualFile child : parent.getChildren()) {
            if (child.isDirectory()) {
                folders.put(pathKey(child), child);
            }
        }

        VirtualFile grandparent = parent.getParent();
        if (grandparent != null) {
            folders.put(pathKey(grandparent), grandparent);
            for (VirtualFile sibling : grandparent.getChildren()) {
                if (sibling.isDirectory()) {
                    folders.put(pathKey(sibling), sibling);
                }
            }
        }
        return folders;
    }

    private static @NotNull String pathKey(@NotNull VirtualFile file) {
        return file.getPath().replace('\\', '/');
    }

    private static @NotNull String locationKey(@NotNull String directory, @NotNull String fileName) {
        return directory + "/" + fileName;
    }
}
