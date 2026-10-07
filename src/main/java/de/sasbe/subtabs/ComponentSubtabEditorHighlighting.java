package de.sasbe.subtabs;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

final class ComponentSubtabEditorHighlighting {
    private ComponentSubtabEditorHighlighting() {
    }

    static boolean countsAsTabError(@Nullable HighlightInfo info) {
        if (info == null) {
            return false;
        }
        if (info.getSeverity().compareTo(HighlightSeverity.ERROR) >= 0) {
            return true;
        }
        if (info.getSeverity().compareTo(HighlightSeverity.WARNING) < 0) {
            return false;
        }
        String toolId = info.getInspectionToolId();
        if (toolId == null) {
            return false;
        }
        String normalized = toolId.toLowerCase(Locale.ROOT);
        return normalized.contains("eslint")
                || normalized.contains("stylelint")
                || normalized.contains("typescript")
                || normalized.contains("ts");
    }
}
