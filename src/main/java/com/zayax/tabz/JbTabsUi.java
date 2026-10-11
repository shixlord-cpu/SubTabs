package com.zayax.tabz;

import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** Helpers that use only public {@link JBTabs} / {@link TabInfo} APIs. */
final class JbTabsUi {
    private JbTabsUi() {
    }

    static @NotNull List<TabInfo> tabInfos(@NotNull JBTabs tabs) {
        int count = tabs.getTabCount();
        List<TabInfo> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            TabInfo info = tabs.getTabAt(index);
            if (info != null) {
                result.add(info);
            }
        }
        return result;
    }

    static @Nullable TabInfo selectedTab(@NotNull JBTabs tabs) {
        return tabs.getSelectedInfo();
    }

    static @Nullable JComponent tabComponent(@NotNull TabInfo tabInfo) {
        Component component = tabInfo.getComponent();
        return component instanceof JComponent jc ? jc : null;
    }

    static void revalidateAndRepaint(@NotNull JBTabs tabs) {
        if (tabs instanceof JComponent component) {
            component.revalidate();
            component.repaint();
        }
    }

    static @Nullable Dimension headerFitSize(@NotNull JBTabs tabs) {
        return InternalPlatformBridge.jbTabsHeaderFitSize(tabs);
    }
}
