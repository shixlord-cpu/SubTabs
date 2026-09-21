package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidetabSectionsTest {
    @Test
    void splitsHtmlIntoHeadAndBody() {
        String html = """
                <!DOCTYPE html>
                <html>
                <head><title>Hi</title></head>
                <body>
                <p>Hello</p>
                </body>
                </html>
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "page.html",
                html,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Prolog", "Head", "Body"), names(sections));
        assertTrue(html.substring(sections.get(1).startOffset(), sections.get(1).endOffset()).contains("<head>"));
        assertTrue(html.substring(sections.get(2).startOffset(), sections.get(2).endOffset()).contains("<body>"));
        assertTrue(html.substring(sections.get(1).startOffset(), sections.get(1).endOffset()).contains("</head>"));
    }

    @Test
    void splitsAngularHeaderElementIntoHeadAndBody() {
        String html = """
                <header class="app-header">
                  <h1>Title</h1>
                  <nav>Links</nav>
                </header>
                <main>Content</main>
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "header.component.html",
                html,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Header", "Nav", "Main"), names(sections));
        SidetabSection header = sections.get(0);
        SidetabSection nav = sections.get(1);
        assertEquals(0, header.depth());
        assertEquals(1, nav.depth());
        assertTrue(slice(html, header).contains("</header>"));
        assertTrue(slice(html, nav).contains("<nav>"));
    }

    @Test
    void htmlPrologStartsAtDoctypeAndIsFoldable() {
        String html = """
                <!DOCTYPE html>
                <html><body></body></html>
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "page.html",
                html,
                SidetabRulesDefaults.createDefaults()
        );
        SidetabSection prolog = sections.stream()
                .filter(section -> "Prolog".equals(section.name()))
                .findFirst()
                .orElseThrow();
        assertTrue(slice(html, prolog).startsWith("<!DOCTYPE"));
        assertTrue(prolog.foldable());
    }

    @Test
    void htmlTaggedSectionsEndAtClosingTagInclusive() {
        String html = """
                <!DOCTYPE html>
                <html>
                <head><title>Hi</title></head>
                <body><p>Hello</p></body>
                </html>
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "page.html",
                html,
                SidetabRulesDefaults.createDefaults()
        );
        SidetabSection head = sections.stream()
                .filter(section -> "Head".equals(section.name()))
                .findFirst()
                .orElseThrow();
        SidetabSection body = sections.stream()
                .filter(section -> "Body".equals(section.name()))
                .findFirst()
                .orElseThrow();
        assertTrue(slice(html, head).contains("</head>"));
        assertTrue(slice(html, body).contains("</body>"));
    }

    @Test
    void splitsTsComponentIntoImportsDecoratorFieldsAndMethods() {
        String ts = """
                import { Component } from '@angular/core';
                
                @Component({
                  selector: 'app-user',
                })
                export class UserComponent {
                  name = 'Ada';
                  count = 0;
                
                  save(): void {
                    this.count++;
                  }
                
                  reset() {
                    this.count = 0;
                  }
                }
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "user.component.ts",
                ts,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Imports", "Decorator", "Fields", "Methods"), names(sections));
        assertTrue(slice(ts, sections.get(0)).contains("import { Component }"));
        assertTrue(slice(ts, sections.get(1)).contains("@Component"));
        assertFalse(slice(ts, sections.get(1)).contains("export class"));
        assertTrue(slice(ts, sections.get(2)).contains("name = 'Ada'"));
        assertTrue(slice(ts, sections.get(3)).contains("save(): void"));
    }

    @Test
    void decoratorSectionIsMultiLineForFolding() {
        String ts = """
                import { Component } from '@angular/core';

                @Component({
                  selector: 'app-user',
                })
                export class UserComponent {
                  name = 'Ada';
                }
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "user.component.ts",
                ts,
                SidetabRulesDefaults.createDefaults()
        );
        SidetabSection decorator = sections.stream()
                .filter(section -> "Decorator".equals(section.name()))
                .findFirst()
                .orElseThrow();
        int lineCount = ts.substring(decorator.startOffset(), decorator.endOffset()).split("\\R", -1).length;
        assertTrue(lineCount >= 2, "Decorator must span multiple lines to be foldable");
    }

    @Test
    void sectionEndExpressionStopsBeforeNextMarker() {
        CustomSidetabRule rule = new CustomSidetabRule();
        rule.name = "Bounded";
        rule.filePatterns = "*.txt";
        rule.enabled = true;
        rule.sectionSpecs = List.of(
                new SidetabSectionSpec(
                        "Block",
                        "@text BEGIN",
                        "@text END"
                )
        );

        String text = "BEGIN\nmiddle\nEND\nafter";
        List<SidetabSection> sections = SidetabSections.split("sample.txt", text, List.of(rule));
        assertEquals(List.of("Block"), names(sections));
        assertEquals("BEGIN\nmiddle\n", slice(text, sections.get(0)));
        assertFalse(slice(text, sections.get(0)).contains("END"));
    }

    @Test
    void splitsCssIntoImportsRulesMediaAndKeyframes() {
        String css = """
                @use 'tokens';
                @import url('fonts.css');

                @mixin shadow { box-shadow: 0 1px 2px; }

                .card { color: red; }

                @media (max-width: 600px) {
                  .card { color: blue; }
                }

                @keyframes fade-in {
                  from { opacity: 0; }
                  to { opacity: 1; }
                }
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "card.component.scss",
                css,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Imports", "Selectors", "Media", "Keyframes"), names(sections));
        assertTrue(slice(css, sectionNamed(sections, "Imports")).contains("@use 'tokens'"));
        assertTrue(slice(css, sectionNamed(sections, "Selectors")).contains("@mixin shadow"));
        assertTrue(slice(css, sectionNamed(sections, "Selectors")).contains(".card { color: red; }"));
        assertFalse(slice(css, sectionNamed(sections, "Selectors")).contains("@media"));
        assertTrue(slice(css, sectionNamed(sections, "Media")).contains("@media"));
        assertTrue(slice(css, sectionNamed(sections, "Keyframes")).contains("@keyframes fade-in"));
    }

    @Test
    void cssRulesStartWithoutImportsWhenFileHasNoPreamble() {
        String css = """
                .card { color: red; }

                @media (max-width: 600px) {
                  .card { color: blue; }
                }
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "plain.css",
                css,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Selectors", "Media"), names(sections));
        assertTrue(slice(css, sections.getFirst()).contains(".card { color: red; }"));
    }

    @Test
    void catalogStylesheetExampleContainsAllCssSections() throws Exception {
        String css = java.nio.file.Files.readString(
                java.nio.file.Path.of("demo-project/sidetabs-examples/css/stylesheet-full.scss")
        );
        List<SidetabSection> sections = SidetabSections.split(
                "stylesheet-full.scss",
                css,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Imports", "Selectors", "Media", "Keyframes"), names(sections));
        assertTrue(slice(css, sectionNamed(sections, "Imports")).contains("@use 'sass:color'"));
        assertTrue(slice(css, sectionNamed(sections, "Selectors")).contains(".card {"));
        assertTrue(slice(css, sectionNamed(sections, "Media")).contains("@media (max-width: 768px)"));
        assertTrue(slice(css, sectionNamed(sections, "Keyframes")).contains("@keyframes catalog-fade-in"));
    }

    private static SidetabSection sectionNamed(@NotNull List<SidetabSection> sections, @NotNull String name) {
        return sections.stream().filter(section -> name.equals(section.name())).findFirst().orElseThrow();
    }

    @Test
    void splitsSpecFileIntoSetupAndTests() {
        String spec = """
                import { HeaderComponent } from './header.component';
                
                describe('HeaderComponent', () => {
                  it('should create', () => {
                    expect(true).toBe(true);
                  });
                });
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "header.component.spec.ts",
                spec,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Setup", "Tests"), names(sections));
        assertTrue(slice(spec, sections.get(0)).contains("import"));
        assertTrue(slice(spec, sections.get(1)).contains("describe("));
    }

    @Test
    void splitsVueSingleFileComponent() {
        String vue = """
                <template>
                  <p>{{ title }}</p>
                </template>
                <script>
                export default { data() { return { title: 'Hi' } } }
                </script>
                <style>
                p { color: navy; }
                </style>
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "ProductCard.vue",
                vue,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Template", "Script", "Style"), names(sections));
        assertTrue(slice(vue, sections.get(0)).contains("<template>"));
        assertTrue(slice(vue, sections.get(1)).contains("<script>"));
        assertTrue(slice(vue, sections.get(2)).contains("<style>"));
    }

    @Test
    void splitsJavaClassIntoImportsClassFieldsAndMethods() {
        String java = """
                package shop;
                
                import shop.User;
                
                public class UserService {
                  private final UserRepository users;
                
                  public User find(String id) {
                    return users.find(id);
                  }
                }
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "UserService.java",
                java,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Imports", "Class", "Fields", "Methods"), names(sections));
        assertTrue(slice(java, sections.get(0)).contains("package shop"));
        assertTrue(slice(java, sections.get(1)).contains("public class UserService"));
        assertTrue(slice(java, sections.get(2)).contains("UserRepository"));
        assertTrue(slice(java, sections.get(3)).contains("public User find"));
    }

    @Test
    void splitsPythonModuleIntoImportsClassesAndFunctions() {
        String python = """
                from dataclasses import dataclass
                
                class Catalog:
                    def items(self):
                        return []
                
                def load_catalog():
                    return Catalog()
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "catalog.py",
                python,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Imports", "Classes", "Functions"), names(sections));
        assertTrue(slice(python, sections.get(1)).contains("class Catalog"));
        assertTrue(slice(python, sections.get(2)).contains("def load_catalog"));
    }

    @Test
    void splitsJavaScriptModuleIntoImportsExportsAndFunctions() {
        String ts = """
                import { Currency } from './money';
                
                export const DEFAULT_CURRENCY: Currency = 'EUR';
                
                export function formatPrice(value: number): string {
                  return `${value.toFixed(2)} ${DEFAULT_CURRENCY}`;
                }
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "price-format.ts",
                ts,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Imports", "Exports", "Functions"), names(sections));
        assertTrue(slice(ts, sections.get(1)).contains("DEFAULT_CURRENCY"));
        assertTrue(slice(ts, sections.get(2)).contains("formatPrice"));
    }

    @Test
    void splitsGoFileIntoImportsTypesAndFunctions() {
        String go = """
                package catalog
                
                import "fmt"
                
                type Product struct {
                	Name string
                }
                
                func main() {
                	fmt.Println("ok")
                }
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "server.go",
                go,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Imports", "Types", "Functions"), names(sections));
        assertTrue(slice(go, sections.get(1)).contains("type Product"));
        assertTrue(slice(go, sections.get(2)).contains("func main"));
    }

    @Test
    void splitsSqlIntoSchemaAndQueries() {
        String sql = """
                CREATE TABLE product (id INTEGER);
                SELECT name FROM product;
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "catalog.sql",
                sql,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Schema", "Queries"), names(sections));
        assertTrue(slice(sql, sections.get(1)).contains("SELECT"));
    }

    @Test
    void topCommentsOverrideDefaultStructure() {
        String html = """
                <html>
                <head></head>
                <body>
                <!-- TOP-Intro -->
                <p>Intro</p>
                <!-- TOP-{{Details}} -->
                <p>Details</p>
                </body>
                </html>
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "page.html",
                html,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Intro", "Details"), names(sections));
        assertTrue(slice(html, sections.get(0)).contains("TOP-Intro"));
        assertTrue(slice(html, sections.get(1)).contains("Details</p>"));
    }

    @Test
    void topCommentsWorkInTsLineComments() {
        String ts = """
                // TOP-Setup
                const a = 1;
                // TOP-Work
                const b = 2;
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "plain.ts",
                ts,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Setup", "Work"), names(sections));
    }

    @Test
    void topEndMarkerClosesSectionBeforeNextStart() {
        String ts = """
                // TOP-Setup
                const a = 1;
                // TOP-END
                const between = 0;
                // TOP-Work
                const b = 2;
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "plain.ts",
                ts,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Setup", "Work"), names(sections));
        assertTrue(slice(ts, sections.get(0)).contains("const a = 1;"));
        assertFalse(slice(ts, sections.get(0)).contains("between"));
        assertTrue(slice(ts, sections.get(1)).contains("const b = 2;"));
    }

    @Test
    void topEndMarkerCanTargetSectionByName() {
        String html = """
                <!-- TOP-Intro -->
                <p>Intro</p>
                <!-- TOP-END-Intro -->
                <p>Between</p>
                <!-- TOP-Details -->
                <p>Details</p>
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "page.html",
                html,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Intro", "Details"), names(sections));
        assertTrue(slice(html, sections.get(0)).contains("<p>Intro</p>"));
        assertFalse(slice(html, sections.get(0)).contains("Between"));
        assertTrue(slice(html, sections.get(1)).contains("<p>Details</p>"));
    }

    @Test
    void topEndMarkerExcludesEndCommentLineFromSection() {
        String ts = """
                // TOP-Only
                const a = 1;
                // TOP-END
                const after = 2;
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "plain.ts",
                ts,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Only"), names(sections));
        assertTrue(slice(ts, sections.getFirst()).contains("const a = 1;"));
        assertFalse(slice(ts, sections.getFirst()).contains("TOP-END"));
        assertFalse(slice(ts, sections.getFirst()).contains("after"));
    }

    @Test
    void topEndCommentLineParsesAsEndMarker() {
        SidetabSections.CommentMarker marker = SidetabSections.parseCommentMarkerForTest("// TOP-END", 0);
        assertNotNull(marker);
        assertTrue(marker.end());
        assertEquals(0, marker.depth());
    }

    @Test
    void topEndClosesSingleTopSection() {
        String ts = """
                // TOP-A
                a();
                // TOP-END
                tail();
                """;
        List<SidetabSection> sections = SidetabSections.parseTopComments(ts);
        assertEquals(List.of("A"), names(sections));
        assertFalse(slice(ts, sections.getFirst()).contains("tail();"));
    }

    @Test
    void subCommentsNestUnderTopSections() {
        String ts = """
                // TOP-Intro
                intro();
                // SUB-{{Setup}}
                setup();
                // +SUB-{{Detail}}
                detail();
                // +SUB-END
                // SUB-END
                // TOP-END
                tail();
                """;
        List<SidetabSection> sections = SidetabSections.parseTopComments(ts);
        int topEndLine = ts.indexOf("// TOP-END");
        assertTrue(topEndLine >= 0);
        int lineEnd = ts.indexOf('\n', topEndLine);
        if (lineEnd < 0) {
            lineEnd = ts.length();
        }
        assertNotNull(SidetabSections.parseCommentMarkerForTest(
                ts.substring(topEndLine, lineEnd),
                topEndLine
        ));
        assertEquals(List.of("Intro", "Setup", "Detail"), names(sections));
        assertEquals(0, sections.get(0).depth());
        assertEquals(1, sections.get(1).depth());
        assertEquals(2, sections.get(2).depth());
        assertTrue(sections.get(0).endOffset() < ts.length());
        assertTrue(slice(ts, sections.get(2)).contains("detail();"));
    }

    @Test
    void legacyFamilyMarkersStillParseAsTop() {
        String ts = """
                // FAMILY-Legacy
                const a = 1;
                """;
        List<SidetabSection> sections = SidetabSections.parseTopComments(ts);
        assertEquals(List.of("Legacy"), names(sections));
    }

    @Test
    void defaultBuiltinRulesHaveEndLogic() {
        for (CustomSidetabRule rule : SidetabRulesDefaults.createDefaults()) {
            if (rule.isTopRule()) {
                continue;
            }
            for (SidetabSectionSpec spec : rule.sectionSpecs) {
                assertFalse(spec.end.isBlank(), rule.name + "/" + spec.name);
            }
        }
    }

    @Test
    void endIncludesMarkerExtendsSectionThroughMarkerLine() {
        CustomSidetabRule rule = new CustomSidetabRule();
        rule.filePatterns = "*.txt";
        rule.enabled = true;
        rule.sectionSpecs = List.of(
                new SidetabSectionSpec("Part", "@start", "@text END", true)
        );
        String text = "Hello\nEND\nAfter";
        List<SidetabSection> sections = SidetabSections.splitByRule(text, rule);
        assertEquals(1, sections.size());
        assertTrue(slice(text, sections.getFirst()).contains("END"));
        assertFalse(slice(text, sections.getFirst()).contains("After"));
    }

    @Test
    void ignoresSectionOrderWhenRespectOrderIsDisabled() {
        CustomSidetabRule rule = new CustomSidetabRule();
        rule.filePatterns = "*.html";
        rule.enabled = true;
        rule.respectOrder = false;
        rule.sectionSpecs = List.of(
                new SidetabSectionSpec("Body", "@tag body", "@eof"),
                new SidetabSectionSpec("Head", "@tag head", "@tag body")
        );
        String html = """
                <html>
                <head><title>Title</title></head>
                <body><p>Hello</p></body>
                </html>
                """;
        List<SidetabSection> sections = SidetabSections.splitByRule(html, rule);
        assertEquals(List.of("Head", "Body"), names(sections));
    }

    @Test
    void respectsSectionOrderWhenRespectOrderIsEnabled() {
        CustomSidetabRule rule = new CustomSidetabRule();
        rule.filePatterns = "*.html";
        rule.enabled = true;
        rule.respectOrder = true;
        rule.sectionSpecs = List.of(
                new SidetabSectionSpec("Body", "@tag body", "@eof"),
                new SidetabSectionSpec("Head", "@tag head", "@tag body")
        );
        String html = """
                <html>
                <head><title>Title</title></head>
                <body><p>Hello</p></body>
                </html>
                """;
        List<SidetabSection> sections = SidetabSections.splitByRule(html, rule);
        assertEquals(List.of("Body"), names(sections));
    }

    @Test
    void customStartLogicRuleSplitsBySnippet() {
        CustomSidetabRule rule = new CustomSidetabRule();
        rule.name = "Custom Markdown";
        rule.filePatterns = "*.md";
        rule.enabled = true;
        rule.sectionSpecs = List.of(
                new SidetabSectionSpec("Intro", "@start"),
                new SidetabSectionSpec("API", "@regex ^## API")
        );

        String markdown = """
                # Title
                Hello
                ## API
                Usage
                """;
        List<SidetabSection> sections = SidetabSections.split("readme.md", markdown, List.of(rule));
        assertEquals(List.of("Intro", "API"), names(sections));
        assertTrue(slice(markdown, sections.get(1)).startsWith("## API"));
    }

    @Test
    void defaultMarkdownRuleSplitsOnHeadings() {
        String markdown = """
                # Guide
                Welcome.
                ## Setup
                Install the plugin.
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "guide.md",
                markdown,
                SidetabRulesDefaults.createDefaults()
        );
        assertEquals(List.of("Intro", "Sections"), names(sections));
        assertTrue(slice(markdown, sections.get(1)).startsWith("## Setup"));
    }

    @Test
    void compactEndMarkersParseAsEnds() {
        assertNotNull(SidetabSections.parseCommentMarkerForTest("// TOPEND", 0));
        assertNotNull(SidetabSections.parseCommentMarkerForTest("// SUBEND", 0));
        assertNotNull(SidetabSections.parseCommentMarkerForTest("// +SUBEND", 0));
        SidetabSections.CommentMarker topEnd = SidetabSections.parseCommentMarkerForTest("// TOPEND", 0);
        assertNotNull(topEnd);
        assertTrue(topEnd.end());
        SidetabSections.CommentMarker subEnd = SidetabSections.parseCommentMarkerForTest("// SUBEND", 0);
        assertNotNull(subEnd);
        assertTrue(subEnd.end());
        assertEquals(1, subEnd.depth());
    }

    @Test
    void topEndWithoutHyphenClosesTopSection() {
        String ts = """
                // TOP-A
                a();
                // TOPEND
                tail();
                """;
        List<SidetabSection> sections = SidetabSections.parseTopComments(ts);
        assertEquals(List.of("A"), names(sections));
        assertFalse(slice(ts, sections.getFirst()).contains("tail();"));
    }

    @Test
    void catalogPageMergesHtmlRuleWithSubComment() throws Exception {
        String html = java.nio.file.Files.readString(
                java.nio.file.Path.of("demo-project/sidetabs-examples/html/catalog-page.html")
        );
        List<SidetabSection> sections = SidetabSections.split(
                "catalog-page.html",
                html,
                SidetabRulesDefaults.createDefaults()
        );
        assertTrue(names(sections).contains("Main"));
        assertTrue(names(sections).contains("Test"));
        SidetabSection main = sections.stream().filter(section -> "Main".equals(section.name())).findFirst().orElseThrow();
        SidetabSection test = sections.stream().filter(section -> "Test".equals(section.name())).findFirst().orElseThrow();
        SidetabSection prolog = sections.stream().filter(section -> "Prolog".equals(section.name())).findFirst().orElseThrow();
        assertTrue(test.depth() > main.depth());
        assertTrue(main.foldable());
        assertTrue(prolog.foldable());
        assertTrue(slice(html, main).contains("<main>"));
        assertTrue(slice(html, main).contains("</main>"));
        assertTrue(slice(html, main).contains("Stickers"));
        assertTrue(slice(html, test).contains("Mugs"));
        assertTrue(test.startOffset() > main.startOffset());
        assertTrue(test.endOffset() <= main.endOffset());
    }

    @Test
    void subOnlyCommentsMergeIntoHtmlRuleSections() {
        String html = """
                <!DOCTYPE html>
                <html><body>
                <main>
                <!-- +SUB-Notes -->
                <p>Notes</p>
                <!-- +SUBEND -->
                </main>
                </body></html>
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "page.html",
                html,
                SidetabRulesDefaults.createDefaults()
        );
        assertTrue(names(sections).contains("Main"));
        assertTrue(names(sections).contains("Notes"));
    }

    @Test
    void combinedCommentsKeepRuleSectionsAndFamilyTabs() {
        String html = """
                <!DOCTYPE html>
                <html>
                <head></head>
                <body>
                <!-- TOP-Intro -->
                <p>Intro</p>
                <!-- TOP-Details -->
                <p>Details</p>
                </body>
                </html>
                """;
        List<SidetabSection> sections = SidetabSections.split(
                "page.html",
                html,
                SidetabRulesDefaults.createDefaults(),
                true
        );
        assertEquals(List.of("Prolog", "Head", "Body", "Intro", "Details"), names(sections));
    }

    @Test
    void matchesComponentTsFilePattern() {
        assertTrue(SidetabFilePatterns.matches("user.component.ts", "*.component.ts, *.component.tsx"));
        assertTrue(SidetabFilePatterns.matches("page.html", "*.html, *.htm"));
        assertTrue(SidetabFilePatterns.matches("styles.scss", "*.css, *.scss"));
        assertTrue(SidetabFilePatterns.matches("UserServiceTest.java", "*Test.java"));
    }

    @Test
    void keepsCustomRulesWhenMergingNewLanguageDefaults() {
        List<CustomSidetabRule> existing = SidetabRulesDefaults.createDefaults();
        CustomSidetabRule custom = new CustomSidetabRule();
        custom.name = "My Notes";
        custom.filePatterns = "*.txt";
        existing.add(custom);

        SidetabRulesDefaults.applyLatestDefaults(existing);

        assertTrue(existing.stream().anyMatch(rule -> "My Notes".equals(rule.name)));
        assertTrue(existing.stream().anyMatch(rule -> "Rust".equals(rule.name)));
        assertTrue(existing.stream().anyMatch(CustomSidetabRule::isTopRule));
    }

    private static List<String> names(List<SidetabSection> sections) {
        return sections.stream().map(SidetabSection::name).toList();
    }

    private static String slice(String text, SidetabSection section) {
        return text.substring(section.startOffset(), section.endOffset());
    }
}
