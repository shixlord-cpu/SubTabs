package de.sasbe.subtabs;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.FileStatusListener;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vcs.changes.ChangeListAdapter;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.Alarm;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTree;

final class ComponentSubtabsDocumentListener implements DocumentListener, FileDocumentManagerListener, CommandListener, Disposable {
    private static final Key<ComponentSubtabsDocumentListener> INSTALLED = Key.create("componentSubtabs.documentListener");
    private static final int DEFERRED_REFRESH_DELAY_MS = 100;

    private final @NotNull Project project;
    private final @NotNull Alarm refreshAlarm;

    private ComponentSubtabsDocumentListener(@NotNull Project project) {
        this.project = project;
        this.refreshAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
    }

    static void install(@NotNull Project project) {
        if (project.isDisposed() || project.getUserData(INSTALLED) != null) {
            return;
        }

        ComponentSubtabsDocumentListener listener = new ComponentSubtabsDocumentListener(project);
        project.putUserData(INSTALLED, listener);
        Disposer.register(project, listener);

        var editorFactory = com.intellij.openapi.editor.EditorFactory.getInstance();
        editorFactory.getEventMulticaster().addDocumentListener(listener, listener);
        editorFactory.addEditorFactoryListener(new EditorFactoryListener() {
            @Override
            public void editorReleased(@NotNull EditorFactoryEvent event) {
                listener.scheduleDeferredRefresh();
            }
        }, listener);

        project.getMessageBus()
                .connect(listener)
                .subscribe(FileDocumentManagerListener.TOPIC, listener);

        ApplicationManager.getApplication().getMessageBus()
                .connect(listener)
                .subscribe(CommandListener.TOPIC, listener);

        if (!project.isDefault()) {
            FileStatusManager.getInstance(project).addFileStatusListener(new FileStatusListener() {
                @Override
                public void fileStatusesChanged() {
                    listener.scheduleDeferredRefresh();
                }

                @Override
                public void fileStatusChanged(@NotNull VirtualFile virtualFile) {
                    listener.refreshModifiedStateForFile(virtualFile);
                    listener.scheduleDeferredRefresh();
                }
            }, listener);
            project.getMessageBus()
                    .connect(listener)
                    .subscribe(FileStatusListener.TOPIC, new FileStatusListener() {
                        @Override
                        public void fileStatusesChanged() {
                            listener.scheduleDeferredRefresh();
                        }

                        @Override
                        public void fileStatusChanged(@NotNull VirtualFile virtualFile) {
                            listener.refreshModifiedStateForFile(virtualFile);
                            listener.scheduleDeferredRefresh();
                        }
                    });
            project.getMessageBus()
                    .connect(listener)
                    .subscribe(com.intellij.problems.ProblemListener.TOPIC, new com.intellij.problems.ProblemListener() {
                        @Override
                        public void problemsAppeared(@NotNull VirtualFile file) {
                            listener.refreshModifiedStateForFile(file);
                        }

                        @Override
                        public void problemsChanged(@NotNull VirtualFile file) {
                            listener.refreshModifiedStateForFile(file);
                        }

                        @Override
                        public void problemsDisappeared(@NotNull VirtualFile file) {
                            listener.refreshModifiedStateForFile(file);
                        }
                    });
            project.getMessageBus()
                    .connect(listener)
                    .subscribe(
                            com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC,
                            new com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.DaemonListener() {
                                @Override
                                public void daemonFinished() {
                                    listener.scheduleDeferredRefresh();
                                }
                            }
                    );
            ChangeListManager.getInstance(project).addChangeListListener(new ChangeListAdapter() {
                @Override
                public void changeListUpdateDone() {
                    listener.scheduleDeferredRefresh();
                }
            }, listener);
        }
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        refreshModifiedStateForDocument(event.getDocument());
        scheduleDeferredRefresh();
    }

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        refreshModifiedStateForDocument(document);
        scheduleDeferredRefresh();
    }

    @Override
    public void beforeAllDocumentsSaving() {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        scheduleDeferredRefresh();
    }

    @Override
    public void unsavedDocumentsDropped() {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        scheduleDeferredRefresh();
    }

    @Override
    public void commandFinished(@NotNull CommandEvent event) {
        if (!SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        scheduleDeferredRefresh();
    }

    void scheduleRefresh() {
        scheduleDeferredRefresh();
    }

    private void refreshModifiedStateForDocument(@NotNull Document document) {
        if (project.isDisposed()) {
            return;
        }
        Runnable update = () -> {
            if (!project.isDisposed()) {
                ComponentSubtabsManager.refreshModifiedStateForDocument(project, document);
                SidetabsManager.refreshForDocument(project, document);
            }
        };
        if (ApplicationManager.getApplication().isDispatchThread()) {
            update.run();
        } else {
            ApplicationManager.getApplication().invokeLater(update);
        }
    }

    private void refreshModifiedStateForFile(@NotNull VirtualFile file) {
        if (project.isDisposed()) {
            return;
        }
        Runnable update = () -> {
            if (!project.isDisposed()) {
                ComponentSubtabsManager.refreshModifiedStateForFile(project, file);
                SidetabsManager.refreshPresentationForFile(project, file);
            }
        };
        if (ApplicationManager.getApplication().isDispatchThread()) {
            update.run();
        } else {
            ApplicationManager.getApplication().invokeLater(update);
        }
    }

    private void scheduleDeferredRefresh() {
        if (project.isDisposed() || !SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }
        refreshAlarm.cancelAllRequests();
        refreshAlarm.addRequest(this::refreshDeferredPresentation, DEFERRED_REFRESH_DELAY_MS);
    }

    private void refreshDeferredPresentation() {
        if (project.isDisposed() || !SubtabsSettings.getInstance().isFamiliaEnabled()) {
            return;
        }

        if (SubtabsSettings.getInstance().isSubtabsActive()) {
            ComponentSubtabsManager.refreshPresentationStates(project);
            ComponentSubtabMainTabSelectPopup.refreshPopupPresentation(project);
        }
        if (SubtabsSettings.getInstance().isSidetabsActive()) {
            SidetabsManager.applyPresentationState(project);
        }

        if (SubtabProjectViewGrouping.isEnabled()) {
            ProjectView.getInstance(project).refresh();
            var toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
            if (toolWindow != null) {
                for (JTree tree : UIUtil.findComponentsOfType(toolWindow.getComponent(), JTree.class)) {
                    tree.repaint();
                }
            }
        }

        var toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
        if (toolWindow != null) {
            for (JTree tree : UIUtil.findComponentsOfType(toolWindow.getComponent(), JTree.class)) {
                SubtabGroupLocationHover.refreshPopupPresentation(project, tree);
            }
        }
    }

    @Override
    public void dispose() {
        refreshAlarm.cancelAllRequests();
        project.putUserData(INSTALLED, null);
    }
}
