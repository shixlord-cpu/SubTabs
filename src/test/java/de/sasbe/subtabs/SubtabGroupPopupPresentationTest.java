package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.ServiceContainerUtil;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.Dispatchers;

/**
 * Covers the popup highlighting when several main tabs of the same subtab group are open, which used
 * to highlight an arbitrary file of the group instead of the hovered tab's own file.
 */
public class SubtabGroupPopupPresentationTest extends HeavyPlatformTestCase {
    private FileEditorManagerEx manager;
    private VirtualFile htmlFile;
    private VirtualFile specFile;
    private VirtualFile styleFile;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        FileEditorManagerImpl real = new FileEditorManagerImpl(
                getProject(),
                CoroutineScopeKt.CoroutineScope(Dispatchers.getDefault())
        );
        ServiceContainerUtil.replaceService(
                getProject(), FileEditorManager.class, real, getTestRootDisposable());
        manager = FileEditorManagerEx.getInstanceEx(getProject());

        VirtualFile dir = getVirtualFile(createTempDir("app"));
        htmlFile = createChild(dir, "product-list.component.html");
        specFile = createChild(dir, "product-list.component.spec.ts");
        styleFile = createChild(dir, "product-list.component.scss");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            ComponentSubtabGroupSplitRegistry.getInstance(getProject()).clear();
        } finally {
            super.tearDown();
        }
    }

    public void testEachMainTabHighlightsItsOwnFile() {
        openTabs(htmlFile, specFile);

        SubtabGroupPopupPresentation.Context htmlTab =
                SubtabGroupPopupPresentation.forMainTab(getProject(), htmlFile);
        SubtabGroupPopupPresentation.Context specTab =
                SubtabGroupPopupPresentation.forMainTab(getProject(), specFile);

        assertTrue(SubtabGroupPopupPresentation.isHighlighted(htmlTab, htmlFile));
        assertFalse(
                "the html tab must not highlight the file of another main tab",
                SubtabGroupPopupPresentation.isHighlighted(htmlTab, specFile)
        );

        assertTrue(SubtabGroupPopupPresentation.isHighlighted(specTab, specFile));
        assertFalse(
                "the spec tab must not highlight the file of another main tab",
                SubtabGroupPopupPresentation.isHighlighted(specTab, htmlFile)
        );
    }

    public void testOtherMainTabsOfTheSameGroupAreGreyedOut() {
        openTabs(htmlFile, specFile);

        SubtabGroupPopupPresentation.Context htmlTab =
                SubtabGroupPopupPresentation.forMainTab(getProject(), htmlFile);

        assertTrue(
                "a file shown by a different main tab is open elsewhere",
                SubtabGroupPopupPresentation.isOpenElsewhere(getProject(), htmlTab, specFile)
        );
        assertFalse(
                "the tab's own file is never greyed out",
                SubtabGroupPopupPresentation.isOpenElsewhere(getProject(), htmlTab, htmlFile)
        );
        assertFalse(
                "a file that is not open at all is not greyed out",
                SubtabGroupPopupPresentation.isOpenElsewhere(getProject(), htmlTab, styleFile)
        );
    }

    public void testHighlightFollowsTheFileAfterASubtabSwitch() {
        openTabs(htmlFile);
        ComponentSubtabNavigation.switchInTabOf(getProject(), htmlFile, styleFile, true);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        SubtabGroupPopupPresentation.Context context =
                SubtabGroupPopupPresentation.forMainTab(getProject(), styleFile);

        assertTrue(SubtabGroupPopupPresentation.isHighlighted(context, styleFile));
        assertFalse(SubtabGroupPopupPresentation.isHighlighted(context, htmlFile));
    }

    public void testProjectViewGreysOutEveryOpenFile() {
        openTabs(htmlFile, specFile);

        SubtabGroupPopupPresentation.Context context = SubtabGroupPopupPresentation.Context.projectView();

        assertNull(context.primaryHighlight());
        assertTrue(SubtabGroupPopupPresentation.isOpenElsewhere(getProject(), context, htmlFile));
        assertTrue(SubtabGroupPopupPresentation.isOpenElsewhere(getProject(), context, specFile));
        assertFalse(SubtabGroupPopupPresentation.isOpenElsewhere(getProject(), context, styleFile));
    }

    private void openTabs(VirtualFile... files) {
        for (VirtualFile file : files) {
            manager.openFile(file, true);
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        }
    }

    private VirtualFile createChild(VirtualFile dir, String name) throws Exception {
        return WriteAction.computeAndWait(() -> dir.createChildData(this, name));
    }
}
