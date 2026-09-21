package de.sasbe.subtabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;

final class SubtabsLegacyRefreshSupport {
    private SubtabsLegacyRefreshSupport() {
    }

    static void applyEditorSettingsChangeLegacy(@NotNull Project project) {
        ComponentRelatedFilesCache.getInstance(project).clear();
        ComponentSubtabGroupRegistry.getInstance(project).clearGroups();
        ComponentSubtabsManager.applyPresentationState(project);
        ComponentSubtabsManager.refreshAppearance(project);
        ComponentSubtabsFileEditorListener.attachToAlreadyOpenFiles(project);
        ComponentSubtabMainTabSelectPopup.installOn(project);
        ComponentSubtabMainTabColors.refresh(project);
    }

    static void applyFullSettingsChangeLegacy() {
        ComponentFileNaming.invalidateRulesCache();
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed()) {
                continue;
            }
            applyEditorSettingsChangeLegacy(project);
        }
        SubtabsPresentation.refreshProjectViews();
    }
}
