package de.sasbe.subtabs;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public final class SubtabsIcons {
    public static final @NotNull Icon ACTIVE = IconLoader.getIcon("/icons/subtabs.png", SubtabsIcons.class);
    public static final @NotNull Icon INACTIVE = IconLoader.getIcon("/icons/subtabsInactive.png", SubtabsIcons.class);

    private SubtabsIcons() {
    }
}
