package de.sasbe.subtabs;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

@Service(Service.Level.PROJECT)
final class SubtabGroupMainTabColorRegistry {
    private final Map<VirtualFile, Color> tints = new HashMap<>();
    private final Map<VirtualFile, Float> mixes = new HashMap<>();

    static @NotNull SubtabGroupMainTabColorRegistry getInstance(@NotNull Project project) {
        return project.getService(SubtabGroupMainTabColorRegistry.class);
    }

    void clear() {
        tints.clear();
        mixes.clear();
    }

    void put(@NotNull VirtualFile file, @NotNull Color tint, float mix) {
        Float existingMix = mixes.get(file);
        if (existingMix == null || mix > existingMix) {
            tints.put(file, tint);
            mixes.put(file, mix);
        }
    }

    @Nullable Color get(@NotNull VirtualFile file) {
        return tints.get(file);
    }
}
