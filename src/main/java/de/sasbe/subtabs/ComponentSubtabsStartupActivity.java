package de.sasbe.subtabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ComponentSubtabsStartupActivity implements ProjectActivity {
    @Override
    public @Nullable Object execute(
            @NotNull Project project,
            @NotNull Continuation<? super Unit> continuation
    ) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return Unit.INSTANCE;
        }
        ComponentSubtabsFileEditorListener.attachToAlreadyOpenFiles(project);
        ComponentSubtabMainTabSelectPopup.installOn(project);
        ComponentSubtabMainTabColors.refresh(project);
        SubtabGroupTreeControl.installOn(project);
        ComponentSubtabProjectViewEditorHover.installOn(project);
        SubtabsProjectViewGroupingOverlay.installOn(project);
        if (SubtabColorDiagnostic.isEnabled()) {
            SubtabColorDiagnostic.schedule(project);
        }
        return Unit.INSTANCE;
    }
}
