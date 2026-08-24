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
        ComponentSubtabsFileEditorListener.attachToAlreadyOpenFiles(project);
        return Unit.INSTANCE;
    }
}
