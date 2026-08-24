package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ComponentSubtabModifiedDetectionTest extends BasePlatformTestCase {
    public void testDetectsUnsavedDocumentChanges() {
        myFixture.configureByText("Main.java", "public class Main {}");
        VirtualFile file = myFixture.getFile().getVirtualFile();

        assertFalse(ComponentSubtabModifiedUi.isModified(getProject(), file));

        Document document = FileDocumentManager.getInstance().getDocument(file);
        assertNotNull(document);
        WriteAction.run(() -> document.setText("public class Main { int x; }"));

        assertTrue(ComponentSubtabModifiedUi.isModified(getProject(), file));
    }
}
