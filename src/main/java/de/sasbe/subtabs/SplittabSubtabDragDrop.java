package de.sasbe.subtabs;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

final class SplittabSubtabDragDrop {
    private static @Nullable Active active;

    private record Active(
            @NotNull Project project,
            @NotNull ComponentSubtabBarPanel barPanel,
            @NotNull VirtualFile anchorFile,
            @NotNull VirtualFile dragFile,
            @NotNull FileEditor editor
    ) {
    }

    private SplittabSubtabDragDrop() {
    }

    static void begin(
            @NotNull Project project,
            @NotNull ComponentSubtabBarPanel barPanel,
            @NotNull VirtualFile dragFile
    ) {
        if (dragFile.equals(barPanel.displayedFile())) {
            return;
        }
        if (!ComponentSubtabNavigation.sameSubtabGroup(barPanel.displayedFile(), dragFile)) {
            return;
        }
        FileEditor editor = ComponentSubtabsManager.editorHostingSubtabBar(project, barPanel);
        if (editor == null) {
            return;
        }
        active = new Active(project, barPanel, barPanel.displayedFile(), dragFile, editor);
        SplittabRestoreOverlay.showForSubtabDragDrop(project, editor);
    }

    static void updatePointer(@NotNull MouseEvent event) {
        Active current = active;
        if (current == null) {
            return;
        }
        boolean over = SplittabRestoreOverlay.isPointerOverDropTarget(current.editor, screenPoint(event));
        SplittabRestoreOverlay.setDropTargetHighlighted(current.editor, over);
    }

    static boolean tryCompleteDrop(@NotNull MouseEvent event) {
        return tryCompleteDropAtScreen(screenPoint(event));
    }

    @TestOnly
    static boolean tryCompleteActiveDropForTest(@NotNull FileEditor editor) {
        Active current = active;
        if (current == null || current.editor != editor) {
            return false;
        }
        ComponentSubtabEditorSplitNavigation.createSplit(
                current.project,
                current.anchorFile,
                current.dragFile
        );
        end();
        return true;
    }

    static boolean tryCompleteDropAtScreen(@NotNull Point screenPoint) {
        Active current = active;
        if (current == null) {
            return false;
        }
        if (!SplittabRestoreOverlay.isPointerOverDropTarget(current.editor, screenPoint)) {
            return false;
        }
        if (!ComponentSubtabNavigation.sameSubtabGroup(current.anchorFile, current.dragFile)) {
            end();
            return false;
        }
        ComponentSubtabEditorSplitNavigation.createSplit(
                current.project,
                current.anchorFile,
                current.dragFile
        );
        end();
        return true;
    }

    static void end() {
        Active current = active;
        active = null;
        if (current == null) {
            return;
        }
        SplittabRestoreOverlay.setDropTargetHighlighted(current.editor, false);
        SplittabRestoreOverlay.clearSubtabDragDropPresentation(current.project, current.editor);
    }

    static boolean isActive() {
        return active != null;
    }

    static boolean keepsDragInEditor(@NotNull MouseEvent event, @NotNull ComponentSubtabBarPanel barPanel) {
        Active current = active;
        if (current == null || current.barPanel != barPanel) {
            return false;
        }
        Point screen = screenPoint(event);
        return SplittabRestoreOverlay.isPointerOverDropTarget(current.editor, screen)
                || pointerInBar(current.barPanel, event);
    }

    static boolean isSplittabPairingDrag(
            @NotNull ComponentSubtabReorderStripHost barPanel,
            @Nullable VirtualFile dragFile,
            @NotNull java.util.Map<VirtualFile, ? extends javax.swing.JToggleButton> buttonsByFile
    ) {
        if (!(barPanel instanceof ComponentSubtabBarPanel) || dragFile == null) {
            return false;
        }
        javax.swing.JToggleButton button = buttonsByFile.get(dragFile);
        return button != null && !button.isSelected();
    }

    private static @NotNull Point screenPoint(@NotNull MouseEvent event) {
        return event.getLocationOnScreen();
    }

    private static boolean pointerInBar(
            @NotNull ComponentSubtabBarPanel barPanel,
            @NotNull MouseEvent event
    ) {
        Point inBar = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), barPanel);
        return barPanel.contains(inBar);
    }
}
