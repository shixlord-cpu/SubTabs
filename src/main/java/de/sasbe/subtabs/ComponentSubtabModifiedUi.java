package de.sasbe.subtabs;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JToggleButton;
import java.awt.Color;

final class ComponentSubtabModifiedUi {
    static final String PLAIN_LABEL_KEY = "componentSubtabs.plainLabel";

    private static final JBColor MODIFIED_FOREGROUND = new JBColor(new Color(0x0042AA), new Color(0x589DF6));
    private static final JBColor MODIFIED_INACTIVE_FOREGROUND = new JBColor(new Color(0x728AAB), new Color(0x6A8FB8));

    private ComponentSubtabModifiedUi() {
    }

    static boolean isModified(@NotNull Project project, @NotNull VirtualFile file) {
        if (project.isDisposed() || file.isDirectory()) {
            return false;
        }

        Application application = ApplicationManager.getApplication();
        if (application.isReadAccessAllowed()) {
            return isModifiedInReadAction(file);
        }
        return ReadAction.compute(() -> isModifiedInReadAction(file));
    }

    static @NotNull Color foreground(boolean modified, boolean grayed) {
        if (!modified) {
            return grayed ? UIUtil.getInactiveTextColor() : UIUtil.getLabelForeground();
        }
        return grayed ? MODIFIED_INACTIVE_FOREGROUND : MODIFIED_FOREGROUND;
    }

    static @NotNull String htmlColoredText(@NotNull String plainText, @NotNull Color color) {
        return "<html><nobr><font color='" + ColorUtil.toHtmlColor(color) + "'>" + plainText + "</font></nobr></html>";
    }

    static void applyToToggleButton(
            @NotNull JToggleButton button,
            @NotNull String plainLabel,
            boolean modified,
            boolean grayed
    ) {
        button.putClientProperty(PLAIN_LABEL_KEY, plainLabel);
        Color color = foreground(modified, grayed);
        button.setText(plainLabel);
        button.setForeground(color);
    }

    static void applyToLabel(
            @NotNull JBLabel label,
            @NotNull String plainLabel,
            boolean modified,
            boolean grayed
    ) {
        label.putClientProperty(PLAIN_LABEL_KEY, plainLabel);
        Color color = foreground(modified, grayed);
        label.setText(modified ? htmlColoredText(plainLabel, color) : plainLabel);
        label.setForeground(color);
    }

    static @NotNull String plainLabel(@NotNull JToggleButton button) {
        Object value = button.getClientProperty(PLAIN_LABEL_KEY);
        if (value instanceof String label) {
            return label;
        }
        String text = button.getText();
        return text.startsWith("<html>") ? stripHtml(text) : text;
    }

    static @NotNull String plainLabel(@NotNull JBLabel label) {
        Object value = label.getClientProperty(PLAIN_LABEL_KEY);
        if (value instanceof String plain) {
            return plain;
        }
        String text = label.getText();
        return text.startsWith("<html>") ? stripHtml(text) : text;
    }

    private static @NotNull String stripHtml(@NotNull String html) {
        return html.replaceAll("<[^>]+>", "");
    }

    private static boolean isModifiedInReadAction(@NotNull VirtualFile file) {
        FileDocumentManager manager = FileDocumentManager.getInstance();
        if (manager.isFileModified(file)) {
            return true;
        }

        Document document = manager.getDocument(file);
        return document != null && manager.isDocumentUnsaved(document);
    }
}
