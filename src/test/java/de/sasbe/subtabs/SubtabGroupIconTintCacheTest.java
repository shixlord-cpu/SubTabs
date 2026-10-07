package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.HeavyPlatformTestCase;

import javax.swing.Icon;
import java.awt.Color;

public class SubtabGroupIconTintCacheTest extends HeavyPlatformTestCase {
    public void testTintedIconIsStableForSameFileAndColor() throws Exception {
        SubtabsSettings.getInstance().setFamiliaEnabled(true);
        SubtabGroupColors.setEnabled(true);
        VirtualFile dir = getVirtualFile(createTempDir("icons"));
        WriteAction.runAndWait(() -> {
            dir.createChildData(this, "header.component.ts");
            dir.createChildData(this, "header.component.scss");
        });
        VirtualFile html = WriteAction.computeAndWait(() -> dir.createChildData(this, "header.component.html"));
        ComponentSubtabGroupRegistry.getInstance(getProject()).getOrCreateGroup(html);

        Color color = SubtabGroupColors.colorForFile(html);
        assertNotNull(color);

        SubtabGroupIconTintCache.clear(getProject());
        Icon first = SubtabGroupIconTintCache.tintedFileIcon(
                getProject(),
                html,
                color,
                Iconable.ICON_FLAG_READ_STATUS
        );
        Icon second = SubtabGroupIconTintCache.tintedFileIcon(
                getProject(),
                html,
                color,
                Iconable.ICON_FLAG_READ_STATUS
        );
        assertNotNull(first);
        assertSame(first, second);
    }

    public void testClearForcesNewIconInstance() throws Exception {
        SubtabsSettings.getInstance().setFamiliaEnabled(true);
        SubtabGroupColors.setEnabled(true);
        VirtualFile dir = getVirtualFile(createTempDir("icons"));
        WriteAction.runAndWait(() -> {
            dir.createChildData(this, "footer.component.ts");
            dir.createChildData(this, "footer.component.scss");
        });
        VirtualFile html = WriteAction.computeAndWait(() -> dir.createChildData(this, "footer.component.html"));
        ComponentSubtabGroupRegistry.getInstance(getProject()).getOrCreateGroup(html);
        Color color = SubtabGroupColors.colorForFile(html);
        assertNotNull(color);

        Icon before = SubtabGroupIconTintCache.tintedFileIcon(
                getProject(),
                html,
                color,
                Iconable.ICON_FLAG_READ_STATUS
        );
        SubtabGroupIconTintCache.clear(getProject());
        Icon after = SubtabGroupIconTintCache.tintedFileIcon(
                getProject(),
                html,
                color,
                Iconable.ICON_FLAG_READ_STATUS
        );
        assertNotNull(before);
        assertNotNull(after);
        assertNotSame(before, after);
    }
}
