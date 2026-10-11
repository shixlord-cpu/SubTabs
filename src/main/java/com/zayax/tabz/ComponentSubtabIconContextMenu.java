package com.zayax.tabz;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class ComponentSubtabIconContextMenu {
    private ComponentSubtabIconContextMenu() {
    }

    static void installTabzIconMenu(
            @NotNull Project project,
            @NotNull FileEditor editor,
            @NotNull JComponent button
    ) {
        installOnce(button, "componentTabz.tabzIconRightClick", () ->
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent event) {
                        if (!SwingUtilities.isLeftMouseButton(event)) {
                            return;
                        }
                        ComponentSubtabEditorSplitRegistry.SplittabPair splittabPair =
                                ComponentSubtabSplittabUi.activeSplittabPairForEditor(project, editor);
                        if (splittabPair != null) {
                            event.consume();
                            ComponentSubtabScopedVisibility.getInstance(project)
                                    .toggleSplittabPairTabzCollapsed(project, splittabPair.id());
                            return;
                        }
                        VirtualFile file = editor.getFile();
                        if (file == null || ComponentRelatedFiles.find(file) == null) {
                            return;
                        }
                        event.consume();
                        ComponentSubtabScopedVisibility.getInstance(project).toggleSubtabGroup(project, file);
                    }
                })
        );
    }

    static void installSidetabIconMenu(
            @NotNull Project project,
            @NotNull FileEditor editor,
            @NotNull JComponent button
    ) {
        installOnce(button, "componentTabz.sidetabIconRightClick", () ->
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent event) {
                        if (!SwingUtilities.isLeftMouseButton(event)) {
                            return;
                        }
                        VirtualFile file = editor.getFile();
                        if (file == null) {
                            return;
                        }
                        event.consume();
                        ComponentSubtabScopedVisibility.getInstance(project).toggleSidetabFile(project, file);
                    }
                })
        );
    }

    static void installTabzIconToggleClick(
            @NotNull Project project,
            @NotNull FileEditor editor,
            @NotNull JComponent button
    ) {
        installOnce(button, "componentTabz.tabzIconToggleClick", () ->
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent event) {
                        if (!SwingUtilities.isRightMouseButton(event)) {
                            return;
                        }
                        event.consume();
                        ComponentSubtabScopedVisibility.handleTabzIconLeftClick(project, editor);
                    }
                })
        );
    }

    static void installSidetabIconToggleClick(
            @NotNull Project project,
            @NotNull FileEditor editor,
            @NotNull JComponent button
    ) {
        installOnce(button, "componentTabz.sidetabIconToggleClick", () ->
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent event) {
                        if (!SwingUtilities.isRightMouseButton(event)) {
                            return;
                        }
                        event.consume();
                        ComponentSubtabScopedVisibility.handleSidetabIconLeftClick(project, editor);
                    }
                })
        );
    }

    private static void installOnce(
            @NotNull JComponent button,
            @NotNull String key,
            @NotNull Runnable installer
    ) {
        if (Boolean.TRUE.equals(button.getClientProperty(key))) {
            return;
        }
        button.putClientProperty(key, Boolean.TRUE);
        installer.run();
    }
}
