package com.zayax.tabz;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.event.MouseEvent;

interface ComponentSubtabReorderStripHost {
    void clearReorderPreview();

    void updateReorderDragState(
            int index,
            @Nullable VirtualFile draggedFile,
            @Nullable Point pointerInTabsHost
    );

    @NotNull Point pointerInTabsHostFromEvent(@NotNull MouseEvent event);

    int resolveReorderDropIndex(@NotNull Point pointerInTabsHost, @NotNull VirtualFile draggedFile);

    int tabIndexForFile(@NotNull VirtualFile file);
}
