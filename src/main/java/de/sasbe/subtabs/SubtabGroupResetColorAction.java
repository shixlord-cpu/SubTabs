package de.sasbe.subtabs;



import com.intellij.openapi.actionSystem.ActionUpdateThread;

import com.intellij.openapi.actionSystem.AnAction;

import com.intellij.openapi.actionSystem.AnActionEvent;

import com.intellij.openapi.project.DumbAware;

import org.jetbrains.annotations.NotNull;



final class SubtabGroupResetColorAction extends AnAction implements DumbAware {

    SubtabGroupResetColorAction() {

        super("Gruppenfarbe neu zuweisen");

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

        String key = SubtabFamiliaContext.colorStorageKey(event);

        if (key == null) {

            return;

        }



        SubtabsSettings.getInstance().getGroupColorHexes().remove(key);

        SubtabGroupColors.ensureColor(key);

        SubtabsPresentation.refreshGroupColors();

    }

}

