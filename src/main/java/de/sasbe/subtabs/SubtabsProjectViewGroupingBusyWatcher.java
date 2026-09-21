package de.sasbe.subtabs;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTree;
import javax.swing.Timer;
import java.util.Arrays;
import java.util.List;

/**
 * Keeps the project-view grouping spinner visible until the project tree stops changing.
 */
@Service(Service.Level.PROJECT)
final class SubtabsProjectViewGroupingBusyWatcher implements Disposable {
    private static final int SETTLE_QUIET_MS = 450;
    private static final int POLL_MS = 100;
    private static final int MAX_WAIT_MS = 15000;
    private static final int UNIT_TEST_SETTLE_QUIET_MS = 80;
    private static final int UNIT_TEST_POLL_MS = 20;
    private static final int UNIT_TEST_MAX_WAIT_MS = 2000;

    private final @NotNull Project project;
    private @Nullable Timer pollTimer;
    private @Nullable Runnable pendingCompletion;
    private long lastChangeAtMs;
    private long deadlineAtMs;
    private long[] lastSnapshot = new long[0];

    SubtabsProjectViewGroupingBusyWatcher(@NotNull Project project) {
        this.project = project;
    }

    static @NotNull SubtabsProjectViewGroupingBusyWatcher getInstance(@NotNull Project project) {
        return project.getService(SubtabsProjectViewGroupingBusyWatcher.class);
    }

    void awaitSettled(@NotNull Runnable onComplete) {
        cancelPending();
        pendingCompletion = onComplete;
        long now = System.currentTimeMillis();
        lastChangeAtMs = now;
        deadlineAtMs = now + maxWaitMs();
        lastSnapshot = snapshotProjectTrees();

        pollTimer = new Timer(pollIntervalMs(), event -> pollForSettledState());
        pollTimer.start();
    }

    private void pollForSettledState() {
        if (project.isDisposed()) {
            completePending();
            return;
        }

        long[] snapshot = snapshotProjectTrees();
        long now = System.currentTimeMillis();
        if (!Arrays.equals(snapshot, lastSnapshot)) {
            lastSnapshot = snapshot;
            lastChangeAtMs = now;
        }

        if (now - lastChangeAtMs >= settleQuietMs() || now >= deadlineAtMs) {
            completePending();
        }
    }

    void cancelPending() {
        cancel();
        pendingCompletion = null;
    }

    private void completePending() {
        cancel();
        Runnable completion = pendingCompletion;
        pendingCompletion = null;
        if (completion == null) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(completion, project.getDisposed());
    }

    private void cancel() {
        if (pollTimer != null) {
            pollTimer.stop();
            pollTimer = null;
        }
    }

    private long[] snapshotProjectTrees() {
        List<JTree> trees = SubtabsProjectViewGroupingOverlay.projectViewTrees(project);
        long[] snapshot = new long[trees.size() * 4 + 1];
        snapshot[0] = trees.size();
        int index = 1;
        for (JTree tree : trees) {
            snapshot[index++] = System.identityHashCode(tree);
            snapshot[index++] = tree.getRowCount();
            Object root = tree.getModel().getRoot();
            snapshot[index++] = root == null ? 0 : tree.getModel().getChildCount(root);
            snapshot[index++] = tree.isShowing() ? 1 : 0;
        }
        return snapshot;
    }

    private static int settleQuietMs() {
        return unitTestMode() ? UNIT_TEST_SETTLE_QUIET_MS : SETTLE_QUIET_MS;
    }

    private static int pollIntervalMs() {
        return unitTestMode() ? UNIT_TEST_POLL_MS : POLL_MS;
    }

    private static int maxWaitMs() {
        return unitTestMode() ? UNIT_TEST_MAX_WAIT_MS : MAX_WAIT_MS;
    }

    private static boolean unitTestMode() {
        return ApplicationManager.getApplication().isUnitTestMode();
    }

    @Override
    public void dispose() {
        cancel();
        pendingCompletion = null;
    }
}
