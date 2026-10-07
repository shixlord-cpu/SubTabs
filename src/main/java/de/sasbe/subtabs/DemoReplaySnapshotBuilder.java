package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;

final class DemoReplaySnapshotBuilder {
    private DemoReplaySnapshotBuilder() {
    }

    static @NotNull DemoReplayJson capture(@NotNull Project project) {
        FileEditorManager manager = FileEditorManager.getInstance(project);
        FileEditorManagerEx managerEx = FileEditorManagerEx.getInstanceEx(project);
        SubtabsSettings settings = SubtabsSettings.getInstance();
        ComponentSubtabEditorSplitRegistry registry =
                ComponentSubtabEditorSplitRegistry.getInstance(project);

        DemoReplayJson root = DemoReplayJson.object();
        root.key("selectedFile").value(pathOf(manager.getSelectedEditor() == null
                ? null
                : manager.getSelectedEditor().getFile()));
        root.nested("settings", settingsSnapshot(settings));
        root.nested("splittabRegistry", splittabRegistrySnapshot(project, registry));
        root.arrayStart("editorWindows");
        EditorWindow[] windows = managerEx.getWindows();
        EditorWindow currentWindow = managerEx.getCurrentWindow();
        for (int index = 0; index < windows.length; index++) {
            EditorWindow window = windows[index];
            root.element(editorWindowSnapshot(project, manager, window, index, window == currentWindow));
        }
        root.arrayEnd();
        return root.endObject();
    }

    private static @NotNull DemoReplayJson settingsSnapshot(@NotNull SubtabsSettings settings) {
        return DemoReplayJson.object()
                .key("familiaEnabled").value(settings.isFamiliaEnabled())
                .key("subtabsActive").value(settings.isSubtabsActive())
                .key("sidetabsActive").value(settings.isSidetabsActive())
                .key("sidetabsExpanded").value(settings.isSidetabsExpanded())
                .key("sidetabsOnRight").value(settings.isSidetabsOnRight())
                .key("sidetabLayoutMode").value(settings.getSidetabLayoutMode().name())
                .endObject();
    }

    private static @NotNull DemoReplayJson splittabRegistrySnapshot(
            @NotNull Project project,
            @NotNull ComponentSubtabEditorSplitRegistry registry
    ) {
        ComponentSubtabEditorSplitRegistry.SplittabPair active = registry.activePair();
        DemoReplayJson json = DemoReplayJson.object()
                .key("activePairId").value(active == null ? null : active.id())
                .key("splittabUiEngaged").value(ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(project))
                .key("splittabChromeShowing").value(active != null
                        && ComponentSubtabEditorSplitNavigation.isSplittabChromeShowing(project, active));
        json.arrayStart("pairs");
        for (ComponentSubtabEditorSplitRegistry.SplittabPair pair : registry.all()) {
            json.element(DemoReplayJson.object()
                    .key("id").value(pair.id())
                    .key("leftFile").value(relativeDemoPath(pair.leftFile()))
                    .key("rightFile").value(relativeDemoPath(pair.rightFile()))
                    .key("linkName").value(pair.linkName())
                    .key("headerLabel").value(pair.headerLabel())
                    .endObject());
        }
        json.arrayEnd();
        return json.endObject();
    }

    private static @NotNull DemoReplayJson editorWindowSnapshot(
            @NotNull Project project,
            @NotNull FileEditorManager manager,
            @NotNull EditorWindow window,
            int index,
            boolean activeWindow
    ) {
        DemoReplayJson json = DemoReplayJson.object()
                .key("index").value(index)
                .key("activeWindow").value(activeWindow)
                .key("rightSplitPane").value(ComponentSubtabEditorLookup.isRightSplitPane(
                        FileEditorManagerEx.getInstanceEx(project),
                        window
                ));
        json.arrayStart("tabs");
        for (VirtualFile file : window.getFiles()) {
            json.element(openTabSnapshot(project, manager, window, file));
        }
        json.arrayEnd();
        return json.endObject();
    }

    private static @NotNull DemoReplayJson openTabSnapshot(
            @NotNull Project project,
            @NotNull FileEditorManager manager,
            @NotNull EditorWindow window,
            @NotNull VirtualFile file
    ) {
        FileEditor editor = ComponentSubtabsManager.selectedEditorFor(manager, file);
        boolean selectedInWindow = window.getSelectedFile() != null && window.getSelectedFile().equals(file);
        DemoReplayJson json = DemoReplayJson.object()
                .key("path").value(relativeDemoPath(file))
                .key("selectedInWindow").value(selectedInWindow)
                .key("pinned").value(window.isFilePinned(file))
                .key("subtabLabel").value(ComponentTabTitles.displaySubtabLabel(file))
                .key("groupName").value(ComponentTabTitles.displayGroupName(file));
        if (editor != null) {
            json.nested("chrome", editorChromeSnapshot(project, editor));
        }
        return json.endObject();
    }

    private static @NotNull DemoReplayJson editorChromeSnapshot(
            @NotNull Project project,
            @NotNull FileEditor editor
    ) {
        ComponentSubtabBarPanel subtabBar = editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY);
        SplittabSwitchBarPanel switchBar = editor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_SWITCH_BAR_KEY);
        SplittabPaneHeaderPanel header = editor.getUserData(ComponentSubtabsSplittabUi.SPLITTAB_HEADER_KEY);
        SidetabBarPanel sidetabBar = editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY);

        DemoReplayJson json = DemoReplayJson.object()
                .key("hasSubtabBar").value(subtabBar != null)
                .key("hasSplittabSwitchBar").value(switchBar != null)
                .key("hasSplittabHeader").value(header != null)
                .key("hasSidetabBar").value(sidetabBar != null);
        if (subtabBar != null) {
            json.key("subtabBarDisplayedFile").value(relativeDemoPath(subtabBar.displayedFile()));
        }
        if (header != null) {
            json.nested("splittabHeader", splittabHeaderSnapshot(header));
        }
        return json.endObject();
    }

    private static @NotNull DemoReplayJson splittabHeaderSnapshot(@NotNull SplittabPaneHeaderPanel header) {
        String title = "";
        boolean titleVisible = false;
        boolean closeVisible = false;
        if (header.getComponentCount() >= 1 && header.getComponent(0) instanceof JLabel label) {
            title = label.getText();
            titleVisible = label.getWidth() > 0 && label.getHeight() > 0 && !title.isBlank();
        }
        if (header.getComponentCount() >= 2) {
            JComponent close = (JComponent) header.getComponent(1);
            closeVisible = close.isShowing() && close.getWidth() > 0 && close.getHeight() > 0;
        }
        return DemoReplayJson.object()
                .key("title").value(title)
                .key("titleVisible").value(titleVisible)
                .key("closeVisible").value(closeVisible)
                .key("headerHeight").value(header.getHeight())
                .key("headerPreferredHeight").value(header.getPreferredSize().height)
                .endObject();
    }

    private static @Nullable String pathOf(@Nullable VirtualFile file) {
        return file == null ? null : relativeDemoPath(file);
    }

    static @NotNull String relativeDemoPath(@NotNull VirtualFile file) {
        String path = file.getPath().replace('\\', '/');
        int demoIndex = path.indexOf("/demo-project/");
        if (demoIndex >= 0) {
            return path.substring(demoIndex + "/demo-project/".length());
        }
        if (path.endsWith("/demo-project")) {
            return "demo-project";
        }
        return file.getName();
    }
}
