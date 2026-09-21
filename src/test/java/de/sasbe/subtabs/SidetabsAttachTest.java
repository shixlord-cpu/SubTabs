package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SidetabsAttachTest extends RealEditorWindowTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setSubtabsActive(true);
        settings.setSidetabsActive(true);
        settings.setSidetabsExpanded(true);
        settings.setSidetabLayoutMode(SidetabLayoutMode.BESIDE);
        settings.setSidetabsOnRight(true);
        settings.setShowCollapseButton(true);
        settings.setSidetabRules(SidetabRulesDefaults.createDefaults());
        ComponentSubtabsDocumentListener.install(getProject());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            SubtabsSettings settings = SubtabsSettings.getInstance();
            settings.setSidetabsActive(false);
            settings.setSidetabsExpanded(true);
            settings.setSidetabLayoutMode(SidetabLayoutMode.BESIDE);
            settings.setSidetabsOnRight(true);
        } finally {
            super.tearDown();
        }
    }

    public void testAttachesHeadAndBodyTabsToTheRightOfTheEditor() throws Exception {
        VirtualFile file = createSourceFile("page.html");
        WriteAction.run(() -> file.setBinaryContent("""
                <html>
                <head><title>Hi</title></head>
                <body><p>Hello</p></body>
                </html>
                """.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        SidetabBarPanel panel = panelFor(file);
        assertEquals(
                "SideTabs must reserve space below the collapse icons",
                SidetabIconLayout.besideColumnTopReserve(),
                panel.topIconReserve()
        );
        assertTrue(panel.sections().stream().anyMatch(section -> "Body".equals(section.name())));

        FileEditor editor = editorFor(file);
        assertNotNull(editor);
        assertSame(panel, editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY));
        assertTrue(panel.getParent() instanceof SidetabEditorHost);
        SidetabEditorHost host = (SidetabEditorHost) panel.getParent();
        assertSame(panel, ((BorderLayout) host.getLayout()).getLayoutComponent(BorderLayout.EAST));
        Component center = ((BorderLayout) host.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        assertNotNull(center);
        assertTrue(
                "SideTabs must wrap the editor composite, not stretch the SubTabs bar",
                javax.swing.SwingUtilities.isDescendingFrom(editor.getComponent(), center)
        );
    }

    public void testSidetabHoverHighlightsCoveredEditorLines() throws Exception {
        SubtabsSettings.getInstance().setHoverViewEnabled(true);
        VirtualFile file = createSourceFile("page.html");
        WriteAction.run(() -> file.setBinaryContent("""
                <html>
                <head><title>Hi</title></head>
                <body><p>Hello</p></body>
                </html>
                """.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        FileEditor fileEditor = editorFor(file);
        assertTrue(fileEditor instanceof TextEditor);
        Editor editor = ((TextEditor) fileEditor).getEditor();

        SidetabBarPanel panel = panelFor(file);
        SidetabSection bodySection = panel.sections().stream()
                .filter(section -> "Body".equals(section.name()))
                .findFirst()
                .orElseThrow();
        JToggleButton bodyButton = buttonNamed(panel, "Body");
        assertNotNull(bodyButton);

        assertFalse(SidetabEditorHover.isActive(editor));
        bodyButton.dispatchEvent(new MouseEvent(
                bodyButton,
                MouseEvent.MOUSE_ENTERED,
                System.currentTimeMillis(),
                0,
                4,
                4,
                0,
                false
        ));
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertTrue(SidetabEditorHover.isActive(editor));
        assertTrue(hasHoverForSection(editor, bodySection));

        bodyButton.dispatchEvent(new MouseEvent(
                bodyButton,
                MouseEvent.MOUSE_EXITED,
                System.currentTimeMillis(),
                0,
                4,
                4,
                0,
                false
        ));
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertFalse(SidetabEditorHover.isActive(editor));
    }

    public void testSidetabHoverRespectsHoverViewSetting() throws Exception {
        SubtabsSettings.getInstance().setHoverViewEnabled(false);
        VirtualFile file = createSourceFile("page.html");
        WriteAction.run(() -> file.setBinaryContent("""
                <html>
                <head><title>Hi</title></head>
                <body><p>Hello</p></body>
                </html>
                """.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        FileEditor fileEditor = editorFor(file);
        assertTrue(fileEditor instanceof TextEditor);
        Editor editor = ((TextEditor) fileEditor).getEditor();
        JToggleButton bodyButton = buttonNamed(panelFor(file), "Body");
        assertNotNull(bodyButton);

        bodyButton.dispatchEvent(new MouseEvent(
                bodyButton,
                MouseEvent.MOUSE_ENTERED,
                System.currentTimeMillis(),
                0,
                4,
                4,
                0,
                false
        ));
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertFalse(SidetabEditorHover.isActive(editor));
    }

    public void testPlacesSidetabsOnTheLeftWhenConfigured() throws Exception {
        SubtabsSettings.getInstance().setSidetabsOnRight(false);
        VirtualFile file = createSourceFile("page.html");
        WriteAction.run(() -> file.setBinaryContent("""
                <html>
                <head><title>Hi</title></head>
                <body><p>Hello</p></body>
                </html>
                """.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        SidetabBarPanel panel = panelFor(file);
        SidetabEditorHost host = (SidetabEditorHost) panel.getParent();
        assertSame(panel, ((BorderLayout) host.getLayout()).getLayoutComponent(BorderLayout.WEST));
    }

    public void testOverlayModeFloatsSidetabsAboveTheEditor() throws Exception {
        SubtabsSettings.getInstance().setSidetabLayoutMode(SidetabLayoutMode.OVERLAY);
        VirtualFile file = createSourceFile("page.html");
        WriteAction.run(() -> file.setBinaryContent("""
                <html>
                <head><title>Hi</title></head>
                <body><p>Hello</p></body>
                </html>
                """.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        SidetabBarPanel panel = panelFor(file);
        assertFalse(panel.getParent() instanceof SidetabEditorHost);
        assertTrue(panel.overlayMode());
        JToggleButton body = panel.buttonAt(panel.sections().size() - 1);
        assertNotNull(body);
        assertEquals("Body", body.getToolTipText());
    }

    public void testOverlayBarsStayTopAligned() throws Exception {
        SubtabsSettings.getInstance().setSidetabLayoutMode(SidetabLayoutMode.OVERLAY);
        VirtualFile file = createSourceFile("stack.component.ts");
        WriteAction.run(() -> file.setBinaryContent("""
                @Component({ selector: 'app-stack' })
                export class StackComponent {
                  title = 'A';
                  load() {}
                }
                """.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        SidetabBarPanel panel = panelFor(file);
        int sectionCount = panel.sections().size();
        assertTrue(sectionCount >= 3);

        panel.setSize(panel.getPreferredSize().width, 720);
        panel.validate();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        int stackTop = Integer.MAX_VALUE;
        int stackBottom = 0;
        for (int index = 0; index < sectionCount; index++) {
            JToggleButton button = panel.buttonAt(index);
            assertNotNull(button);
            java.awt.Point top = javax.swing.SwingUtilities.convertPoint(button, 0, 0, panel);
            java.awt.Point bottom = javax.swing.SwingUtilities.convertPoint(button, 0, button.getHeight(), panel);
            stackTop = Math.min(stackTop, top.y);
            stackBottom = Math.max(stackBottom, bottom.y);
        }

        int expectedHeight = SidetabBarPanel.overlayStackHeight(sectionCount);
        assertTrue(
                "Overlay stack must stay compact",
                stackBottom - stackTop <= expectedHeight + com.intellij.util.ui.JBUI.scale(2)
        );
        int expectedTop = SidetabBarPanel.stackTopInset();
        assertTrue(
                "Overlay bars must sit near the top of the panel (stackTop="
                        + stackTop + ", expectedTop=" + expectedTop + ")",
                stackTop >= 0 && stackTop <= expectedTop + com.intellij.util.ui.JBUI.scale(8)
        );
        assertTrue(
                "Overlay bars must not stay vertically centered in a tall panel",
                stackTop < panel.getHeight() / 3
        );
    }

    public void testSelectingASidetabMovesTheCaret() throws Exception {
        VirtualFile file = createSourceFile("page.html");
        String html = """
                <html>
                <head><title>Hi</title></head>
                <body><p>Hello</p></body>
                </html>
                """;
        WriteAction.run(() -> file.setBinaryContent(html.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        SidetabBarPanel panel = panelFor(file);
        JToggleButton body = buttonNamed(panel, "Body");
        assertNotNull(body);
        body.doClick();
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = editorFor(file);
        assertTrue(editor instanceof TextEditor);
        int offset = ((TextEditor) editor).getEditor().getCaretModel().getOffset();
        assertEquals(panel.sections().stream().filter(s -> "Body".equals(s.name())).findFirst().orElseThrow().startOffset(), offset);
    }

    public void testRightClickOnPrologFoldsHtml() throws Exception {
        VirtualFile file = createSourceFile("catalog-page.html");
        String html = """
                <!DOCTYPE html>

                <html lang="en">
                <head>
                  <title>Catalog</title>
                </head>
                <body>
                  <header><h1>Catalog</h1></header>
                  <main><p>Hello</p></main>
                </body>
                </html>
                """;
        WriteAction.run(() -> file.setBinaryContent(html.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        SidetabBarPanel panel = panelFor(file);
        JToggleButton prolog = buttonNamed(panel, "Prolog");
        assertNotNull(prolog);
        SidetabSection prologSection = panel.sections().stream()
                .filter(section -> "Prolog".equals(section.name()))
                .findFirst()
                .orElseThrow();
        assertTrue(prologSection.foldable());

        rightClick(prolog);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = editorFor(file);
        assertTrue(editor instanceof TextEditor);
        TextEditor textEditor = (TextEditor) editor;
        assertTrue(SidetabSectionFolding.isFolded(textEditor.getEditor(), prologSection));
        assertTrue(Boolean.TRUE.equals(prolog.getClientProperty(SidetabBarPanel.FOLDED_KEY)));

        rightClick(prolog);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        assertFalse(SidetabSectionFolding.isFolded(textEditor.getEditor(), prologSection));
        PlatformTestUtil.waitWithEventsDispatching(
                "Prolog fold state did not clear",
                () -> !Boolean.TRUE.equals(prolog.getClientProperty(SidetabBarPanel.FOLDED_KEY)),
                10
        );
    }

    public void testCatalogPageSubCommentStaysUnderMain() throws Exception {
        VirtualFile file = createSourceFile("catalog-page.html");
        String html = java.nio.file.Files.readString(
                java.nio.file.Path.of("demo-project/sidetabs-examples/html/catalog-page.html")
        );
        WriteAction.run(() -> file.setBinaryContent(html.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        SidetabBarPanel panel = panelFor(file);
        List<SidetabSection> sections = panel.sections();
        SidetabSection main = sections.stream()
                .filter(section -> "Main".equals(section.name()))
                .findFirst()
                .orElseThrow();
        SidetabSection test = sections.stream()
                .filter(section -> "Test".equals(section.name()))
                .findFirst()
                .orElseThrow();
        assertTrue(test.depth() > main.depth());
        assertNotNull(buttonNamed(panel, "Main"));
        assertNotNull(buttonNamed(panel, "Test"));

        TextEditor textEditor = (TextEditor) editorFor(file);
        String text = textEditor.getEditor().getDocument().getText();
        String mainSlice = text.substring(main.startOffset(), main.endOffset());
        String testSlice = text.substring(test.startOffset(), test.endOffset());
        assertTrue(mainSlice.contains("<main>"));
        assertTrue(mainSlice.contains("</main>"));
        assertTrue(mainSlice.contains("Stickers"));
        assertTrue(testSlice.contains("Mugs"));
        assertTrue(test.startOffset() > main.startOffset());
        assertTrue(test.endOffset() <= main.endOffset());
    }

    public void testCatalogPageSubCommentFoldsFullSection() throws Exception {
        VirtualFile file = createSourceFile("catalog-page.html");
        String html = java.nio.file.Files.readString(
                java.nio.file.Path.of("demo-project/sidetabs-examples/html/catalog-page.html")
        );
        WriteAction.run(() -> file.setBinaryContent(html.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        SidetabBarPanel panel = panelFor(file);
        JToggleButton test = buttonNamed(panel, "Test");
        assertNotNull(test);
        SidetabSection testSection = panel.sections().stream()
                .filter(section -> "Test".equals(section.name()))
                .findFirst()
                .orElseThrow();

        TextEditor textEditor = (TextEditor) editorFor(file);
        var editor = textEditor.getEditor();
        var foldRange = SidetabSectionFolding.foldRangeOffsetsForTest(editor, testSection);
        assertNotNull(foldRange);
        String text = editor.getDocument().getText();
        assertTrue(text.substring(foldRange[0], foldRange[1]).contains("Mugs"));
        assertTrue(text.substring(foldRange[0], foldRange[1]).contains("Stickers"));
        assertTrue(text.substring(foldRange[0], foldRange[1]).contains("+SUB-Test"));

        rightClick(test);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        assertTrue(SidetabSectionFolding.isFolded(editor, testSection));
        var foldedRegion = SidetabSectionFolding.foldRegionForTest(editor, testSection);
        assertNotNull(foldedRegion);
        assertEquals(foldRange[0], foldedRegion.getStartOffset());
        assertEquals(foldRange[1], foldedRegion.getEndOffset());
        assertFalse(foldedRegion.isExpanded());
    }

    public void testRightClickOnSidetabFoldsAndUnfoldsSection() throws Exception {
        VirtualFile file = createSourceFile("page.html");
        String html = """
                <html>
                <head><title>Hi</title></head>
                <body>
                <p>Hello</p>
                <p>World</p>
                </body>
                </html>
                """;
        WriteAction.run(() -> file.setBinaryContent(html.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        SidetabBarPanel panel = panelFor(file);
        JToggleButton body = buttonNamed(panel, "Body");
        assertNotNull(body);
        rightClick(body);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = editorFor(file);
        assertTrue(editor instanceof TextEditor);
        TextEditor textEditor = (TextEditor) editor;
        SidetabSection bodySection = panel.sections().stream().filter(s -> "Body".equals(s.name())).findFirst().orElseThrow();
        assertTrue(SidetabSectionFolding.isFolded(textEditor.getEditor(), bodySection));
        assertTrue(Boolean.TRUE.equals(body.getClientProperty(SidetabBarPanel.FOLDED_KEY)));

        rightClick(body);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        assertFalse(SidetabSectionFolding.isFolded(textEditor.getEditor(), bodySection));
        PlatformTestUtil.waitWithEventsDispatching(
                "SideTab fold state did not clear",
                () -> !Boolean.TRUE.equals(body.getClientProperty(SidetabBarPanel.FOLDED_KEY)),
                10
        );
        assertFalse(Boolean.TRUE.equals(body.getClientProperty(SidetabBarPanel.FOLDED_KEY)));
    }

    public void testRightClickOnDecoratorFoldsTsComponent() throws Exception {
        VirtualFile file = createSourceFile("header.component.ts");
        String ts = """
                import { Component } from '@angular/core';

                @Component({
                  selector: 'app-header',
                  templateUrl: './header.component.html',
                })
                export class HeaderComponent {
                  title = 'Catalog';

                  save(): void {
                    this.title = 'Saved';
                  }
                }
                """;
        WriteAction.run(() -> file.setBinaryContent(ts.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        SidetabBarPanel panel = panelFor(file);
        JToggleButton decorator = buttonNamed(panel, "Decorator");
        assertNotNull(decorator);
        rightClick(decorator);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = editorFor(file);
        assertTrue(editor instanceof TextEditor);
        TextEditor textEditor = (TextEditor) editor;
        SidetabSection decoratorSection = panel.sections().stream()
                .filter(section -> "Decorator".equals(section.name()))
                .findFirst()
                .orElseThrow();
        assertTrue(SidetabSectionFolding.isFolded(textEditor.getEditor(), decoratorSection));
        assertTrue(Boolean.TRUE.equals(decorator.getClientProperty(SidetabBarPanel.FOLDED_KEY)));
    }

    public void testRightClickOnDecoratorFoldsTsComponentInOverlayMode() throws Exception {
        SubtabsSettings.getInstance().setSidetabLayoutMode(SidetabLayoutMode.OVERLAY);
        VirtualFile file = createSourceFile("header.component.ts");
        String ts = """
                import { Component } from '@angular/core';

                @Component({
                  selector: 'app-header',
                  templateUrl: './header.component.html',
                })
                export class HeaderComponent {
                  title = 'Catalog';
                }
                """;
        WriteAction.run(() -> file.setBinaryContent(ts.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        SidetabBarPanel panel = panelFor(file);
        assertTrue(panel.overlayMode());
        JToggleButton decorator = buttonNamed(panel, "Decorator");
        assertNotNull(decorator);
        rightClick(decorator);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = editorFor(file);
        assertTrue(editor instanceof TextEditor);
        SidetabSection decoratorSection = panel.sections().stream()
                .filter(section -> "Decorator".equals(section.name()))
                .findFirst()
                .orElseThrow();
        assertTrue(SidetabSectionFolding.isFolded(((TextEditor) editor).getEditor(), decoratorSection));
    }

    public void testRightClickOnDecoratorFoldsWithLegacyPersistedRule() throws Exception {
        CustomSidetabRule tsComponent = SidetabRulesDefaults.createDefaults().stream()
                .filter(rule -> "TS-Component".equals(rule.name))
                .findFirst()
                .orElseThrow();
        CustomSidetabRule legacy = tsComponent.copy();
        legacy.sectionSpecs = List.of(
                new SidetabSectionSpec("Imports", "@start"),
                new SidetabSectionSpec("Decorator", "@text @Component || @text @Directive || @text @Pipe"),
                new SidetabSectionSpec("Fields", "@after-class-open"),
                new SidetabSectionSpec("Methods", "@first-method")
        );
        List<CustomSidetabRule> rules = SidetabRulesDefaults.createDefaults();
        for (int index = 0; index < rules.size(); index++) {
            if ("TS-Component".equals(rules.get(index).name)) {
                rules.set(index, legacy);
                break;
            }
        }
        SubtabsSettings.getInstance().setSidetabRules(rules);

        VirtualFile file = createSourceFile("header.component.ts");
        String ts = """
                import { Component } from '@angular/core';

                @Component({
                  selector: 'app-header',
                  templateUrl: './header.component.html',
                })
                export class HeaderComponent {
                  title = 'Catalog';
                }
                """;
        WriteAction.run(() -> file.setBinaryContent(ts.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        SidetabBarPanel panel = panelFor(file);
        JToggleButton decorator = buttonNamed(panel, "Decorator");
        assertNotNull(decorator);
        rightClick(decorator);
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();

        FileEditor editor = editorFor(file);
        assertTrue(editor instanceof TextEditor);
        SidetabSection decoratorSection = panel.sections().stream()
                .filter(section -> "Decorator".equals(section.name()))
                .findFirst()
                .orElseThrow();
        assertTrue(SidetabSectionFolding.isFolded(((TextEditor) editor).getEditor(), decoratorSection));
    }

    public void testTopCommentsReplaceHtmlStructureInTheEditor() throws Exception {
        VirtualFile file = createSourceFile("page.html");
        WriteAction.run(() -> file.setBinaryContent("""
                <html>
                <head></head>
                <body>
                <!-- TOP-Intro -->
                <p>Intro</p>
                <!-- TOP-Details -->
                <p>Details</p>
                </body>
                </html>
                """.getBytes(StandardCharsets.UTF_8)));
        openAndSettle(file);
        SidetabsManager.attachIfNeeded(getProject(), file);

        SidetabBarPanel panel = panelFor(file);
        assertEquals(List.of("Intro", "Details"), panel.sections().stream().map(SidetabSection::name).toList());
    }

    public void testSubtabsBarDoesNotStretchOverExpandedSidetabs() throws Exception {
        VirtualFile html = createSourceFile("header.component.html");
        WriteAction.run(() -> html.setBinaryContent("""
                <header>Title</header>
                <main>Content</main>
                """.getBytes(StandardCharsets.UTF_8)));
        createSourceFile("header.component.ts");
        openAndSettle(html);
        ComponentSubtabsManager.attachIfNeeded(getProject(), html);
        SidetabsManager.attachIfNeeded(getProject(), html);

        SidetabBarPanel side = panelFor(html);
        ComponentSubtabBarPanel bar = barFor(html);
        SidetabEditorHost host = (SidetabEditorHost) side.getParent();
        Component center = ((BorderLayout) host.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        assertTrue(javax.swing.SwingUtilities.isDescendingFrom(bar, center));
        assertFalse(javax.swing.SwingUtilities.isDescendingFrom(bar, side));
    }

    private SidetabBarPanel panelFor(VirtualFile file) {
        PlatformTestUtil.waitWithEventsDispatching(
                "no SideTabs bar appeared for " + file.getName(),
                () -> attachedPanel(file) != null,
                30
        );
        return attachedPanel(file);
    }

    private ComponentSubtabBarPanel barFor(VirtualFile file) {
        PlatformTestUtil.waitWithEventsDispatching(
                "no SubTabs bar appeared for " + file.getName(),
                () -> attachedBar(file) != null,
                30
        );
        return attachedBar(file);
    }

    private ComponentSubtabBarPanel attachedBar(VirtualFile file) {
        FileEditor editor = editorFor(file);
        return editor == null ? null : editor.getUserData(ComponentSubtabsManager.SUBTAB_BAR_KEY);
    }

    private SidetabBarPanel attachedPanel(VirtualFile file) {
        FileEditor editor = editorFor(file);
        return editor == null ? null : editor.getUserData(SidetabsManager.SIDETAB_BAR_KEY);
    }

    private FileEditor editorFor(VirtualFile file) {
        FileEditor[] editors = ComponentSubtabsManager.editorsFor(
                FileEditorManager.getInstance(getProject()),
                file
        );
        return editors.length == 0 ? null : editors[0];
    }

    private static JToggleButton buttonNamed(SidetabBarPanel panel, String name) {
        for (Component component : panel.getComponents()) {
            JToggleButton nested = findButton(component, name);
            if (nested != null) {
                return nested;
            }
        }
        return findButton(panel, name);
    }

    private static JToggleButton findButton(Component component, String name) {
        if (component instanceof JToggleButton button) {
            if (name.equals(button.getText()) || name.equals(button.getToolTipText())) {
                return button;
            }
        }
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                JToggleButton nested = findButton(child, name);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static void rightClick(Component component) {
        MouseEvent click = new MouseEvent(
                component,
                MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(),
                MouseEvent.BUTTON3_DOWN_MASK,
                5,
                5,
                1,
                false,
                MouseEvent.BUTTON3
        );
        component.dispatchEvent(click);
    }

    private static boolean hasHoverForSection(@NotNull Editor editor, @NotNull SidetabSection section) {
        Document document = editor.getDocument();
        int textLength = document.getTextLength();
        int sectionStart = Math.max(0, Math.min(section.startOffset(), textLength));
        int sectionEnd = Math.max(sectionStart, Math.min(section.endOffset(), textLength));
        int startLine = document.getLineNumber(sectionStart);
        int endLine = document.getLineNumber(Math.max(sectionStart, sectionEnd - 1));
        int startOffset = document.getLineStartOffset(startLine);
        int endOffset = Math.min(textLength, document.getLineEndOffset(endLine));

        for (RangeHighlighter highlighter : editor.getMarkupModel().getAllHighlighters()) {
            if (highlighter.getLayer() == SidetabEditorHover.HOVER_LAYER
                    && highlighter.getTargetArea() == HighlighterTargetArea.LINES_IN_RANGE
                    && highlighter.getStartOffset() == startOffset
                    && highlighter.getEndOffset() == endOffset) {
                return true;
            }
        }
        return false;
    }
}
