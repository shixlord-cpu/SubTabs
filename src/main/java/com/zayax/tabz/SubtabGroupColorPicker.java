package com.zayax.tabz;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JColorChooser;
import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Window;

final class SubtabGroupColorPicker {
    private SubtabGroupColorPicker() {
    }

    static @Nullable Color choose(@NotNull Project project, @Nullable Color current) {
        Color initial = current != null ? current : Color.GRAY;
        return JColorChooser.showDialog(parentComponent(project), "Choose group color", initial);
    }

    private static @Nullable Component parentComponent(@NotNull Project project) {
        IdeFrame frame = WindowManager.getInstance().getIdeFrame(project);
        if (frame != null) {
            return frame.getComponent();
        }

        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        return activeWindow instanceof Component component ? component : null;
    }
}
