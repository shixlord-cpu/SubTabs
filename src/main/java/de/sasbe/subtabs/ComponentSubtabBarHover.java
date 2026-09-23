package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.ui.tabs.impl.TabLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private static final String ACTIVE_MAIN_TAB_SYNC_KEY = "componentSubtabs.mainTabBarSync";

    private record Handle(@NotNull JToggleButton button) {
    }

    private ComponentSubtabBarHover() {
    }

    static void onEnterMainTab(
            @NotNull Project project,
            @NotNull VirtualFile tabFile,
            @NotNull JComponent source
    ) {
        if (SubtabHoverView.isDisabled()) {
            return;
        }
        clearMainTabSyncForSource(source, false);

        SubtabGroupPopupPresentation.Context context = SubtabGroupPopupPresentation.forMainTab(project, tabFile);
        java.util.LinkedHashSet<JToggleButton> seen = new java.util.LinkedHashSet<>();
        List<Handle> handles = new ArrayList<>();
        for (VirtualFile file : context.highlightedFiles()) {
            JToggleButton button = findSubtabButtonInMainTabEditor(project, tabFile, file);
            if (button != null && seen.add(button)) {
                handles.add(new Handle(button));
            }
        }
        if (handles.isEmpty()) {
            return;
        }

        source.putClientProperty(ACTIVE_MAIN_TAB_SYNC_KEY, handles);
        for (Handle handle : handles) {
            ComponentSubtabUi.setMainTabSyncHighlight(handle.button(), true);
        }
    }

    static void onExitMainTab(@NotNull JComponent source) {
        clearMainTabSyncForSource(source, true);
    }

    /**
     * Re-applies main-tab hover sync on every tab label that is still in the hover state after the
     * visible file of a main tab changed (for example via the hover select box).
     */
    static void refreshAllActiveMainTabSync(@NotNull Project project) {
        if (SubtabHoverView.isDisabled()) {
            return;
        }

        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        for (EditorWindow window : manager.getWindows()) {
            JBTabs tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }
            for (TabInfo tabInfo : tabsImpl.getTabs()) {
                if (!(tabInfo.getObject() instanceof VirtualFile file)) {
                    continue;
                }
                TabLabel label = tabsImpl.getTabLabel(tabInfo);
                if (label == null || label.getClientProperty(ACTIVE_MAIN_TAB_SYNC_KEY) == null) {
                    continue;
                }
                onEnterMainTab(project, file, label);
            }
        }
    }

    private static void clearMainTabSyncForSource(@NotNull JComponent source, boolean keepHighlightWhenPointerOverSubtab) {
        @SuppressWarnings("unchecked")
        List<Handle> handles = (List<Handle>) source.getClientProperty(ACTIVE_MAIN_TAB_SYNC_KEY);
        if (handles == null || handles.isEmpty()) {
            return;
        }

        if (keepHighlightWhenPointerOverSubtab) {
            Point pointer = MouseInfo.getPointerInfo().getLocation();
            for (Handle handle : handles) {
                if (isPointerOver(handle.button(), pointer)) {
                    source.putClientProperty(ACTIVE_MAIN_TAB_SYNC_KEY, null);
                    return;
                }
            }
        }

        clearMainTabSync(handles);
        source.putClientProperty(ACTIVE_MAIN_TAB_SYNC_KEY, null);
    }

    static void onEnter(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull JComponent source
    ) {
        if (SubtabHoverView.isDisabled()) {
            return;
        }
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

    private static @Nullable JToggleButton findSubtabButtonInMainTabEditor(
            @NotNull Project project,
            @NotNull VirtualFile mainTabFile,
            @NotNull VirtualFile subtabFile
    ) {
        if (!SubtabsSettings.getInstance().isSubtabsActive()) {
            return null;
        }

        FileEditorManager manager = FileEditorManager.getInstance(project);
        for (FileEditor editor : manager.getAllEditors()) {
            if (!mainTabFile.equals(editor.getFile())) {
                continue;
            }
            ComponentSubtabBarPanel panel = editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY);
            if (panel == null) {
                return null;
            }
            return panel.buttonFor(subtabFile);
        }
        return null;
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

    private static void clearMainTabSync(@NotNull List<Handle> handles) {
        for (Handle handle : handles) {
            ComponentSubtabUi.setMainTabSyncHighlight(handle.button(), false);
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
