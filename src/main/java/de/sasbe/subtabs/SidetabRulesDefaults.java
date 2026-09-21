package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SidetabRulesDefaults {
    static final int VERSION = 11;

    private static final String CSS_IMPORTS_START =
            "@regex (?m)^\\s*@(?:import|use|forward|charset)\\b";
    private static final String CSS_RULES_START =
            "@regex (?m)^\\s*(?!(?:@import|@use|@forward|@charset)\\b)"
                    + "(?:@(?:mixin|function|include|extend|apply)\\b|[.#:])";
    private static final String CSS_KEYFRAMES_START =
            "@regex (?m)^\\s*@keyframes\\b";

    private static volatile List<CustomSidetabRule> defaultTemplate;

    private SidetabRulesDefaults() {
    }

    public static @NotNull List<CustomSidetabRule> createDefaults() {
        List<CustomSidetabRule> template = defaultTemplate;
        if (template == null) {
            template = buildDefaults();
            defaultTemplate = template;
        }
        List<CustomSidetabRule> rules = new ArrayList<>(template.size());
        for (CustomSidetabRule rule : template) {
            rules.add(rule.copy());
        }
        return rules;
    }

    private static @NotNull List<CustomSidetabRule> buildDefaults() {
        List<CustomSidetabRule> rules = new ArrayList<>();
        rules.add(topCommentsRule());
        rules.add(testsRule());
        rules.add(tsComponentRule());
        rules.add(htmlRule());
        rules.add(cssRule());
        rules.add(vueRule());
        rules.add(javaScriptRule());
        rules.add(javaRule());
        rules.add(kotlinRule());
        rules.add(csharpRule());
        rules.add(pythonRule());
        rules.add(goRule());
        rules.add(phpRule());
        rules.add(rustRule());
        rules.add(sqlRule());
        rules.add(markdownRule());
        return List.copyOf(rules);
    }

    static void applyLatestDefaults(@NotNull List<CustomSidetabRule> existing) {
        ensureTopRule(existing);
        List<CustomSidetabRule> defaults = createDefaults();
        for (CustomSidetabRule stock : defaults) {
            CustomSidetabRule found = findByName(existing, stock.name);
            if (found == null && stock.isTopRule()) {
                found = findTopRule(existing);
            }
            if (found == null) {
                existing.add(stock.copy());
                continue;
            }
            if (stock.builtin) {
                found.filePatterns = stock.filePatterns;
                found.type = stock.type;
                if (stock.isTopRule()) {
                    found.familyMode = stock.familyMode;
                    found.name = stock.name;
                } else {
                    found.sectionSpecs = stock.copySpecs();
                }
                found.builtin = true;
            }
        }
    }

    static void repairBuiltinSectionSpecs(@NotNull List<CustomSidetabRule> rules) {
        repairTsComponentDecoratorEnd(rules);
        repairHtmlSections(rules);
        repairChainedEnds(rules);
    }

    static boolean needsBuiltinSectionRepair(@NotNull List<CustomSidetabRule> rules) {
        CustomSidetabRule html = findByName(rules, "HTML");
        if (html != null && html.builtin && needsHtmlSectionRepair(html.sectionSpecs)) {
            return true;
        }
        CustomSidetabRule tsComponent = findByName(rules, "TS-Component");
        if (tsComponent == null || !tsComponent.builtin || tsComponent.sectionSpecs == null) {
            return false;
        }
        for (SidetabSectionSpec spec : tsComponent.sectionSpecs) {
            if ("Decorator".equals(spec.name) && (spec.end == null || spec.end.isBlank())) {
                return true;
            }
        }
        return false;
    }

    private static void repairChainedEnds(@NotNull List<CustomSidetabRule> rules) {
        for (CustomSidetabRule rule : rules) {
            if (!rule.builtin || rule.isTopRule() || rule.sectionSpecs == null || rule.sectionSpecs.isEmpty()) {
                continue;
            }
            List<SidetabSectionSpec> specs = rule.sectionSpecs;
            for (int index = 0; index < specs.size(); index++) {
                SidetabSectionSpec spec = specs.get(index);
                if (spec.end != null && !spec.end.isBlank()) {
                    continue;
                }
                spec.end = index + 1 < specs.size() ? specs.get(index + 1).start : "@eof";
            }
        }
    }

    private static void repairTsComponentDecoratorEnd(@NotNull List<CustomSidetabRule> rules) {
        CustomSidetabRule tsComponent = findByName(rules, "TS-Component");
        if (tsComponent == null || !tsComponent.builtin || tsComponent.sectionSpecs == null) {
            return;
        }
        for (SidetabSectionSpec spec : tsComponent.sectionSpecs) {
            if (!"Decorator".equals(spec.name)) {
                continue;
            }
            if (spec.end != null && !spec.end.isBlank()) {
                continue;
            }
            spec.start = "@regex (?m)^\\s*@(Component|Directive|Pipe)\\s*[({\\[]";
            spec.end = "@regex (?m)^\\s*(?:export\\s+)?(?:default\\s+)?(?:abstract\\s+)?class\\s+\\w+";
        }
    }

    private static void repairHtmlSections(@NotNull List<CustomSidetabRule> rules) {
        CustomSidetabRule html = findByName(rules, "HTML");
        if (html == null || !html.builtin) {
            return;
        }
        if (!needsHtmlSectionRepair(html.sectionSpecs)) {
            return;
        }
        CustomSidetabRule stock = htmlRule();
        html.filePatterns = stock.filePatterns;
        html.sectionSpecs = stock.copySpecs();
    }

    private static boolean needsHtmlSectionRepair(@Nullable List<SidetabSectionSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return true;
        }
        SidetabSectionSpec prolog = null;
        SidetabSectionSpec header = null;
        for (SidetabSectionSpec spec : specs) {
            if ("Prolog".equals(spec.name)) {
                prolog = spec;
            }
            if ("Header".equals(spec.name)) {
                header = spec;
            }
        }
        if (prolog == null || header == null) {
            return true;
        }
        String prologStart = prolog.start == null ? "" : prolog.start.trim();
        if (!prologStart.toLowerCase(Locale.ROOT).contains("<!doctype")) {
            return true;
        }
        String headerEnd = header.end == null ? "" : header.end.trim();
        if (!headerEnd.toLowerCase(Locale.ROOT).contains("@close-tag")) {
            return true;
        }
        return !prolog.foldable;
    }

    static void ensureTopRule(@NotNull List<CustomSidetabRule> rules) {
        for (CustomSidetabRule rule : rules) {
            if (rule.isTopRule()) {
                rule.builtin = true;
                rule.type = CustomSidetabRule.Type.TOP;
                if (CustomSidetabRule.LEGACY_FAMILY_RULE_NAME.equals(rule.name) || rule.name.isBlank()) {
                    rule.name = CustomSidetabRule.TOP_RULE_NAME;
                }
                return;
            }
        }
        rules.add(0, topCommentsRule());
    }

    static @Nullable CustomSidetabRule findTopRule(@NotNull List<CustomSidetabRule> rules) {
        for (CustomSidetabRule rule : rules) {
            if (rule.isTopRule()) {
                return rule;
            }
        }
        return null;
    }

    static @NotNull List<CustomSidetabRule> normalRules(@NotNull List<CustomSidetabRule> rules) {
        List<CustomSidetabRule> normal = new ArrayList<>();
        for (CustomSidetabRule rule : rules) {
            if (!rule.isTopRule()) {
                normal.add(rule);
            }
        }
        return normal;
    }

    static @NotNull CustomSidetabRule topCommentsRule() {
        CustomSidetabRule rule = new CustomSidetabRule();
        rule.name = CustomSidetabRule.TOP_RULE_NAME;
        rule.filePatterns = "*";
        rule.enabled = true;
        rule.builtin = true;
        rule.type = CustomSidetabRule.Type.TOP;
        rule.familyMode = TopCommentMode.OVERRIDE;
        return rule;
    }

    static @NotNull CustomSidetabRule htmlRule() {
        return rule("HTML", "*.html, *.htm, *.xhtml",
                section("Prolog", "@regex (?i)<!DOCTYPE", "@tag html", false, true),
                htmlTaggedSection("Head", "head"),
                htmlTaggedSection("Header", "header"),
                htmlTaggedSection("Nav", "nav"),
                htmlTaggedSection("Body", "body"),
                htmlTaggedSection("Main", "main"),
                htmlTaggedSection("Section", "section"),
                htmlTaggedSection("Footer", "footer"),
                htmlTaggedSection("Scripts", "script"),
                htmlTaggedSection("Styles", "style")
        );
    }

    static @NotNull CustomSidetabRule tsComponentRule() {
        return rule("TS-Component", "*.component.ts, *.component.tsx",
                section("Imports", "@start"),
                section(
                        "Decorator",
                        "@regex (?m)^\\s*@(Component|Directive|Pipe)\\s*[({\\[]",
                        "@regex (?m)^\\s*(?:export\\s+)?(?:default\\s+)?(?:abstract\\s+)?class\\s+\\w+"
                ),
                section("Fields", "@after-class-open"),
                section("Methods", "@first-method")
        );
    }

    static @NotNull CustomSidetabRule cssRule() {
        return rule("CSS", "*.css, *.scss, *.sass, *.less",
                section("Imports", CSS_IMPORTS_START),
                section("Selectors", CSS_RULES_START),
                section("Media", "@media"),
                section("Keyframes", CSS_KEYFRAMES_START)
        );
    }

    static @NotNull CustomSidetabRule testsRule() {
        return rule(
                "Tests",
                "*.spec.ts, *.test.ts, *.spec.js, *.test.js, *.spec.tsx, *.test.tsx,"
                        + " test_*.py, *_test.py, *Test.java, *Tests.java, *Test.kt, *Tests.kt, *_test.go",
                section("Setup", "@start"),
                section("Tests", "@regex (?m)^\\s*(?:it|test|describe)\\s*\\("
                        + " || @text @Test"
                        + " || @regex (?m)^\\s*def test_"
                        + " || @regex (?m)^func Test")
        );
    }

    static @NotNull CustomSidetabRule vueRule() {
        return rule("Vue", "*.vue",
                section("Template", "@regex (?m)^\\s*<template\\b", "@close-tag template", true),
                section("Script", "@tag script", "@close-tag script", true),
                section("Style", "@tag style", "@close-tag style", true)
        );
    }

    static @NotNull CustomSidetabRule javaScriptRule() {
        return rule("JavaScript/TS", "*.ts, *.tsx, *.js, *.jsx, *.mjs, *.cjs",
                section("Imports", "@start"),
                section("Exports", "@regex (?m)^export\\s+"),
                section("Functions", "@regex (?m)^(?:export\\s+)?(?:async\\s+)?function\\s+"
                        + " || @regex (?m)^(?:export\\s+)?const\\s+\\w+\\s*=\\s*(?:async\\s+)?\\(")
        );
    }

    static @NotNull CustomSidetabRule javaRule() {
        return rule("Java", "*.java",
                section("Imports", "@start"),
                section("Class", "@regex (?m)^.*\\b(class|interface|enum|record)\\s+"),
                section("Fields", "@after-class-open"),
                section("Methods", "@regex (?m)^\\s+(public|private|protected)\\s+.+\\(")
        );
    }

    static @NotNull CustomSidetabRule kotlinRule() {
        return rule("Kotlin", "*.kt, *.kts",
                section("Imports", "@start"),
                section("Class", "@regex (?m)^.*\\b(class|object|interface)\\s+"),
                section("Functions", "@regex (?m)^\\s*fun\\s+")
        );
    }

    static @NotNull CustomSidetabRule csharpRule() {
        return rule("C#", "*.cs",
                section("Imports", "@start"),
                section("Class", "@regex (?m)^.*\\b(class|record|struct|interface)\\s+"),
                section("Methods", "@regex (?m)^\\s+(public|private|protected)\\s+.+\\(")
        );
    }

    static @NotNull CustomSidetabRule pythonRule() {
        return rule("Python", "*.py",
                section("Imports", "@start"),
                section("Classes", "@regex (?m)^class\\s+"),
                section("Functions", "@regex (?m)^(?:async\\s+)?def\\s+")
        );
    }

    static @NotNull CustomSidetabRule goRule() {
        return rule("Go", "*.go",
                section("Imports", "@start"),
                section("Types", "@regex (?m)^type\\s+"),
                section("Functions", "@regex (?m)^func\\s+")
        );
    }

    static @NotNull CustomSidetabRule phpRule() {
        return rule("PHP", "*.php",
                section("Imports", "@start"),
                section("Class", "@regex (?m)^(?:abstract\\s+|final\\s+)?class\\s+"),
                section("Methods", "@regex (?m)^\\s+(?:public|private|protected)\\s+function\\s+")
        );
    }

    static @NotNull CustomSidetabRule rustRule() {
        return rule("Rust", "*.rs",
                section("Imports", "@start"),
                section("Types", "@regex (?m)^(?:pub\\s+)?(?:struct|enum|trait|impl)\\s+"),
                section("Functions", "@regex (?m)^\\s*(?:pub\\s+)?fn\\s+")
        );
    }

    static @NotNull CustomSidetabRule sqlRule() {
        return rule("SQL", "*.sql",
                section("Schema", "@start"),
                section("Queries", "@regex (?i)\\bSELECT\\b")
        );
    }

    static @NotNull CustomSidetabRule markdownRule() {
        return rule("Markdown", "*.md, *.mdx",
                section("Intro", "@start"),
                section("Sections", "@regex (?m)^##\\s+")
        );
    }

    static @NotNull List<SidetabSectionSpec> htmlSpecs() {
        return htmlRule().copySpecs();
    }

    static @NotNull List<SidetabSectionSpec> tsComponentSpecs() {
        return tsComponentRule().copySpecs();
    }

    static @NotNull List<SidetabSectionSpec> cssSpecs() {
        return cssRule().copySpecs();
    }

    static @NotNull List<SidetabSectionSpec> specsFromLegacy(@NotNull CustomSidetabRule rule) {
        if (rule.isTopRule()) {
            return List.of();
        }
        if (rule.sectionSpecs != null && !rule.sectionSpecs.isEmpty()) {
            return copy(rule.sectionSpecs);
        }
        List<String> labels = SidetabFilePatterns.split(rule.sections == null ? "" : rule.sections);
        CustomSidetabRule.Kind kind = rule.kind == null ? CustomSidetabRule.Kind.REGEX : rule.kind;
        List<SidetabSectionSpec> template = switch (kind) {
            case HTML -> htmlSpecs();
            case TS_COMPONENT -> tsComponentSpecs();
            case CSS -> cssSpecs();
            case REGEX -> regexSpecs(labels, rule.patterns == null ? "" : rule.patterns);
        };
        if (kind != CustomSidetabRule.Kind.REGEX && !labels.isEmpty()) {
            List<SidetabSectionSpec> renamed = new ArrayList<>(template.size());
            for (int index = 0; index < template.size(); index++) {
                SidetabSectionSpec spec = template.get(index).copy();
                if (index < labels.size() && !labels.get(index).isBlank()) {
                    spec.name = labels.get(index);
                }
                renamed.add(spec);
            }
            return renamed;
        }
        return template;
    }

    private static @Nullable CustomSidetabRule findByName(
            @NotNull List<CustomSidetabRule> rules,
            @NotNull String name
    ) {
        for (CustomSidetabRule rule : rules) {
            if (name.equals(rule.name)) {
                return rule;
            }
        }
        return null;
    }

    private static @NotNull CustomSidetabRule rule(
            @NotNull String name,
            @NotNull String filePatterns,
            @NotNull SidetabSectionSpec... specs
    ) {
        CustomSidetabRule rule = new CustomSidetabRule();
        rule.name = name;
        rule.filePatterns = filePatterns;
        rule.enabled = true;
        rule.builtin = true;
        rule.sectionSpecs = chainEnds(specs);
        return rule;
    }

    private static @NotNull List<SidetabSectionSpec> chainEnds(@NotNull SidetabSectionSpec... specs) {
        List<SidetabSectionSpec> result = new ArrayList<>(specs.length);
        for (int index = 0; index < specs.length; index++) {
            SidetabSectionSpec spec = specs[index].copy();
            if (spec.end == null || spec.end.isBlank()) {
                spec.end = index + 1 < specs.length ? specs[index + 1].start : "@eof";
            }
            result.add(spec);
        }
        return result;
    }

    private static @NotNull SidetabSectionSpec section(@NotNull String name, @NotNull String start) {
        return section(name, start, "");
    }

    private static @NotNull SidetabSectionSpec section(
            @NotNull String name,
            @NotNull String start,
            @NotNull String end
    ) {
        return new SidetabSectionSpec(name, start, end);
    }

    private static @NotNull SidetabSectionSpec section(
            @NotNull String name,
            @NotNull String start,
            @NotNull String end,
            boolean endIncludesMarker
    ) {
        return new SidetabSectionSpec(name, start, end, endIncludesMarker);
    }

    private static @NotNull SidetabSectionSpec section(
            @NotNull String name,
            @NotNull String start,
            @NotNull String end,
            boolean endIncludesMarker,
            boolean foldable
    ) {
        return new SidetabSectionSpec(name, start, end, endIncludesMarker, foldable);
    }

    private static @NotNull SidetabSectionSpec htmlTaggedSection(@NotNull String name, @NotNull String tag) {
        return section(
                name,
                "@tag " + tag + " || @regex (?m)^\\s*<" + tag + "\\b",
                "@close-tag " + tag,
                true
        );
    }

    private static @NotNull List<SidetabSectionSpec> regexSpecs(
            @NotNull List<String> labels,
            @NotNull String patterns
    ) {
        List<String> regexes = new ArrayList<>();
        for (String raw : patterns.split(";;")) {
            regexes.add(raw.trim());
        }
        if (labels.isEmpty()) {
            return new ArrayList<>();
        }
        List<SidetabSectionSpec> specs = new ArrayList<>(labels.size());
        for (int index = 0; index < labels.size(); index++) {
            String start;
            if (index == 0 && (regexes.isEmpty() || regexes.get(0).isBlank())) {
                start = "@start";
            } else if (index < regexes.size() && !regexes.get(index).isBlank()) {
                start = "@regex " + regexes.get(index);
            } else if (index == 0) {
                start = "@start";
            } else {
                start = "";
            }
            specs.add(new SidetabSectionSpec(labels.get(index), start));
        }
        return chainEnds(specs.toArray(SidetabSectionSpec[]::new));
    }

    private static @NotNull List<SidetabSectionSpec> copy(@NotNull List<SidetabSectionSpec> specs) {
        List<SidetabSectionSpec> copy = new ArrayList<>(specs.size());
        for (SidetabSectionSpec spec : specs) {
            copy.add(spec.copy());
        }
        return copy;
    }
}
