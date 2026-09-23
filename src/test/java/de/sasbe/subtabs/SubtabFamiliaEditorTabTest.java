package de.sasbe.subtabs;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SubtabFamiliaEditorTabTest extends HeavyPlatformTestCase {
    public void testSingleFileEditorTabOffersFamiliaRevealOnly() throws Exception {
        SubtabsSettings.getInstance().setSubtabsActive(true);
        SubtabGroupColors.setEnabled(true);

        VirtualFile readme = WriteAction.computeAndWait(() ->
                getVirtualFile(createTempDir("app")).createChildData(this, "readme.md")
        );
        assertNull(ComponentRelatedFiles.find(readme));

        AnActionEvent event = editorTabEvent(readme);
        SubtabFamiliaActionGroup familiaGroup = new SubtabFamiliaActionGroup();
        familiaGroup.update(event);
        assertTrue(event.getPresentation().isEnabledAndVisible());

        SubtabRevealInProjectViewAction revealAction = new SubtabRevealInProjectViewAction();
        revealAction.update(event);
        assertTrue(event.getPresentation().isEnabledAndVisible());

        SubtabGroupChangeColorAction changeColorAction = new SubtabGroupChangeColorAction();
        changeColorAction.update(event);
        assertFalse(event.getPresentation().isEnabledAndVisible());

        SubtabGroupResetColorAction resetColorAction = new SubtabGroupResetColorAction();
        resetColorAction.update(event);
        assertFalse(event.getPresentation().isEnabledAndVisible());
    }

    public void testFolderGroupFilesGetSubtabsWhenMultipleFilesShareFolder() throws Exception {
        SubtabsSettings.getInstance().setSubtabsActive(true);

        VirtualFile dir = getVirtualFile(createTempDir("docs"));
        WriteAction.runAndWait(() -> {
            dir.createChildData(this, "readme.md");
            dir.createChildData(this, "notes.txt");
        });
        VirtualFile license = WriteAction.computeAndWait(() -> dir.createChildData(this, "license.md"));

        ComponentRelatedFiles.Match match = ComponentRelatedFiles.find(license);
        assertNotNull(match);
        assertEquals(3, match.relatedFiles().size());
        assertTrue(CustomSubtabRuleMatcher.isFolderGroupKey(match.baseName()));
        assertNotNull(ComponentSubtabGroupRegistry.getInstance(getProject()).getOrCreateGroup(license));
    }

    public void testGroupedProjectViewFileOffersFamiliaColorsWhenEnabled() throws Exception {
        SubtabsSettings.getInstance().setFamiliaEnabled(true);
        SubtabsSettings.getInstance().setSubtabsActive(true);
        SubtabGroupColors.setEnabled(true);

        VirtualFile dir = getVirtualFile(createTempDir("app"));
        WriteAction.runAndWait(() -> {
            dir.createChildData(this, "header.component.ts");
            dir.createChildData(this, "header.component.scss");
        });
        VirtualFile html = WriteAction.computeAndWait(() -> dir.createChildData(this, "header.component.html"));
        assertTrue(SubtabFamiliaContext.belongsToSubtabGroup(html));

        AnActionEvent event = projectViewFileEvent(html);
        SubtabFamiliaActionGroup familiaGroup = new SubtabFamiliaActionGroup();
        familiaGroup.update(event);
        assertTrue(event.getPresentation().isEnabledAndVisible());

        SubtabGroupChangeColorAction changeColorAction = new SubtabGroupChangeColorAction();
        changeColorAction.update(event);
        assertTrue(event.getPresentation().isEnabledAndVisible());
        assertNotNull(SubtabFamiliaContext.colorStorageKey(event));
    }

    public void testGroupedProjectViewFileHidesFamiliaWhenGroupColorsDisabled() throws Exception {
        SubtabsSettings.getInstance().setFamiliaEnabled(true);
        SubtabsSettings.getInstance().setSubtabsActive(true);
        SubtabGroupColors.setEnabled(false);

        VirtualFile dir = getVirtualFile(createTempDir("app"));
        WriteAction.runAndWait(() -> {
            dir.createChildData(this, "footer.component.ts");
            dir.createChildData(this, "footer.component.scss");
        });
        VirtualFile html = WriteAction.computeAndWait(() -> dir.createChildData(this, "footer.component.html"));

        AnActionEvent event = projectViewFileEvent(html);
        SubtabFamiliaActionGroup familiaGroup = new SubtabFamiliaActionGroup();
        familiaGroup.update(event);
        assertFalse(event.getPresentation().isEnabledAndVisible());
    }

    public void testGroupedEditorTabStillOffersGroupColors() throws Exception {
        SubtabsSettings.getInstance().setSubtabsActive(true);
        SubtabGroupColors.setEnabled(true);

        VirtualFile dir = getVirtualFile(createTempDir("app"));
        WriteAction.runAndWait(() -> {
            dir.createChildData(this, "header.component.ts");
            dir.createChildData(this, "header.component.scss");
        });
        VirtualFile html = WriteAction.computeAndWait(() -> dir.createChildData(this, "header.component.html"));
        assertNotNull(ComponentRelatedFiles.find(html));

        AnActionEvent event = editorTabEvent(html);
        SubtabGroupChangeColorAction changeColorAction = new SubtabGroupChangeColorAction();
        changeColorAction.update(event);
        assertTrue(event.getPresentation().isEnabledAndVisible());
    }

    private @NotNull AnActionEvent editorTabEvent(@NotNull VirtualFile file) {
        DataContext context = new DataContext() {
            @Override
            public @Nullable Object getData(@NotNull String dataId) {
                return resolveData(dataId, file);
            }
        };
        return AnActionEvent.createFromDataContext(ActionPlaces.EDITOR_TAB_POPUP, null, context);
    }

    private @NotNull AnActionEvent projectViewFileEvent(@NotNull VirtualFile file) {
        DataContext context = new DataContext() {
            @Override
            public @Nullable Object getData(@NotNull String dataId) {
                return resolveData(dataId, file);
            }
        };
        return AnActionEvent.createFromDataContext(ActionPlaces.PROJECT_VIEW_POPUP, null, context);
    }

    private @Nullable Object resolveData(@NotNull String dataId, @NotNull VirtualFile file) {
        if (CommonDataKeys.PROJECT.is(dataId)) {
            return getProject();
        }
        if (CommonDataKeys.VIRTUAL_FILE.is(dataId)) {
            return file;
        }
        return null;
    }
}
