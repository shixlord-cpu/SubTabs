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
        String baseName = ComponentFileNaming.componentBaseName(currentFile.getName());
        if (parent == null || baseName == null) {
            return null;
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

        String anchorPath = SubtabCandidateResolver.commonDirectory(locatedFiles, pathKey(parent));
        VirtualFile anchor = foldersByPath.get(anchorPath);
        if (anchor == null) {
            anchor = parent;
        }
        return new Match(anchor, baseName, List.copyOf(relatedFiles));
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

        relatedFiles.sort(Comparator.comparing(entry -> entry.file().getName()));
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
