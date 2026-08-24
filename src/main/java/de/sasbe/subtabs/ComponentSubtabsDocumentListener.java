package de.sasbe.subtabs;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandEvent;
import com.intellij.openapi.command.CommandListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.Alarm;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTree;

final class ComponentSubtabsDocumentListener implements DocumentListener, FileDocumentManagerListener, CommandListener, Disposable {
    private static final Key<ComponentSubtabsDocumentListener> INSTALLED = Key.create("componentSubtabs.documentListener");
    private static final int REFRESH_DELAY_MS = 100;

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
                listener.scheduleRefresh();
            }
        }, listener);

        project.getMessageBus()
                .connect(listener)
                .subscribe(FileDocumentManagerListener.TOPIC, listener);

        ApplicationManager.getApplication().getMessageBus()
                .connect(listener)
                .subscribe(CommandListener.TOPIC, listener);
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        scheduleRefresh();
    }

    @Override
    public void beforeDocumentSaving(@NotNull com.intellij.openapi.editor.Document document) {
        scheduleRefresh();
    }

    @Override
    public void beforeAllDocumentsSaving() {
        scheduleRefresh();
    }

    @Override
    public void unsavedDocumentsDropped() {
        scheduleRefresh();
    }

    @Override
    public void commandFinished(@NotNull CommandEvent event) {
        scheduleRefresh();
    }

    private void scheduleRefresh() {
        if (project.isDisposed()) {
            return;
        }
        refreshAlarm.cancelAllRequests();
        refreshAlarm.addRequest(this::refreshPresentation, REFRESH_DELAY_MS);
    }

    private void refreshPresentation() {
        if (project.isDisposed()) {
            return;
        }

        if (SubtabsSettings.getInstance().isSubtabsActive()) {
            ComponentSubtabsManager.refreshPresentationStates(project);
            ComponentSubtabMainTabSelectPopup.refreshPopupPresentation(project);
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
