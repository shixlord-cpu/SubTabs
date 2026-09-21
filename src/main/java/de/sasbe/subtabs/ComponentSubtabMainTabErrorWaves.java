package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.ui.tabs.impl.TabLabel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ComponentSubtabMainTabErrorWaves {
    private ComponentSubtabMainTabErrorWaves() {
    }

    static void refresh(@NotNull Project project) {
        if (project.isDisposed()) {
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
                apply(tabInfo, tabsImpl.getTabLabel(tabInfo),
                        ComponentSubtabFilePresentation.compute(project, file).hasErrors());
            }
        }
    }

    static void applyToFile(@NotNull Project project, @NotNull VirtualFile file, boolean hasErrors) {
        if (project.isDisposed()) {
            return;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        for (EditorWindow window : manager.getWindows()) {
            JBTabs tabs = window.getTabbedPane().getTabs();
            if (!(tabs instanceof JBTabsImpl tabsImpl)) {
                continue;
            }
            for (TabInfo tabInfo : tabsImpl.getTabs()) {
                if (file.equals(tabInfo.getObject())) {
                    apply(tabInfo, tabsImpl.getTabLabel(tabInfo), hasErrors);
                }
            }
        }
    }

    static void apply(@NotNull TabInfo tabInfo, boolean hasErrors) {
        apply(tabInfo, null, hasErrors);
    }

    static void apply(@NotNull TabInfo tabInfo, @Nullable TabLabel label, boolean hasErrors) {
        tabInfo.setDefaultStyle(hasErrors ? SimpleTextAttributes.STYLE_WAVED : -1);
        if (label == null) {
            return;
        }
        for (SimpleColoredComponent colored : UIUtil.findComponentsOfType(label, SimpleColoredComponent.class)) {
            applyWave(colored, hasErrors);
        }
        label.repaint();
    }

    private static void applyWave(@NotNull SimpleColoredComponent colored, boolean hasErrors) {
        for (SimpleColoredComponent.ColoredIterator iterator = colored.iterator(); iterator.hasNext(); ) {
            iterator.next();
            SimpleTextAttributes current = iterator.getTextAttributes();
            int style = current.getStyle();
            if (hasErrors) {
                style |= SimpleTextAttributes.STYLE_WAVED;
            } else {
                style &= ~SimpleTextAttributes.STYLE_WAVED;
            }
            iterator.setTextAttributes(new SimpleTextAttributes(
                    style,
                    current.getFgColor(),
                    hasErrors ? ComponentSubtabTextPainter.errorWaveColor() : null
            ));
        }
    }
}
