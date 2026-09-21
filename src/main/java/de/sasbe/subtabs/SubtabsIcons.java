package de.sasbe.subtabs;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public final class SubtabsIcons {
    public static final @NotNull Icon ACTIVE = IconLoader.getIcon("/icons/subtabs.png", SubtabsIcons.class);
    public static final @NotNull Icon INACTIVE = IconLoader.getIcon("/icons/subtabsInactive.png", SubtabsIcons.class);
    public static final @NotNull Icon SIDE_ACTIVE = new RotatedIcon(ACTIVE, 90);
    public static final @NotNull Icon SIDE_INACTIVE = new RotatedIcon(INACTIVE, 90);
    public static final @NotNull Icon GROUPING_EXPANDED = new GroupingIcon(true);
    public static final @NotNull Icon GROUPING_COLLAPSED = new GroupingIcon(false);
    public static final @NotNull Icon GROUPING_NODE = GROUPING_EXPANDED;

    private SubtabsIcons() {
    }
}
