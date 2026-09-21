package de.sasbe.subtabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
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
        Project project = ProjectUtil.guessProjectForFile(currentFile);
        if (project != null && !project.isDisposed()) {
            return ComponentRelatedFilesCache.getInstance(project).get(currentFile);
        }
        return findUncached(currentFile);
    }

    static @Nullable Match findUncached(@NotNull VirtualFile currentFile) {
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
        if (!ComponentFileNaming.createsSubtabs(baseName)) {
            return null;
        }
        if (CustomSubtabRuleMatcher.isFolderGroupKey(baseName)) {
            return findFolderGroup(parent, baseName, rules);
        }
        if (CustomSubtabRuleMatcher.isUserGroupKey(baseName)) {
            return findUserGroup(parent, baseName);
        }

        if (CustomSubtabRuleMatcher.isExtensionFolderGroupKey(baseName)) {
            return findExtensionFolderGroup(currentFile, parent, baseName);
        }
        return findGroupNamePeers(currentFile, parent, baseName, rules);
    }

    private static @Nullable Match findGroupNamePeers(
            @NotNull VirtualFile currentFile,
            @NotNull VirtualFile parent,
            @NotNull String groupKey,
            @NotNull List<CustomSubtabRule> rules
    ) {
        CustomSubtabRuleMatcher.ParsedGroupKey parsed = CustomSubtabRuleMatcher.parseGroupKey(groupKey);
        if (parsed == null || parsed.ruleIndex() < 0 || parsed.ruleIndex() >= rules.size()) {
            return null;
        }

        CustomSubtabRule activeRule = rules.get(parsed.ruleIndex());
        Map<String, VirtualFile> foldersByPath = indexSearchFolders(parent, groupKey);
        List<Entry> relatedFiles = new ArrayList<>();
        Set<String> seenLocations = new LinkedHashSet<>();
        List<SubtabCandidateResolver.Located> locatedFiles = new ArrayList<>();

        for (Map.Entry<String, VirtualFile> folder : foldersByPath.entrySet()) {
            String directory = folder.getKey();
            for (VirtualFile child : folder.getValue().getChildren()) {
                if (child.isDirectory()) {
                    continue;
                }
                if (CustomSubtabRuleMatcher.isFileExcluded(child.getName(), activeRule)) {
                    continue;
                }

                CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.matchAtIndex(
                        child.getName(),
                        rules,
                        parsed.ruleIndex()
                );
                if (match == null) {
                    continue;
                }
                boolean sameGroup = ComponentFileNaming.searchNeighbors(groupKey)
                        ? CustomSubtabRuleMatcher.sameGroupIdentity(groupKey, match.groupKey())
                        : CustomSubtabRuleMatcher.sameFolderGroupIdentity(groupKey, match.groupKey());
                if (!sameGroup) {
                    continue;
                }

                String location = locationKey(directory, child.getName());
                if (!seenLocations.add(location)) {
                    continue;
                }

                relatedFiles.add(new Entry(
                        CustomSubtabRuleMatcher.resolveTabName(activeRule, child.getName()),
                        child
                ));
                locatedFiles.add(new SubtabCandidateResolver.Located(
                        "",
                        0,
                        directory,
                        child.getName()
                ));
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
        return new Match(anchor, groupKey, List.copyOf(relatedFiles));
    }

    private static @Nullable Match findFolderGroup(
            @NotNull VirtualFile parent,
            @NotNull String groupKey,
            @NotNull List<CustomSubtabRule> rules
    ) {
        CustomSubtabRuleMatcher.ParsedGroupKey parsed = CustomSubtabRuleMatcher.parseGroupKey(groupKey);
        CustomSubtabRule folderRule = parsed != null && parsed.ruleIndex() >= 0 && parsed.ruleIndex() < rules.size()
                ? rules.get(parsed.ruleIndex())
                : null;
        List<Entry> relatedFiles = new ArrayList<>();
        for (VirtualFile child : parent.getChildren()) {
            if (child.isDirectory()) {
                continue;
            }
            CustomSubtabRuleMatcher.Match match = CustomSubtabRuleMatcher.match(child.getName(), rules);
            if (match != null && groupKey.equals(match.groupKey())) {
                String label = folderRule != null
                        ? CustomSubtabRuleMatcher.resolveTabName(folderRule, child.getName())
                        : SubtabNameSegment.resolve(child.getName(), 1);
                relatedFiles.add(new Entry(label, child));
            }
        }

        sortByFileName(relatedFiles);
        if (relatedFiles.size() < 2) {
            return null;
        }
        return new Match(parent, groupKey, List.copyOf(relatedFiles));
    }

    private static @Nullable Match findUserGroup(
            @NotNull VirtualFile parent,
            @NotNull String groupKey
    ) {
        String parentFileName = SubtabFileNestingGroups.parentFileName(
                parsedGroupName(groupKey)
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
                        SubtabNameSegment.resolve(fileName, 1),
                        file
                ));
            }
        }
        if (relatedFiles.size() < 2) {
            return null;
        }
        return new Match(parent, groupKey, List.copyOf(relatedFiles));
    }

    private static @NotNull String parsedGroupName(@NotNull String groupKey) {
        CustomSubtabRuleMatcher.ParsedGroupKey parsed = CustomSubtabRuleMatcher.parseGroupKey(groupKey);
        return parsed == null ? "" : parsed.groupName();
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
            if (CustomSubtabRuleMatcher.isFileExcluded(child.getName(), spec.rule())) {
                continue;
            }
            relatedFiles.add(new Entry(
                    CustomSubtabRuleMatcher.resolveTabName(spec.rule(), child.getName()),
                    child
            ));
        }

        sortByFileName(relatedFiles);
        if (relatedFiles.size() < 2) {
            return null;
        }
        return new Match(parent, baseName, List.copyOf(relatedFiles));
    }

    private static final Comparator<Entry> FILE_NAME_ORDER =
            Comparator.comparing(entry -> entry.file().getName());

    private static void sortByFileName(@NotNull List<Entry> relatedFiles) {
        relatedFiles.sort(FILE_NAME_ORDER);
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

    private static @Nullable CustomSubtabRule ruleForGroupKey(
            @NotNull String groupKey,
            @NotNull List<CustomSubtabRule> rules
    ) {
        CustomSubtabRuleMatcher.ParsedGroupKey parsed = CustomSubtabRuleMatcher.parseGroupKey(groupKey);
        if (parsed == null || parsed.ruleIndex() < 0 || parsed.ruleIndex() >= rules.size()) {
            return null;
        }
        return rules.get(parsed.ruleIndex());
    }

}
