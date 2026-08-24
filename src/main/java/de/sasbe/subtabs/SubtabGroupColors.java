package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class SubtabGroupColors {
    private static final int[] PALETTE = {
            0xE11D48, 0x2563EB, 0x059669, 0xD97706, 0x7C3AED, 0x0891B2,
            0xDB2777, 0x4F46E5, 0x16A34A, 0xEA580C, 0x9333EA, 0x0D9488,
            0xBE123C, 0x1D4ED8, 0x047857, 0xB45309, 0x6D28D9, 0x0E7490
    };

    private SubtabGroupColors() {
    }

    static boolean isEnabled() {
        return SubtabsSettings.getInstance().isGroupColorsEnabled();
    }

    static void setEnabled(boolean enabled) {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        boolean wasEnabled = settings.isGroupColorsEnabled();
        settings.setGroupColorsEnabled(enabled);
        if (enabled && !wasEnabled) {
            ensureColorsForAllKnownGroups();
        }
    }

    static @Nullable Color colorForFile(@Nullable VirtualFile file) {
        if (!isEnabled() || file == null) {
            return null;
        }
        return colorForKey(colorKey(file));
    }

    static @Nullable Color colorForGroupNode(@NotNull SubtabGroupProjectViewNode groupNode) {
        if (!isEnabled()) {
            return null;
        }
        VirtualFile primary = groupNode.getVirtualFile();
        return primary == null ? null : colorForFile(primary);
    }

    static @Nullable String colorKey(@NotNull VirtualFile file) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        return match == null ? null : match.key();
    }

    static @Nullable Color colorForKey(@Nullable String key) {
        if (!isEnabled() || key == null || key.isBlank()) {
            return null;
        }
        String hex = SubtabsSettings.getInstance().getGroupColorHex(key);
        if (hex == null || hex.isBlank()) {
            return null;
        }
        try {
            return ColorUtil.fromHex(hex);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static @NotNull Color ensureColor(@NotNull String key) {
        SubtabsSettings settings = SubtabsSettings.getInstance();
        Color existing = colorForKey(key);
        if (existing != null) {
            return existing;
        }
        Color assigned = paletteColor(settings.getNextGroupColorIndex());
        settings.setGroupColorHex(key, ColorUtil.toHex(assigned));
        settings.setNextGroupColorIndex(settings.getNextGroupColorIndex() + 1);
        return assigned;
    }

    static void setColor(@NotNull String key, @NotNull Color color) {
        SubtabsSettings.getInstance().setGroupColorHex(key, ColorUtil.toHex(color));
    }

    static void ensureColorsForAllKnownGroups() {
        if (!isEnabled()) {
            return;
        }
        Set<String> keys = new LinkedHashSet<>();
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed()) {
                continue;
            }
            collectKeys(project, keys);
        }
        for (String key : keys) {
            ensureColor(key);
        }
    }

    static void onGroupDiscovered(@NotNull String key) {
        if (isEnabled()) {
            ensureColor(key);
        }
    }

    private static void collectKeys(@NotNull Project project, @NotNull Set<String> keys) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (VirtualFile file : manager.getOpenFiles()) {
            String key = colorKey(file);
            if (key != null) {
                keys.add(key);
            }
        }
    }

    private static @NotNull Color paletteColor(int index) {
        int paletteIndex = Math.floorMod(index, PALETTE.length);
        Color light = new Color(PALETTE[paletteIndex]);
        Color dark = lightenForDarkTheme(light);
        return new JBColor(light, dark);
    }

    private static @NotNull Color lightenForDarkTheme(@NotNull Color light) {
        float[] hsb = Color.RGBtoHSB(light.getRed(), light.getGreen(), light.getBlue(), null);
        hsb[1] = Math.min(1f, hsb[1] * 0.75f);
        hsb[2] = Math.min(1f, hsb[2] * 1.15f);
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    static @NotNull Map<String, Color> snapshot() {
        Map<String, Color> snapshot = new LinkedHashMap<>();
        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            for (Map.Entry<String, String> entry : SubtabsSettings.getInstance().getGroupColorHexes().entrySet()) {
                Color color = colorForKey(entry.getKey());
                if (color != null) {
                    snapshot.put(entry.getKey(), color);
                }
            }
        }
        return snapshot;
    }
}
