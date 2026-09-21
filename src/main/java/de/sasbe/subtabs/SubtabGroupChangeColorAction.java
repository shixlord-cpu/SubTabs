package de.sasbe.subtabs;



import com.intellij.openapi.actionSystem.ActionUpdateThread;

import com.intellij.openapi.actionSystem.AnAction;

import com.intellij.openapi.actionSystem.AnActionEvent;

import com.intellij.openapi.application.ApplicationManager;

import com.intellij.openapi.project.DumbAware;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;



import java.awt.Color;



final class SubtabGroupChangeColorAction extends AnAction implements DumbAware {

    SubtabGroupChangeColorAction() {

        super("Gruppenfarbe ändern…");

    }



    @Override

    public @NotNull ActionUpdateThread getActionUpdateThread() {

        return ActionUpdateThread.EDT;

    }



    @Override

    public void update(@NotNull AnActionEvent event) {

        SubtabFamiliaContext.updateColorAction(event);

    }



    @Override

    public void actionPerformed(@NotNull AnActionEvent event) {

        Project project = event.getProject();

        String key = SubtabFamiliaContext.colorStorageKey(event);

        if (project == null || key == null) {

            return;

        }



        Color current = SubtabGroupColors.colorForKey(key);

        ApplicationManager.getApplication().invokeLater(() -> {

            if (project.isDisposed()) {

                return;

            }

            Color chosen = SubtabGroupColorPicker.choose(project, current);

            if (chosen == null) {

                return;

            }

            SubtabGroupColors.setColor(key, chosen);

            SubtabsPresentation.refreshGroupColors();

        });

    }

}

