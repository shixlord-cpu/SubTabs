package com.zayax.tabz;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.startup.StartupManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ComponentSubtabStartupActivity implements ProjectActivity {
    @Override
    public @Nullable Object execute(
            @NotNull Project project,
            @NotNull Continuation<? super Unit> continuation
    ) {
        if (!TabzSettings.getInstance().isTabzEnabled()) {
            return Unit.INSTANCE;
        }
        ComponentSubtabFileEditorListener.attachToAlreadyOpenFiles(project);
        ComponentSubtabMainTabSelectPopup.installOn(project);
        ComponentSubtabMainTabColors.refresh(project);
        ComponentSubtabMainTabIcons.scheduleStartupRefresh(project);
        StartupManager.getInstance(project).runAfterOpened(() -> {
            if (project.isDisposed()) {
                return;
            }
            ComponentSubtabMainTabColors.refresh(project);
            ComponentSubtabMainTabIcons.scheduleStartupRefresh(project);
            ComponentSubtabEditorSplitNavigation.restorePersistedActiveSplittab(project);
        });
        SubtabGroupTreeControl.installOn(project);
        ComponentSubtabProjectViewEditorHover.installOn(project);
        SubtabsProjectViewGroupingOverlay.installOn(project);
        if (SubtabColorDiagnostic.isEnabled()) {
            SubtabColorDiagnostic.schedule(project);
        }
        return Unit.INSTANCE;
    }
}
