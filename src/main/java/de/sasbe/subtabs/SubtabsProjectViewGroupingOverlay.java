package de.sasbe.subtabs;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Places the project-view grouping toggle on the first tree row inside the project view viewport.
 */
final class SubtabsProjectViewGroupingOverlay {
    private static final Key<Handle> OVERLAY_KEY = Key.create("componentSubtabs.projectViewGroupingOverlay");
    private static final Key<Set<Handle>> PROJECT_HANDLES_KEY =
            Key.create("componentSubtabs.projectViewGroupingHandles");
    private static final Key<PropertyChangeListener> VIEWPORT_LISTENER_KEY =
            Key.create("componentSubtabs.projectViewGroupingViewportListener");
    private static final Key<Integer> INSTALL_GENERATION_KEY =
            Key.create("componentSubtabs.projectViewGroupingInstallGeneration");
    private static final int MAX_INSTALL_ATTEMPTS = 20;

    private SubtabsProjectViewGroupingOverlay() {
    }

    static void installOn(@NotNull Project project) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            disposeAll(project);
            return;
        }
        scheduleInstall(project, 0);
    }

    static void disposeAll(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }
        bumpInstallGeneration(project);
        Set<Handle> handles = project.getUserData(PROJECT_HANDLES_KEY);
        if (handles != null) {
            for (Handle handle : new ArrayList<>(handles)) {
                handle.dispose();
            }
        }
        for (JTree tree : projectViewTrees(project)) {
            disposeHandle(tree);
        }
        clearOrphanedGroupingControls(project);
        project.putUserData(PROJECT_HANDLES_KEY, null);
    }

    static void installOnTree(@NotNull Project project, @NotNull JTree tree) {
        if (project.isDisposed()) {
            return;
        }
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            disposeHandle(tree);
            return;
        }
        if (!SubtabsSettings.getInstance().isShowCollapseButton()) {
            disposeHandle(tree);
            return;
        }
        show(project, tree);
    }

    static void refresh(@NotNull Project project) {
        installOn(project);
    }

    static void installNow(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }
        if (ApplicationManager.getApplication().isDispatchThread()) {
            installNow(project, 0);
        } else {
            SwingUtilities.invokeLater(() -> installNow(project, 0));
        }
    }

    static void flushBusyPresentation(@NotNull Project project) {
        setBusy(project, true);
        Set<Handle> handles = project.getUserData(PROJECT_HANDLES_KEY);
        if (handles == null || handles.isEmpty()) {
            return;
        }
        for (Handle handle : handles) {
            handle.button.repaint();
        }
    }

    static void refreshPresentation(@NotNull Project project) {
        Set<Handle> handles = project.getUserData(PROJECT_HANDLES_KEY);
        if (handles == null) {
            return;
        }
        for (Handle handle : handles) {
            handle.updateButtonPresentation(project);
        }
    }

    static void setBusy(@NotNull Project project, boolean busy) {
        Set<Handle> handles = project.getUserData(PROJECT_HANDLES_KEY);
        if (handles == null) {
            return;
        }
        for (Handle handle : handles) {
            handle.setBusy(busy, project);
        }
    }

    static void finishBusyWhenReady(@NotNull Project project) {
        SubtabsProjectViewGroupingBusyWatcher.getInstance(project).awaitSettled(() -> {
            if (project.isDisposed()) {
                SubtabsProjectViewGroupingBusyState.getInstance(project).end();
                return;
            }
            SubtabGroupTreeControl.installOn(project);
            SubtabsProjectViewGroupingOverlay.installNow(project);
            SubtabsProjectViewGroupingOverlay.refreshPresentation(project);
            SubtabsProjectViewGroupingBusyState.getInstance(project).end();
        });
    }

    @TestOnly
    static void attachForTest(@NotNull Project project, @NotNull JScrollPane scrollPane) {
        JTree tree = UIUtil.findComponentOfType(scrollPane, JTree.class);
        if (tree != null) {
            installOnTree(project, tree);
        }
    }

    @TestOnly
    static @Nullable ComponentSubtabIconButton installedGroupingButtonForTree(@NotNull JTree tree) {
        Handle handle = handleFor(tree);
        return handle == null ? null : handle.button;
    }

    @TestOnly
    static @Nullable Component installedButtonForTree(@NotNull JTree tree) {
        Handle handle = handleFor(tree);
        return handle == null ? null : handle.button;
    }

    @TestOnly
    static @Nullable Component installedButton(@NotNull Project project) {
        for (JTree tree : projectViewTrees(project)) {
            Handle handle = handleFor(tree);
            if (handle != null && handle.button.isVisible()) {
                return handle.button;
            }
        }
        return null;
    }

    private static void scheduleInstall(@NotNull Project project, int attempt) {
        Runnable task = () -> installNow(project, attempt);
        if (ApplicationManager.getApplication().isDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    private static void installNow(@NotNull Project project, int attempt) {
        if (project.isDisposed()) {
            return;
        }
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            disposeAll(project);
            return;
        }

        List<JTree> trees = projectViewTrees(project);
        if (trees.isEmpty()) {
            retryLater(project, attempt);
            return;
        }

        if (!SubtabsSettings.getInstance().isShowCollapseButton()) {
            for (JTree tree : trees) {
                disposeHandle(tree);
            }
            return;
        }

        for (JTree tree : trees) {
            show(project, tree);
        }
    }

    private static void retryLater(@NotNull Project project, int attempt) {
        if (attempt >= MAX_INSTALL_ATTEMPTS) {
            if (SubtabsProjectViewGroupingBusyState.getInstance(project).isBusy()) {
                finishBusyWhenReady(project);
            }
            return;
        }
        int generation = installGeneration(project);
        ApplicationManager.getApplication().invokeLater(
                () -> {
                    if (generation != installGeneration(project)) {
                        return;
                    }
                    scheduleInstall(project, attempt + 1);
                },
                project.getDisposed()
        );
    }

    private static void bumpInstallGeneration(@NotNull Project project) {
        project.putUserData(INSTALL_GENERATION_KEY, installGeneration(project) + 1);
    }

    private static int installGeneration(@NotNull Project project) {
        Integer generation = project.getUserData(INSTALL_GENERATION_KEY);
        return generation == null ? 0 : generation;
    }

    private static void restoreTreeInViewport(@NotNull JTree tree) {
        JViewport viewport = viewportOf(tree);
        if (viewport == null) {
            return;
        }
        Component view = viewport.getView();
        if (view instanceof ProjectViewTreeOverlayPanel panel && panel.tree() == tree) {
            viewport.setView(tree);
            viewport.revalidate();
            viewport.repaint();
        }
    }

    private static void clearOrphanedGroupingControls(@NotNull Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (toolWindow == null) {
            return;
        }
        for (JBScrollPane scrollPane : UIUtil.findComponentsOfType(toolWindow.getComponent(), JBScrollPane.class)) {
            Component status = scrollPane.getStatusComponent();
            if (status instanceof ComponentSubtabIconButton groupingButton) {
                scrollPane.setStatusComponent(null);
                groupingButton.setVisible(false);
            }
            Component view = scrollPane.getViewport().getView();
            if (view instanceof ProjectViewTreeOverlayPanel panel) {
                restoreTreeInViewport(panel.tree());
            }
        }
    }

    private static void show(@NotNull Project project, @NotNull JTree tree) {
        Handle handle = handleFor(tree);
        if (handle == null) {
            ComponentSubtabIconButton button = createButton(project);
            handle = new Handle(project, tree, button);
            tree.putClientProperty(OVERLAY_KEY, handle);
            registerHandle(project, handle);
        }
        if (SubtabsProjectViewGroupingBusyState.getInstance(project).isBusy()) {
            handle.setBusy(true, project);
        } else {
            handle.updateButtonPresentation(project);
        }
        handle.attach();
    }

    private static @NotNull ComponentSubtabIconButton createButton(@NotNull Project project) {
        ComponentSubtabIconButton button = new ComponentSubtabIconButton(SubtabsIcons.ACTIVE);
        int size = Math.max(JBUI.scale(20), ComponentSubtabUi.tabHeight());
        Dimension dimension = new Dimension(size, size);
        button.setPreferredSize(dimension);
        button.setMinimumSize(dimension);
        button.addActionListener(event -> SubtabsProjectViewGroupingState.getInstance(project).toggle(project));
        return button;
    }

    static @NotNull List<JTree> projectViewTrees(@NotNull Project project) {
        Set<JTree> trees = new LinkedHashSet<>();

        AbstractProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        if (pane != null && pane.getTree() != null) {
            trees.add(pane.getTree());
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (toolWindow != null) {
            trees.addAll(UIUtil.findComponentsOfType(toolWindow.getComponent(), JTree.class));
        }

        return new ArrayList<>(trees);
    }

    private static void registerHandle(@NotNull Project project, @NotNull Handle handle) {
        Set<Handle> handles = project.getUserData(PROJECT_HANDLES_KEY);
        if (handles == null) {
            handles = new LinkedHashSet<>();
            project.putUserData(PROJECT_HANDLES_KEY, handles);
        }
        handles.add(handle);
    }

    private static void unregisterHandle(@NotNull Project project, @NotNull Handle handle) {
        Set<Handle> handles = project.getUserData(PROJECT_HANDLES_KEY);
        if (handles != null) {
            handles.remove(handle);
            if (handles.isEmpty()) {
                project.putUserData(PROJECT_HANDLES_KEY, null);
            }
        }
    }

    private static @Nullable Handle handleFor(@NotNull JTree tree) {
        Object value = tree.getClientProperty(OVERLAY_KEY);
        return value instanceof Handle handle ? handle : null;
    }

    private static void disposeHandle(@NotNull JTree tree) {
        Handle handle = handleFor(tree);
        if (handle != null) {
            handle.dispose();
        }
    }

    private static @Nullable JViewport viewportOf(@NotNull Component component) {
        Component current = component;
        while (current != null) {
            if (current instanceof JViewport viewport) {
                return viewport;
            }
            current = current.getParent();
        }
        return null;
    }

    private static final class Handle {
        private final Project project;
        private final JTree tree;
        private final ComponentSubtabIconButton button;
        private @Nullable ProjectViewTreeOverlayPanel overlayPanel;
        private @Nullable JViewport watchedViewport;
        private boolean attaching;

        private Handle(
                @NotNull Project project,
                @NotNull JTree tree,
                @NotNull ComponentSubtabIconButton button
        ) {
            this.project = project;
            this.tree = tree;
            this.button = button;
        }

        private void attach() {
            if (attaching) {
                return;
            }
            attaching = true;
            try {
                attachImpl();
            } finally {
                attaching = false;
            }
        }

        private void attachImpl() {
            JViewport viewport = viewportOf(tree);
            if (viewport == null) {
                attachToScrollPaneStatusComponent();
                return;
            }

            installViewportListener(viewport);
            Component view = viewport.getView();

            if (view instanceof ProjectViewTreeOverlayPanel existing) {
                if (existing.trailingControl() == button) {
                    overlayPanel = existing;
                    layoutControl(viewport);
                    clearScrollPaneStatusComponent();
                    return;
                }
                viewport.setView(tree);
            }

            overlayPanel = new ProjectViewTreeOverlayPanel(tree, button);
            overlayPanel.setComponentZOrder(button, 0);
            viewport.setView(overlayPanel);
            layoutControl(viewport);
            clearScrollPaneStatusComponent();
        }

        private void attachToScrollPaneStatusComponent() {
            JScrollPane scrollPane = ComponentUtil.getScrollPane(tree);
            if (!(scrollPane instanceof JBScrollPane jbScrollPane)) {
                return;
            }
            jbScrollPane.setStatusComponent(button);
            button.setVisible(true);
        }

        private void clearScrollPaneStatusComponent() {
            JScrollPane scrollPane = ComponentUtil.getScrollPane(tree);
            if (scrollPane instanceof JBScrollPane jbScrollPane && jbScrollPane.getStatusComponent() == button) {
                jbScrollPane.setStatusComponent(null);
            }
        }

        private void installViewportListener(@NotNull JViewport viewport) {
            if (watchedViewport == viewport && viewport.getClientProperty(VIEWPORT_LISTENER_KEY) != null) {
                return;
            }

            if (watchedViewport != null) {
                Object previous = watchedViewport.getClientProperty(VIEWPORT_LISTENER_KEY);
                if (previous instanceof PropertyChangeListener listener) {
                    watchedViewport.removePropertyChangeListener("view", listener);
                }
                watchedViewport.putClientProperty(VIEWPORT_LISTENER_KEY, null);
            }

            PropertyChangeListener listener = event -> {
                if (!"view".equals(event.getPropertyName())) {
                    return;
                }
                Object newView = event.getNewValue();
                if (newView == tree) {
                    ApplicationManager.getApplication().invokeLater(
                            this::attach,
                            project.getDisposed()
                    );
                }
            };
            viewport.addPropertyChangeListener("view", listener);
            viewport.putClientProperty(VIEWPORT_LISTENER_KEY, listener);
            watchedViewport = viewport;
        }

        private void layoutControl(@NotNull JViewport viewport) {
            if (overlayPanel == null) {
                return;
            }
            overlayPanel.relayout();
            overlayPanel.revalidate();
            viewport.revalidate();
            viewport.repaint();
        }

        private void updateButtonPresentation(@NotNull Project project) {
            if (button.isLoading()) {
                return;
            }
            boolean collapsed = SubtabsProjectViewGroupingState.getInstance(project).isCollapsed();
            button.setIcon(collapsed ? SubtabsIcons.INACTIVE : SubtabsIcons.ACTIVE);
            String tooltip = collapsed
                    ? "Gruppierung im Projektbaum ausklappen"
                    : "Gruppierung im Projektbaum einklappen";
            button.setToolTipText(tooltip);
            button.getAccessibleContext().setAccessibleName(tooltip);
        }

        private void setBusy(boolean busy, @NotNull Project project) {
            button.setLoading(busy);
            if (!busy) {
                updateButtonPresentation(project);
            }
        }

        private void dispose() {
            if (watchedViewport != null) {
                Object previous = watchedViewport.getClientProperty(VIEWPORT_LISTENER_KEY);
                if (previous instanceof PropertyChangeListener listener) {
                    watchedViewport.removePropertyChangeListener("view", listener);
                }
                watchedViewport.putClientProperty(VIEWPORT_LISTENER_KEY, null);
                watchedViewport = null;
            }

            restoreTreeInViewport(tree);

            Container buttonParent = button.getParent();
            if (buttonParent != null) {
                buttonParent.remove(button);
            }
            button.setVisible(false);

            clearScrollPaneStatusComponent();
            overlayPanel = null;
            unregisterHandle(project, this);
            tree.putClientProperty(OVERLAY_KEY, null);
        }
    }
}
