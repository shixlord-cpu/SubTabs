package de.sasbe.subtabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.startup.StartupManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class DemoReplayStartupActivity implements ProjectActivity {
    @Override
    public @Nullable Object execute(
            @NotNull Project project,
            @NotNull Continuation<? super Unit> continuation
    ) {
        if (!DemoReplayConfig.isRecordingEnabled()) {
            return Unit.INSTANCE;
        }
        if (!DemoProjectDetection.isDemoProject(project) && !DemoReplayConfig.allowNonDemoProjectForTests) {
            return Unit.INSTANCE;
        }
        StartupManager.getInstance(project).runAfterOpened(() -> {
            DemoReplayRecorder recorder = DemoReplayRecorder.getInstance(project);
            if (recorder != null) {
                recorder.captureNow("startup");
            }
        });
        return Unit.INSTANCE;
    }
}
