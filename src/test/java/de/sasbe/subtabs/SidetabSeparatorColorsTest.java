package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.testFramework.HeavyPlatformTestCase;

import java.awt.Color;
import java.nio.charset.StandardCharsets;

public class SidetabSeparatorColorsTest extends RealEditorWindowTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setFamiliaEnabled(true);
        settings.setGroupColorsEnabled(true);
    }

    public void testUsesDefaultSeparatorWhenGroupColorMissing() throws Exception {
        VirtualFile html = createSourceFile("plain.html");
        WriteAction.run(() -> html.setBinaryContent("<html></html>".getBytes(StandardCharsets.UTF_8)));
        assertEquals(SidetabSeparatorColors.defaultSeparatorColor(), SidetabSeparatorColors.forFile(html));
    }

    public void testUsesGroupColorWhenAvailable() throws Exception {
        VirtualFile html = createSourceFile("header.component.html");
        WriteAction.run(() -> html.setBinaryContent("<header></header>".getBytes(StandardCharsets.UTF_8)));
        createSourceFile("header.component.ts");

        String key = SubtabGroupColors.colorKey(html);
        assertNotNull(key);
        Color groupColor = new JBColor(new Color(0x2563EB), new Color(0x3B82F6));
        SubtabGroupColors.setColor(key, groupColor);

        assertEquals(groupColor, SidetabSeparatorColors.forFile(html));
    }
}
