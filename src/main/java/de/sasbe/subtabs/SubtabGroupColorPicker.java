package de.sasbe.subtabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.ColorChooser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Window;

final class SubtabGroupColorPicker {
    private SubtabGroupColorPicker() {
    }

    static @Nullable Color choose(@NotNull Project project, @Nullable Color current) {
        return ColorChooser.chooseColor(
                project,
                parentComponent(project),
                "Gruppenfarbe wählen",
                current != null ? current : Color.GRAY,
                false
        );
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
