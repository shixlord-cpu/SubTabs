package de.sasbe.subtabs;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.HeavyPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;

/**
 * Mirrors {@code NavigationHoverDemoStandalone.java}: Java Navigate to Declaration must resolve
 * {@code familiaNavigationDemoTarget} when the file is on a marked source root.
 */
public class NavigationHoverDemoGotoDeclarationTest extends HeavyPlatformTestCase {
    public void testStandaloneDemoFileResolvesFamiliaNavigationDemoTarget() throws Exception {
        java.nio.file.Path standalone = java.nio.file.Path.of(
                "demo-project/sidetabs-examples/java/NavigationHoverDemoStandalone.java"
        );
        assertTrue("missing demo java file: " + standalone, java.nio.file.Files.isRegularFile(standalone));
        String text = java.nio.file.Files.readString(standalone);

        Module module = getModule();
        VirtualFile sourceRoot = WriteAction.computeAndWait(() -> getVirtualFile(createTempDir("java-src")));
        WriteAction.runAndWait(() -> ModuleRootModificationUtil.updateModel(module, model -> {
            var entry = model.getContentEntries().length == 0
                    ? model.addContentEntry(sourceRoot)
                    : model.getContentEntries()[0];
            if (entry.getSourceFolders().length == 0) {
                entry.addSourceFolder(sourceRoot, false);
            }
        }));

        VirtualFile shopDir = WriteAction.computeAndWait(() -> sourceRoot.createChildDirectory(this, "shop"));
        VirtualFile file = WriteAction.computeAndWait(() -> {
            VirtualFile created = shopDir.createChildData(this, "NavigationHoverDemoStandalone.java");
            Document document = FileDocumentManager.getInstance().getDocument(created);
            assertNotNull(document);
            document.setText(text);
            return created;
        });

        waitUntilSmart();

        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);
        assertNotNull(psiFile);
        assertEquals(Language.findLanguageByID("JAVA"), psiFile.getLanguage());

        Document document = FileDocumentManager.getInstance().getDocument(file);
        assertNotNull(document);
        Editor editor = EditorFactory.getInstance().createEditor(document, getProject());
        try {
            int offset = document.getText().indexOf("familiaNavigationDemoTarget();");
            assertTrue(offset > 0);
            PsiElement target = TargetElementUtil.getInstance().findTargetElement(
                    editor,
                    TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED,
                    offset
            );
            assertNotNull(
                    "Navigate to Declaration must resolve familiaNavigationDemoTarget (same symptom as IDE message when null)",
                    target
            );
        } finally {
            EditorFactory.getInstance().releaseEditor(editor);
        }
    }

    private void waitUntilSmart() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 60_000L;
        while (DumbService.getInstance(getProject()).isDumb() && System.currentTimeMillis() < deadline) {
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
            Thread.sleep(25);
        }
        assertFalse("project stayed dumb; indexes not ready", DumbService.getInstance(getProject()).isDumb());
    }
}
