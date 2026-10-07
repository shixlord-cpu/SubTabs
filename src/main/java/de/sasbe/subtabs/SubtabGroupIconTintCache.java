package de.sasbe.subtabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColorUtil;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches uncolored and group-tinted file icons per project. Tree repaints and tab refreshes
 * otherwise repeat the full {@link FileIconProvider} chain and {@link IconUtil#colorize} work.
 */
final class SubtabGroupIconTintCache {
    private static final Key<Cache> CACHE_KEY = Key.create("componentSubtabs.groupIconTintCache");

    private SubtabGroupIconTintCache() {
    }

    static @Nullable Icon tintedFileIcon(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull Color groupColor,
            @Iconable.IconFlags int flags
    ) {
        if (project.isDisposed()) {
            return null;
        }
        String key = cacheKey(file, groupColor, flags);
        Cache cache = cache(project);
        Icon cached = cache.tinted.get(key);
        if (cached != null) {
            return cached;
        }
        Icon base = baseFileIcon(project, file, flags, cache);
        if (base == null) {
            return null;
        }
        Icon tinted = IconUtil.colorize(base, groupColor);
        cache.tinted.put(key, tinted);
        return tinted;
    }

    static @Nullable Icon baseFileIcon(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @Iconable.IconFlags int flags
    ) {
        if (project.isDisposed()) {
            return null;
        }
        return baseFileIcon(project, file, flags, cache(project));
    }

    private static @Nullable Icon baseFileIcon(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @Iconable.IconFlags int flags,
            @NotNull Cache cache
    ) {
        String key = baseKey(file, flags);
        Icon cached = cache.base.get(key);
        if (cached != null) {
            return cached;
        }
        Icon base = SubtabGroupFileIconProvider.uncoloredPlatformIcon(file, flags, project);
        if (base != null) {
            cache.base.put(key, base);
        }
        return base;
    }

    static void clear(@NotNull Project project) {
        if (!project.isDisposed()) {
            project.putUserData(CACHE_KEY, null);
        }
    }

    static void clearAllOpenProjects() {
        for (Project project : com.intellij.openapi.project.ProjectManager.getInstance().getOpenProjects()) {
            clear(project);
        }
    }

    private static @NotNull Cache cache(@NotNull Project project) {
        Cache cache = project.getUserData(CACHE_KEY);
        if (cache == null) {
            cache = new Cache();
            project.putUserData(CACHE_KEY, cache);
        }
        return cache;
    }

    private static @NotNull String baseKey(@NotNull VirtualFile file, @Iconable.IconFlags int flags) {
        return file.getPath() + "|" + file.getModificationCount() + "|b|" + flags;
    }

    private static @NotNull String cacheKey(
            @NotNull VirtualFile file,
            @NotNull Color groupColor,
            @Iconable.IconFlags int flags
    ) {
        return file.getPath() + "|" + file.getModificationCount() + "|t|" + flags + "|" + ColorUtil.toHex(groupColor);
    }

    private static final class Cache {
        private final Map<String, Icon> base = new ConcurrentHashMap<>();
        private final Map<String, Icon> tinted = new ConcurrentHashMap<>();
    }
}
