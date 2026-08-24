package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.awt.IllegalComponentStateException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JToggleButton;

final class ComponentSubtabBarHover {
    private static final String ACTIVE_HOVERS_KEY = "componentSubtabs.subtabBarHovers";

    private record Handle(@NotNull JToggleButton button) {
    }

    private ComponentSubtabBarHover() {
    }

    static void onEnter(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull JComponent source
    ) {
        onExit(source);

        List<Handle> handles = findVisibleSubtabButtons(project, file);
        if (handles.isEmpty()) {
            return;
        }

        source.putClientProperty(ACTIVE_HOVERS_KEY, handles);
        for (Handle handle : handles) {
            ComponentSubtabUi.setExternalHover(handle.button(), true);
        }
    }

    static void onExit(@NotNull JComponent source) {
        @SuppressWarnings("unchecked")
        List<Handle> handles = (List<Handle>) source.getClientProperty(ACTIVE_HOVERS_KEY);
        if (handles == null || handles.isEmpty()) {
            return;
        }

        Point pointer = MouseInfo.getPointerInfo().getLocation();
        for (Handle handle : handles) {
            if (isPointerOver(handle.button(), pointer)) {
                source.putClientProperty(ACTIVE_HOVERS_KEY, null);
                return;
            }
        }

        clear(handles);
        source.putClientProperty(ACTIVE_HOVERS_KEY, null);
    }

    private static @NotNull List<Handle> findVisibleSubtabButtons(
            @NotNull Project project,
            @NotNull VirtualFile file
    ) {
        if (!SubtabsSettings.getInstance().isSubtabsActive()) {
            return List.of();
        }

        FileEditorManager manager = FileEditorManager.getInstance(project);
        List<Handle> handles = new ArrayList<>();

        for (FileEditor editor : manager.getAllEditors()) {
            ComponentSubtabBarPanel panel = editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY);
            if (panel == null || !panel.isShowing()) {
                continue;
            }

            JToggleButton button = panel.buttonFor(file);
            if (button != null && button.isShowing()) {
                handles.add(new Handle(button));
            }
        }
        return handles;
    }

    private static void clear(@NotNull List<Handle> handles) {
        for (Handle handle : handles) {
            ComponentSubtabUi.setExternalHover(handle.button(), false);
        }
    }

    private static boolean isPointerOver(@NotNull JToggleButton button, @NotNull Point pointerOnScreen) {
        if (!button.isShowing()) {
            return false;
        }
        try {
            Point origin = button.getLocationOnScreen();
            return new Rectangle(origin, button.getSize()).contains(pointerOnScreen);
        } catch (IllegalComponentStateException ignored) {
            return false;
        }
    }
}
