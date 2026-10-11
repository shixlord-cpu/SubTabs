package com.zayax.tabz;



import com.intellij.openapi.actionSystem.ActionUpdateThread;

import com.intellij.openapi.actionSystem.AnAction;

import com.intellij.openapi.actionSystem.AnActionEvent;

import com.intellij.openapi.project.DumbAware;

import org.jetbrains.annotations.NotNull;



final class SubtabGroupResetColorAction extends AnAction implements DumbAware {

    SubtabGroupResetColorAction() {

        super("Reassign group color");

    }



    @Override

    public @NotNull ActionUpdateThread getActionUpdateThread() {

        return ActionUpdateThread.EDT;

    }



    @Override

    public void update(@NotNull AnActionEvent event) {

        TabzContext.updateColorAction(event);

    }



    @Override

    public void actionPerformed(@NotNull AnActionEvent event) {

        String key = TabzContext.colorStorageKey(event);

        if (key == null) {

            return;

        }



        TabzSettings.getInstance().getGroupColorHexes().remove(key);

        SubtabGroupColors.ensureColor(key);

        TabzPresentation.refreshGroupColors();

    }

}

