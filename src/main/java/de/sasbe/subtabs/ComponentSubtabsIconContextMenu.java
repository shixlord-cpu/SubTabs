package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class ComponentSubtabsIconContextMenu {
    private ComponentSubtabsIconContextMenu() {
    }

    static void installSubtabIconMenu(
            @NotNull Project project,
            @NotNull FileEditor editor,
            @NotNull JComponent button
    ) {
        installOnce(button, "componentSubtabs.subtabIconRightClick", () ->
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent event) {
                        if (!SwingUtilities.isLeftMouseButton(event)) {
                            return;
                        }
                        ComponentSubtabEditorSplitRegistry.SplittabPair splittabPair =
                                ComponentSubtabsSplittabUi.activeSplittabPairForEditor(project, editor);
                        if (splittabPair != null) {
                            event.consume();
                            ComponentSubtabsScopedVisibility.getInstance(project)
                                    .toggleSplittabPairSubtabsCollapsed(project, splittabPair.id());
                            return;
                        }
                        VirtualFile file = editor.getFile();
                        if (file == null || ComponentRelatedFiles.find(file) == null) {
                            return;
                        }
                        event.consume();
                        ComponentSubtabsScopedVisibility.getInstance(project).toggleSubtabGroup(project, file);
                    }
                })
        );
    }

    static void installSidetabIconMenu(
            @NotNull Project project,
            @NotNull FileEditor editor,
            @NotNull JComponent button
    ) {
        installOnce(button, "componentSubtabs.sidetabIconRightClick", () ->
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
                        ComponentSubtabsScopedVisibility.getInstance(project).toggleSidetabFile(project, file);
                    }
                })
        );
    }

    static void installSubtabIconToggleClick(
            @NotNull Project project,
            @NotNull FileEditor editor,
            @NotNull JComponent button
    ) {
        installOnce(button, "componentSubtabs.subtabIconToggleClick", () ->
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent event) {
                        if (!SwingUtilities.isRightMouseButton(event)) {
                            return;
                        }
                        event.consume();
                        ComponentSubtabsScopedVisibility.handleSubtabIconLeftClick(project, editor);
                    }
                })
        );
    }

    static void installSidetabIconToggleClick(
            @NotNull Project project,
            @NotNull FileEditor editor,
            @NotNull JComponent button
    ) {
        installOnce(button, "componentSubtabs.sidetabIconToggleClick", () ->
                button.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent event) {
                        if (!SwingUtilities.isRightMouseButton(event)) {
                            return;
                        }
                        event.consume();
                        ComponentSubtabsScopedVisibility.handleSidetabIconLeftClick(project, editor);
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
