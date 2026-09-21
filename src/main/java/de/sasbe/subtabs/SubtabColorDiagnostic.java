package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class SubtabColorDiagnostic {
    private SubtabColorDiagnostic() {
    }

    static boolean isEnabled() {
        return "1".equals(System.getenv("SUBTABS_DIAGNOSE_COLORS"));
    }

    static void schedule(@NotNull Project project) {
        ApplicationManager.getApplication().invokeLater(() -> run(project));
    }

    private static void run(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }

        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setSubtabsActive(true);
        SubtabGroupColors.setEnabled(true);
        SubtabGroupColors.ensureColorsForAllKnownGroups();

        openDemoFiles(project);
        ComponentSubtabMainTabColors.refresh(project);

        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }
            ComponentSubtabMainTabColors.refresh(project);
            writeReport(project);
            System.exit(0);
        });
    }

    private static void openDemoFiles(@NotNull Project project) {
        VirtualFile html = findDemoFile(project, "product-list.component.html");
        VirtualFile scss = findDemoFile(project, "product-list.component.scss");
        FileEditorManager manager = FileEditorManager.getInstance(project);
        if (html != null) {
            manager.openFile(html, true);
        }
        if (scss != null) {
            manager.openFile(scss, true);
        }
    }

    private static @org.jetbrains.annotations.Nullable VirtualFile findDemoFile(
            @NotNull Project project,
            @NotNull String name
    ) {
        VirtualFile base = project.getBaseDir();
        if (base == null) {
            return null;
        }
        VirtualFile file = base.findFileByRelativePath("src/app/" + name);
        if (file != null) {
            return file;
        }
        return LocalFileSystem.getInstance().findFileByPath(base.getPath() + "/src/app/" + name);
    }

    private static void writeReport(@NotNull Project project) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        EditorWindow currentWindow = manager.getCurrentWindow();
        List<String> lines = new ArrayList<>();
        lines.add("groupColorsEnabled=" + SubtabGroupColors.isEnabled());

        for (EditorWindow window : manager.getWindows()) {
            JBTabs tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }
            TabInfo selected = tabsImpl.getSelectedInfo();
            boolean windowFocused = window == currentWindow;
            for (TabInfo tabInfo : tabsImpl.getTabs()) {
                if (!(tabInfo.getObject() instanceof VirtualFile file)) {
                    continue;
                }
                var tabColor = tabInfo.getTabColor();
                var groupColor = SubtabGroupColors.colorForFile(file);
                var registryColor = SubtabGroupMainTabColorRegistry.getInstance(project).get(file);
                lines.add(String.format(
                        "file=%s selected=%s focusedWindow=%s tabColor=%s groupColor=%s registryColor=%s",
                        file.getName(),
                        tabInfo == selected,
                        windowFocused,
                        formatColor(tabColor),
                        formatColor(groupColor),
                        formatColor(registryColor)
                ));
            }
        }

        Path output = Path.of(project.getBasePath()).getParent().resolve("build/tab-color-diagnostics.txt");
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, String.join(System.lineSeparator(), lines), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static @NotNull String formatColor(@org.jetbrains.annotations.Nullable java.awt.Color color) {
        if (color == null) {
            return "null";
        }
        return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
    }
}
