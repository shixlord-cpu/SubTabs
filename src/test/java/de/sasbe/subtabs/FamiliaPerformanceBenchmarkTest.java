package de.sasbe.subtabs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified Familia performance benchmark. Run via {@code ./gradlew test --tests FamiliaPerformanceBenchmarkTest}
 * and inspect stdout for the report table.
 */
public class FamiliaPerformanceBenchmarkTest extends RealEditorWindowTestCase {
    private static final int RELATED_FILES_ITERATIONS = 500;
    private static final int RULE_LOOKUP_ITERATIONS = 100;
    private static final int RULE_FILE_COUNT = 500;
    private static final int SIDETAB_SPLIT_ITERATIONS = 200;
    private static final int SIDETAB_MATCH_ITERATIONS = 5_000;
    private static final int DOCUMENT_REFRESH_ITERATIONS = 50;

    private static final String HTML_FIXTURE = """
            <!DOCTYPE html>
            <html>
            <head>
              <title>Product List</title>
              <link rel="stylesheet" href="app.css">
            </head>
            <body>
              <header class="toolbar">
                <h1>Products</h1>
              </header>
              <main class="content">
                <section id="filters">
                  <input type="search" placeholder="Search">
                </section>
                <section id="grid">
                  <article class="card">One</article>
                  <article class="card">Two</article>
                </section>
              </main>
              <footer>Footer</footer>
            </body>
            </html>
            """;

    private static final String TS_FIXTURE = """
            import { Component } from '@angular/core';

            @Component({
              selector: 'app-product-list',
              templateUrl: './product-list.component.html',
              styleUrl: './product-list.component.scss',
            })
            export class ProductListComponent {
              title = 'Products';

              load(): void {
                console.log('load');
              }
            }
            """;

    private VirtualFile htmlFile;
    private VirtualFile tsFile;
    private List<String> componentFileNames;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setSubtabsActive(true);
        settings.setSidetabsActive(true);
        settings.setSidetabsExpanded(true);
        settings.setGroupRelatedFilesInProjectView(true);

        htmlFile = createSourceFile("product-list.component.html");
        tsFile = createSourceFile("product-list.component.ts");
        createSourceFile("product-list.component.scss");
        WriteAction.run(() -> htmlFile.setBinaryContent(HTML_FIXTURE.getBytes(StandardCharsets.UTF_8)));
        WriteAction.run(() -> tsFile.setBinaryContent(TS_FIXTURE.getBytes(StandardCharsets.UTF_8)));

        componentFileNames = new ArrayList<>(RULE_FILE_COUNT);
        for (int index = 0; index < RULE_FILE_COUNT; index++) {
            componentFileNames.add("widget-" + index + ".component.ts");
        }
        ComponentSubtabsDocumentListener.install(getProject());
    }

    public void testPrintFamiliaPerformanceReport() {
        warmUp();

        Map<String, Long> metrics = new LinkedHashMap<>();
        metrics.put("SubTabs: related-files lookup (cached, 500x)", measureRelatedFilesCached());
        metrics.put("SubTabs: related-files lookup (uncached, 500x)", measureRelatedFilesUncached());
        metrics.put("SubTabs: rule key via settings snapshot (50k)", measureRuleLookupsUncached());
        metrics.put("SubTabs: rule key via ComponentFileNaming (50k)", measureRuleLookupsCached());
        metrics.put("SideTabs: HTML section split uncached (200x)", measureSidetabHtmlSplitUncached());
        metrics.put("SideTabs: HTML section split cached (200x)", measureSidetabHtmlSplitCached());
        metrics.put("SideTabs: TS section split (200x)", measureSidetabTsSplit());
        metrics.put("SideTabs: start matcher @tag head (5k)", measureSidetabStartMatcher());
        metrics.put("SideTabs: document refresh path (50x)", measureSidetabDocumentRefresh());
        metrics.put("Settings: legacy editor refresh", measureSettingsChange(true));
        metrics.put("Settings: consolidated editor refresh", measureSettingsChange(false));
        metrics.put("Project view: grouping toggle round-trip", measureGroupingToggle());

        printReport(metrics);
        assertNoRegressions(metrics);
    }

    private void warmUp() {
        ComponentRelatedFiles.find(htmlFile);
        ComponentFileNaming.componentBaseName("header.component.ts");
        SidetabSections.split("page.html", HTML_FIXTURE, SubtabsSettings.getInstance().getSidetabRules());
        SidetabStartMatcher.find(HTML_FIXTURE, "@tag head", 0);
    }

    private long measureRelatedFilesCached() {
        ComponentRelatedFilesCache cache = ComponentRelatedFilesCache.getInstance(getProject());
        cache.clear();
        ComponentRelatedFiles.find(htmlFile);
        return measure(() -> ComponentRelatedFiles.find(htmlFile), RELATED_FILES_ITERATIONS);
    }

    private long measureRelatedFilesUncached() {
        ComponentRelatedFilesCache cache = ComponentRelatedFilesCache.getInstance(getProject());
        return measure(() -> {
            cache.clear();
            ComponentRelatedFiles.findUncached(htmlFile);
        }, RELATED_FILES_ITERATIONS);
    }

    private long measureRuleLookupsCached() {
        return measure(() -> {
            for (String fileName : componentFileNames) {
                ComponentFileNaming.componentBaseName(fileName);
            }
        }, RULE_LOOKUP_ITERATIONS);
    }

    private long measureRuleLookupsUncached() {
        return measure(() -> {
            List<CustomSubtabRule> rules = SubtabsSettings.getInstance().getRules();
            if (rules.isEmpty()) {
                rules = SubtabRulesDefaults.createDefaults();
            }
            for (String fileName : componentFileNames) {
                CustomSubtabRuleMatcher.match(fileName, rules);
            }
        }, RULE_LOOKUP_ITERATIONS);
    }

    private long measureSidetabHtmlSplitUncached() {
        List<CustomSidetabRule> rules = SubtabsSettings.getInstance().getSidetabRules();
        return measure(() -> SidetabSections.split(htmlFile.getName(), HTML_FIXTURE, rules), SIDETAB_SPLIT_ITERATIONS);
    }

    private long measureSidetabHtmlSplitCached() {
        SidetabSectionsCache cache = SidetabSectionsCache.getInstance(getProject());
        cache.clear();
        List<CustomSidetabRule> rules = SubtabsSettings.getInstance().getSidetabRules();
        cache.split(htmlFile, htmlFile.getName(), HTML_FIXTURE, SubtabsSettings.getInstance().getSidetabRulesGeneration(), rules);
        return measure(() -> cache.split(
                htmlFile,
                htmlFile.getName(),
                HTML_FIXTURE,
                SubtabsSettings.getInstance().getSidetabRulesGeneration(),
                rules
        ), SIDETAB_SPLIT_ITERATIONS);
    }

    private long measureSidetabTsSplit() {
        List<CustomSidetabRule> rules = SubtabsSettings.getInstance().getSidetabRules();
        return measure(() -> SidetabSections.split(tsFile.getName(), TS_FIXTURE, rules), SIDETAB_SPLIT_ITERATIONS);
    }

    private long measureSidetabStartMatcher() {
        return measure(() -> SidetabStartMatcher.find(HTML_FIXTURE, "@tag head", 0), SIDETAB_MATCH_ITERATIONS);
    }

    private long measureSidetabDocumentRefresh() {
        openAndSettle(htmlFile);
        Document document = FileDocumentManager.getInstance().getDocument(htmlFile);
        assertNotNull(document);
        return measure(() -> SidetabsManager.refreshForDocument(getProject(), document), DOCUMENT_REFRESH_ITERATIONS);
    }

    private long measureSettingsChange(boolean legacy) {
        List<CustomSubtabRule> rules = toggleKomponenteRule();
        openAndSettle(htmlFile);
        openAndSettle(tsFile);
        SubtabsSettings.getInstance().setRules(rules);
        ComponentFileNaming.invalidateRulesCache();
        long start = System.nanoTime();
        if (legacy) {
            SubtabsLegacyRefreshSupport.applyEditorSettingsChangeLegacy(getProject());
        } else {
            ComponentSubtabsManager.applySettingsChange(getProject());
        }
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        return System.nanoTime() - start;
    }

    private long measureGroupingToggle() {
        openAndSettle(htmlFile);
        long start = System.nanoTime();
        SubtabsProjectViewGroupingState.getInstance(getProject()).toggle(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        SubtabsProjectViewGroupingState.getInstance(getProject()).toggle(getProject());
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
        return System.nanoTime() - start;
    }

    private static long measure(@NotNull Runnable action, int iterations) {
        long start = System.nanoTime();
        for (int index = 0; index < iterations; index++) {
            action.run();
        }
        return System.nanoTime() - start;
    }

    private static void printReport(@NotNull Map<String, Long> metrics) {
        System.out.println();
        System.out.println("=== Familia Performance Report ===");
        System.out.printf("%-52s %12s %12s%n", "Scenario", "Total (ms)", "Per op (µs)");
        for (Map.Entry<String, Long> entry : metrics.entrySet()) {
            long totalMs = entry.getValue() / 1_000_000;
            long iterations = extractIterationCount(entry.getKey());
            long perOpUs = iterations > 0 ? entry.getValue() / iterations / 1_000 : totalMs * 1_000;
            System.out.printf("%-52s %12d %12d%n", entry.getKey(), totalMs, perOpUs);
        }
        long cached = metrics.get("SubTabs: related-files lookup (cached, 500x)");
        long uncached = metrics.get("SubTabs: related-files lookup (uncached, 500x)");
        if (cached > 0 && uncached > 0) {
            System.out.printf("%nRelated-files cache speedup: %.1fx%n", (double) uncached / cached);
        }
        long naming = metrics.get("SubTabs: rule key via ComponentFileNaming (50k)");
        long snapshot = metrics.get("SubTabs: rule key via settings snapshot (50k)");
        if (naming > 0 && snapshot > 0) {
            System.out.printf("Rules snapshot speedup: %.1fx%n", (double) snapshot / naming);
        }
        long splitUncached = metrics.get("SideTabs: HTML section split uncached (200x)");
        long splitCached = metrics.get("SideTabs: HTML section split cached (200x)");
        if (splitUncached > 0 && splitCached > 0) {
            System.out.printf("SideTab split cache speedup: %.1fx%n", (double) splitUncached / splitCached);
        }
        long optimized = metrics.get("Settings: consolidated editor refresh");
        long legacy = metrics.get("Settings: legacy editor refresh");
        if (optimized > 0 && legacy > 0) {
            System.out.printf("Settings refresh speedup: %.1fx%n", (double) legacy / optimized);
        }
        System.out.println("==================================");
        System.out.println();
    }

    private static long extractIterationCount(@NotNull String key) {
        int open = key.lastIndexOf('(');
        int close = key.lastIndexOf(')');
        if (open < 0 || close <= open) {
            return 1;
        }
        String raw = key.substring(open + 1, close).replace(",", "").trim();
        if (raw.endsWith("x")) {
            raw = raw.substring(0, raw.length() - 1).trim();
        }
        if (raw.endsWith("k")) {
            return Long.parseLong(raw.substring(0, raw.length() - 1)) * 1_000;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private void assertNoRegressions(@NotNull Map<String, Long> metrics) {
        long cached = metrics.get("SubTabs: related-files lookup (cached, 500x)");
        long uncached = metrics.get("SubTabs: related-files lookup (uncached, 500x)");
        assertTrue("related-files cache should stay faster", cached * 5 < uncached);

        long splitUncached = metrics.get("SideTabs: HTML section split uncached (200x)");
        long splitCached = metrics.get("SideTabs: HTML section split cached (200x)");
        assertTrue("sidetab split cache should stay faster", splitCached * 3 < splitUncached);
    }

    private static @NotNull List<CustomSubtabRule> toggleKomponenteRule() {
        List<CustomSubtabRule> rules = new ArrayList<>();
        for (CustomSubtabRule rule : SubtabsSettings.getInstance().getRules()) {
            CustomSubtabRule copy = rule.copy();
            if ("Komponente".equals(copy.name)) {
                copy.enabled = !copy.enabled;
            }
            rules.add(copy);
        }
        return rules;
    }
}
