package de.sasbe.subtabs;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service(Service.Level.PROJECT)
final class ComponentRelatedFilesCache implements Disposable {
    private static final Object NO_MATCH = new Object();

    private final ConcurrentHashMap<String, Object> byFilePath = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, java.util.Set<String>> byDirectoryPrefix = new ConcurrentHashMap<>();

    ComponentRelatedFilesCache(@NotNull Project project) {
        MessageBusConnection connection = project.getMessageBus().connect(this);
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
                for (VFileEvent event : events) {
                    VirtualFile file = event.getFile();
                    if (file != null) {
                        invalidateAffected(file);
                    }
                }
            }
        });
    }

    static @NotNull ComponentRelatedFilesCache getInstance(@NotNull Project project) {
        return project.getService(ComponentRelatedFilesCache.class);
    }

    @Nullable ComponentRelatedFiles.Match get(@NotNull VirtualFile file) {
        String path = file.getPath();
        Object cached = byFilePath.get(path);
        if (cached != null) {
            return cached == NO_MATCH ? null : (ComponentRelatedFiles.Match) cached;
        }

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.findUncached(file);
        byFilePath.put(path, match == null ? NO_MATCH : match);
        rememberDirectory(path);
        return match;
    }

    void clear() {
        byFilePath.clear();
        byDirectoryPrefix.clear();
    }

    private void rememberDirectory(@NotNull String filePath) {
        String directory = directoryPrefix(filePath);
        byDirectoryPrefix.computeIfAbsent(directory, unused -> ConcurrentHashMap.newKeySet()).add(filePath);
    }

    private void invalidateAffected(@NotNull VirtualFile file) {
        byFilePath.remove(file.getPath());
        VirtualFile parent = file.getParent();
        if (parent != null) {
            invalidateUnderDirectory(parent);
            VirtualFile grandparent = parent.getParent();
            if (grandparent != null) {
                invalidateUnderDirectory(grandparent);
            }
        }
    }

    private void invalidateUnderDirectory(@NotNull VirtualFile directory) {
        String prefix = normalizePath(directory.getPath()) + "/";
        java.util.Set<String> indexed = byDirectoryPrefix.remove(prefix);
        if (indexed != null) {
            for (String path : indexed) {
                byFilePath.remove(path);
            }
            return;
        }
        byFilePath.keySet().removeIf(path -> normalizePath(path).startsWith(prefix));
    }

    private static @NotNull String directoryPrefix(@NotNull String filePath) {
        String normalized = normalizePath(filePath);
        int separator = normalized.lastIndexOf('/');
        return separator < 0 ? "" : normalized.substring(0, separator + 1);
    }

    private static @NotNull String normalizePath(@NotNull String path) {
        return path.replace('\\', '/');
    }

    @Override
    public void dispose() {
        byFilePath.clear();
        byDirectoryPrefix.clear();
    }
}
