package com.zayax.tabz;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.util.Alarm;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class DemoLintWarmupActivity implements ProjectActivity {
    @Override
    public @Nullable Object execute(
            @NotNull Project project,
            @NotNull Continuation<? super Unit> continuation
    ) {
        if (!DemoProjectDetection.isDemoProject(project)) {
            return Unit.INSTANCE;
        }
        ApplicationManager.getApplication().invokeLater(
                () -> scheduleLintWarmup(project),
                ModalityState.nonModal()
        );
        return Unit.INSTANCE;
    }

    private static void scheduleLintWarmup(@NotNull Project project) {
        Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, project);
        ApplicationManager.getApplication().invokeLater(
                () -> restartAnalysisAndRefreshPresentation(project),
                ModalityState.nonModal()
        );
        alarm.addRequest(() -> restartAnalysisAndRefreshPresentation(project), 4_000);
        alarm.addRequest(() -> restartAnalysisAndRefreshPresentation(project), 12_000);
    }

    private static void restartAnalysisAndRefreshPresentation(@NotNull Project project) {
        if (project.isDisposed() || !DemoProjectDetection.isDemoProject(project)) {
            return;
        }
        if (!TabzSettings.getInstance().isTabzEnabled()) {
            return;
        }
        DaemonCodeAnalyzer.getInstance(project).restart(project);
        if (TabzSettings.getInstance().isSubtabsActive()) {
            ComponentSubtabsManager.refreshPresentationStates(project);
            ComponentSubtabMainTabSelectPopup.refreshPopupPresentation(project);
        }
        if (TabzSettings.getInstance().isSidetabsActive()) {
            SidetabsManager.applyPresentationState(project);
        }
    }
}
