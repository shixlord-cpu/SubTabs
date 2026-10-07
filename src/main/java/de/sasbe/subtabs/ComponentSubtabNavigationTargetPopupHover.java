package de.sasbe.subtabs;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JList;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class ComponentSubtabNavigationTargetPopupHover {
    private static final String INSTALLED = "componentSubtabs.navigationTargetPopupHover";

    private ComponentSubtabNavigationTargetPopupHover() {
    }

    static void ensureInstalled(@NotNull JList<?> list) {
        if (Boolean.TRUE.equals(list.getClientProperty(INSTALLED))) {
            return;
        }
        list.putClientProperty(INSTALLED, Boolean.TRUE);
        list.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                handleMouse(list, event.getPoint());
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                clearHover(list);
            }

            @Override
            public void mousePressed(MouseEvent event) {
                if (!SwingUtilities.isRightMouseButton(event)) {
                    return;
                }
                if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
                    return;
                }
                VirtualFile file = fileAt(list, event.getPoint());
                if (file == null) {
                    return;
                }
                Project project = projectFor(list);
                if (project == null) {
                    return;
                }
                showFamiliaContextMenu(project, file, list, event.getX(), event.getY());
            }
        });
    }

    static void handleMouse(@NotNull JList<?> list, @NotNull Point pointInList) {
        if (SubtabHoverView.isDisabled()) {
            clearHover(list);
            return;
        }
        Project project = projectFor(list);
        if (project == null) {
            clearHover(list);
            return;
        }
        VirtualFile file = fileAt(list, pointInList);
        if (file == null) {
            clearHover(list);
            return;
        }
        Integer lastRow = (Integer) list.getClientProperty("componentSubtabs.navigationTargetPopupHoverRow");
        int row = list.locationToIndex(pointInList);
        if (lastRow != null && lastRow == row && row >= 0) {
            return;
        }
        list.putClientProperty("componentSubtabs.navigationTargetPopupHoverRow", row);
        clearHover(list);
        enterHover(project, file, list);
    }

    private static void enterHover(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull JList<?> list
    ) {
        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(file);
        if (match != null && match.relatedFiles().size() >= 2) {
            ComponentSubtabProjectViewHover.onEnterRelatedGroup(project, file, list);
            ComponentSubtabMainTabHover.onEnterGroup(project, match.baseName(), list);
            ComponentSubtabBarHover.onEnter(project, file, list);
        } else {
            ComponentSubtabProjectViewHover.onEnter(project, file, list);
            ComponentSubtabMainTabHover.onEnter(project, file, list);
            ComponentSubtabBarHover.onEnter(project, file, list);
        }
    }

    private static void clearHover(@NotNull JList<?> list) {
        list.putClientProperty("componentSubtabs.navigationTargetPopupHoverRow", null);
        ComponentSubtabProjectViewHover.onExit(list);
        ComponentSubtabMainTabHover.onExit(list);
        ComponentSubtabBarHover.onExit(list);
    }

    private static @Nullable VirtualFile fileAt(@NotNull JList<?> list, @NotNull Point pointInList) {
        int row = list.locationToIndex(pointInList);
        if (row < 0 || row >= list.getModel().getSize()) {
            return null;
        }
        if (!list.getCellBounds(row, row).contains(pointInList)) {
            return null;
        }
        return ComponentSubtabNavigationTargetFiles.resolveVirtualFile(list.getModel().getElementAt(row));
    }

    private static @Nullable Project projectFor(@NotNull Component component) {
        Project project = CommonDataKeys.PROJECT.getData(
                DataManager.getInstance().getDataContext(component)
        );
        if (project != null && !project.isDisposed()) {
            return project;
        }
        for (Project open : ProjectManager.getInstance().getOpenProjects()) {
            if (!open.isDisposed()) {
                return open;
            }
        }
        return null;
    }

    private static void showFamiliaContextMenu(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull Component component,
            int x,
            int y
    ) {
        DefaultActionGroup group = new DefaultActionGroup("Familia", true);
        group.add(new AnAction("Im Projektbaum anzeigen") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent event) {
                SubtabProjectViewReveal.revealSubtab(project, file);
            }
        });
        ActionPopupMenu popupMenu = ActionManager.getInstance()
                .createActionPopupMenu("SubTabs.NavigationTargetContextMenu", group);
        popupMenu.getComponent().show(component, x, y);
    }

    static boolean isPopupNavigationList(@NotNull JList<?> list) {
        if (!ComponentSubtabNavigationTargetFiles.isNavigationTargetList(list)) {
            return false;
        }
        Window window = SwingUtilities.getWindowAncestor(list);
        if (window == null) {
            return false;
        }
        return window.getType() == Window.Type.POPUP || window.getType() == Window.Type.UTILITY;
    }
}
