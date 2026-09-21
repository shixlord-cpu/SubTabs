package de.sasbe.subtabs;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service(Service.Level.PROJECT)
final class SidetabSectionsCache {
    private record Entry(long modificationStamp, int rulesGeneration, @NotNull List<SidetabSection> sections) {
    }

    private final ConcurrentHashMap<String, Entry> byFilePath = new ConcurrentHashMap<>();

    static @NotNull SidetabSectionsCache getInstance(@NotNull com.intellij.openapi.project.Project project) {
        return project.getService(SidetabSectionsCache.class);
    }

    @NotNull List<SidetabSection> split(
            @NotNull VirtualFile file,
            @NotNull String fileName,
            @NotNull String text,
            int rulesGeneration,
            @NotNull List<CustomSidetabRule> rules
    ) {
        long modificationStamp = modificationStamp(file, text);
        String path = file.getPath();
        Entry cached = byFilePath.get(path);
        if (cached != null
                && cached.modificationStamp == modificationStamp
                && cached.rulesGeneration == rulesGeneration) {
            return cached.sections;
        }

        List<SidetabSection> sections = List.copyOf(SidetabSections.split(fileName, text, rules));
        byFilePath.put(path, new Entry(modificationStamp, rulesGeneration, sections));
        return sections;
    }

    void clear() {
        byFilePath.clear();
    }

    void invalidate(@NotNull VirtualFile file) {
        byFilePath.remove(file.getPath());
    }

    private static long modificationStamp(@NotNull VirtualFile file, @NotNull String text) {
        Document document = FileDocumentManager.getInstance().getDocument(file);
        return document == null ? text.hashCode() : document.getModificationStamp();
    }
}
