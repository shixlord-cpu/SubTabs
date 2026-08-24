package de.sasbe.subtabs;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ComponentRelatedFiles {
    record Entry(
            @NotNull ComponentFileNaming.Kind kind,
            @NotNull VirtualFile file
    ) {
        @NotNull String label() {
            if (kind != ComponentFileNaming.Kind.STYLE) {
                return kind.label();
            }

            String extension = file.getExtension();
            return extension == null ? kind.label() : extension.toUpperCase();
        }
    }

    private ComponentRelatedFiles() {
    }

    static @Nullable List<Entry> find(
            @NotNull VirtualFile parent,
            @NotNull String baseName,
            @NotNull VirtualFile currentFile
    ) {
        Map<String, VirtualFile> childrenByName = indexChildren(parent);
        Map<ComponentFileNaming.Kind, Entry> filesByKind =
                new EnumMap<>(ComponentFileNaming.Kind.class);

        for (ComponentFileNaming.Candidate candidate : ComponentFileNaming.candidates(baseName)) {
            VirtualFile candidateFile = childrenByName.get(candidate.fileName());
            if (candidateFile == null) {
                continue;
            }

            Entry entry = new Entry(candidate.kind(), candidateFile);
            if (candidateFile.equals(currentFile)) {
                filesByKind.put(candidate.kind(), entry);
            } else {
                filesByKind.putIfAbsent(candidate.kind(), entry);
            }
        }

        List<Entry> result = new ArrayList<>();
        for (ComponentFileNaming.Kind kind : ComponentFileNaming.Kind.values()) {
            Entry entry = filesByKind.get(kind);
            if (entry != null) {
                result.add(entry);
            }
        }

        if (result.size() < 2) {
            return null;
        }
        return List.copyOf(result);
    }

    private static @NotNull Map<String, VirtualFile> indexChildren(@NotNull VirtualFile parent) {
        Map<String, VirtualFile> childrenByName = new HashMap<>();
        for (VirtualFile child : parent.getChildren()) {
            if (!child.isDirectory()) {
                childrenByName.put(child.getName(), child);
            }
        }
        return childrenByName;
    }
}
