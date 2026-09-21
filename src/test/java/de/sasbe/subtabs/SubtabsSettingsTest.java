package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtabsSettingsTest {
    @Test
    void showsSubtabsByDefault() {
        assertTrue(new SubtabsSettings.State().subtabsActive);
    }

    @Test
    void showsCollapseButtonByDefault() {
        assertTrue(new SubtabsSettings.State().showCollapseButton);
    }

    @Test
    void groupsRelatedFilesInProjectViewByDefault() {
        assertTrue(new SubtabsSettings.State().groupRelatedFilesInProjectView);
    }

    @Test
    void fitsTabsToEditorWidthByDefault() {
        assertTrue(new SubtabsSettings.State().fitTabsToEditorWidth);
    }

    @Test
    void usesCompactBarHeightByDefault() {
        assertEquals(75, new SubtabsSettings.State().barHeightPercent);
    }

    @Test
    void usesIdeStandardTabFontStyleByDefault() {
        assertEquals(TabFontStyle.IDE_STANDARD.name(), new SubtabsSettings.State().tabFontStyle);
        assertEquals("IDE-Standard", TabFontStyle.IDE_STANDARD.label());
        assertEquals("Monospace", TabFontStyle.MONOSPACED.label());
    }

    @Test
    void usesRelativeTextSizeByDefault() {
        assertEquals(75, new SubtabsSettings.State().textSizePercent);
    }

    @Test
    void usesScrollbarOverflowByDefault() {
        assertEquals("SCROLLBAR", new SubtabsSettings.State().overflowMode);
    }

    @Test
    void usesDefaultGroupTreeControlsByDefault() {
        assertEquals("DEFAULT", new SubtabsSettings.State().groupTreeControlStyle);
        assertFalse(new SubtabsSettings.State().invertGroupTreeControlFill);
        assertEquals("Default", SubtabGroupTreeControlStyle.DEFAULT.label());
        assertEquals("Cubes", SubtabGroupTreeControlStyle.CUBES.label());
        assertEquals("Circles", SubtabGroupTreeControlStyle.CIRCLES.label());
        assertEquals("Blue Arrows", SubtabGroupTreeControlStyle.BLUE_ARROWS.label());
        assertEquals("Colored Arrows", SubtabGroupTreeControlStyle.COLORED_ARROWS.label());
        assertEquals("None", SubtabGroupTreeControlStyle.NONE.label());
        assertFalse(SubtabGroupTreeControlStyle.NONE.allowsGroupExpansion());
        assertTrue(SubtabGroupTreeControlStyle.DEFAULT.allowsGroupExpansion());
    }

    @Test
    void enablesGroupColorsByDefault() {
        assertTrue(new SubtabsSettings.State().groupColorsEnabled);
        assertTrue(new SubtabsSettings.State().groupColorHexes.isEmpty());
        assertEquals(0, new SubtabsSettings.State().nextGroupColorIndex);
    }

    @Test
    void namesArrowOverflowRandpfeile() {
        assertEquals("Randpfeile", SubtabOverflowMode.ARROWS.label());
        assertEquals("Randpfeile", SubtabOverflowMode.ARROWS.toString());
        assertEquals("Scrollbalken", SubtabOverflowMode.SCROLLBAR.label());
    }

    @Test
    void enablesSidetabsByDefault() {
        SubtabsSettings settings = new SubtabsSettings();
        assertTrue(settings.isSidetabsActive());
        assertTrue(settings.isSidetabsExpanded());
        assertEquals(SidetabLayoutMode.BESIDE, settings.getSidetabLayoutMode());
        assertFalse(settings.isSidetabsCombineComments());
    }

    @Test
    void defaultsSidetabLayoutModeToBeside() {
        assertEquals(SidetabLayoutMode.BESIDE, new SubtabsSettings().getSidetabLayoutMode());
    }

    @Test
    void includesTopCommentsAsSpecialBuiltinRule() {
        List<CustomSidetabRule> rules = new SubtabsSettings.State().sidetabRules;
        assertEquals(CustomSidetabRule.TOP_RULE_NAME, rules.get(0).name);
        assertTrue(rules.get(0).isTopRule());
        assertTrue(rules.get(0).builtin);
    }

    @Test
    void shipsWithBuiltInSidetabRules() {
        List<CustomSidetabRule> rules = new SubtabsSettings.State().sidetabRules;
        assertEquals(16, rules.size());
        assertEquals(CustomSidetabRule.TOP_RULE_NAME, rules.get(0).name);
        assertEquals("Tests", rules.get(1).name);
        assertEquals("TS-Component", rules.get(2).name);
        assertEquals("HTML", rules.get(3).name);
        assertEquals("CSS", rules.get(4).name);
        assertEquals("Datei-Regeln überschreiben", rules.get(0).sectionsSummary());
        assertEquals("Setup, Tests", rules.get(1).sectionsSummary());
        assertEquals("Imports, Decorator, Fields, Methods", rules.get(2).sectionsSummary());
        assertTrue(rules.get(3).sectionsSummary().contains("Prolog"));
        assertTrue(rules.get(3).sectionsSummary().contains("Head"));
        assertTrue(rules.get(3).sectionsSummary().contains("Header"));
        assertTrue(rules.get(3).sectionsSummary().contains("Main"));
        assertTrue(rules.get(3).sectionsSummary().contains("Body"));
        assertEquals("Imports, Selectors, Media, Keyframes", rules.get(4).sectionsSummary());
    }

    @Test
    void repairsStaleTsComponentDecoratorEndExpression() {
        SubtabsSettings.State state = new SubtabsSettings.State();
        state.sidetabRulesVersion = 5;
        CustomSidetabRule tsComponent = state.sidetabRules.stream()
                .filter(rule -> "TS-Component".equals(rule.name))
                .findFirst()
                .orElseThrow();
        SidetabSectionSpec decorator = tsComponent.sectionSpecs.stream()
                .filter(spec -> "Decorator".equals(spec.name))
                .findFirst()
                .orElseThrow();
        decorator.start = "@text @Component || @text @Directive || @text @Pipe";
        decorator.end = "";

        SubtabsSettings settings = new SubtabsSettings();
        settings.loadState(state);

        SidetabSectionSpec repaired = settings.getSidetabRules().stream()
                .filter(rule -> "TS-Component".equals(rule.name))
                .findFirst()
                .orElseThrow()
                .sectionSpecs.stream()
                .filter(spec -> "Decorator".equals(spec.name))
                .findFirst()
                .orElseThrow();
        assertFalse(repaired.end.isBlank());
        assertTrue(repaired.end.contains("@regex"));
        assertEquals(11, settings.getState().sidetabRulesVersion);
    }

    @Test
    void repairsStaleHtmlSections() {
        SubtabsSettings.State state = new SubtabsSettings.State();
        state.sidetabRulesVersion = 6;
        CustomSidetabRule html = state.sidetabRules.stream()
                .filter(rule -> "HTML".equals(rule.name))
                .findFirst()
                .orElseThrow();
        html.sectionSpecs = List.of(
                new SidetabSectionSpec("Head", "@tag head"),
                new SidetabSectionSpec("Body", "@tag body")
        );

        SubtabsSettings settings = new SubtabsSettings();
        settings.loadState(state);

        CustomSidetabRule repaired = settings.getSidetabRules().stream()
                .filter(rule -> "HTML".equals(rule.name))
                .findFirst()
                .orElseThrow();
        assertTrue(repaired.sectionSpecs.size() >= 8);
        assertTrue(repaired.sectionsSummary().contains("Prolog"));
        assertTrue(repaired.sectionsSummary().contains("Header"));
        assertTrue(repaired.sectionsSummary().contains("Main"));
        assertEquals(11, settings.getState().sidetabRulesVersion);
    }

    @Test
    void resetToDefaultsRestoresFactorySettings() {
        SubtabsSettings settings = new SubtabsSettings();
        settings.setSubtabsActive(false);
        settings.setSidetabsActive(true);
        settings.setSidetabLayoutMode(SidetabLayoutMode.OVERLAY);
        settings.setSidetabsOnRight(false);
        settings.setBarHeightPercent(40);
        settings.setGroupColorsEnabled(true);
        settings.setGroupColorHex("demo", "#ff0000");

        settings.resetToDefaults();

        assertTrue(settings.isSubtabsActive());
        assertTrue(settings.isFamiliaEnabled());
        assertTrue(settings.isSidetabsActive());
        assertEquals(SidetabLayoutMode.BESIDE, settings.getSidetabLayoutMode());
        assertTrue(settings.isSidetabsOnRight());
        assertEquals(75, settings.getBarHeightPercent());
        assertTrue(settings.isGroupColorsEnabled());
        assertTrue(settings.getGroupColorHexes().isEmpty());
        assertEquals(16, settings.getSidetabRules().size());
        assertTrue(settings.getSidetabRules().stream()
                .filter(rule -> "HTML".equals(rule.name))
                .findFirst()
                .orElseThrow()
                .sectionsSummary()
                .contains("Prolog"));
    }

    @Test
    void loadStateKeepsExistingSectionSpecsWithoutCopying() {
        SubtabsSettings.State state = new SubtabsSettings.State();
        state.sidetabRulesVersion = SidetabRulesDefaults.VERSION;
        CustomSidetabRule html = state.sidetabRules.stream()
                .filter(rule -> "HTML".equals(rule.name))
                .findFirst()
                .orElseThrow();
        List<SidetabSectionSpec> originalSpecs = html.sectionSpecs;

        SubtabsSettings settings = new SubtabsSettings();
        settings.loadState(state);

        CustomSidetabRule loaded = settings.getSidetabRules().stream()
                .filter(rule -> "HTML".equals(rule.name))
                .findFirst()
                .orElseThrow();
        assertTrue(originalSpecs == loaded.sectionSpecs);
    }

    @Test
    void shipsWithBuiltInRules() {
        assertEquals(10, new SubtabsSettings.State().rules.size());
        assertEquals("npm", new SubtabsSettings.State().rules.get(0).name);
        assertEquals("tsconfig", new SubtabsSettings.State().rules.get(1).name);
        assertEquals("State Folder", new SubtabsSettings.State().rules.get(4).name);
        assertEquals("1", new SubtabsSettings.State().rules.get(3).groupNameSegments);
        assertEquals("1", new SubtabsSettings.State().rules.get(4).groupNameSegments);
        assertFalse(new SubtabsSettings.State().rules.get(4).searchNeighbors);
        assertEquals("state", new SubtabsSettings.State().rules.get(4).groupSuffix);
        assertFalse(new SubtabsSettings.State().rules.get(4).builtin);
        assertEquals("state", new SubtabsSettings.State().rules.get(3).groupSuffix);
        assertEquals("HTML", new SubtabsSettings.State().rules.get(6).name);
        assertFalse(new SubtabsSettings.State().rules.get(6).builtin);
        assertTrue(new SubtabsSettings.State().rules.get(6).excludePatterns.contains(".component.html"));
        assertEquals("Komponente", new SubtabsSettings.State().rules.get(7).name);
        assertTrue(new SubtabsSettings.State().rules.get(7).excludePatterns.contains(".actions.ts"));
        assertEquals("Eigene Gruppen", new SubtabsSettings.State().rules.get(8).name);
        assertTrue(new SubtabsSettings.State().rules.get(8).enabled);
        assertTrue(new SubtabsSettings.State().rules.get(8).builtin);
        assertEquals("Ordner", new SubtabsSettings.State().rules.get(9).name);
        assertTrue(new SubtabsSettings.State().rules.get(9).enabled);
        assertTrue(new SubtabsSettings.State().rules.get(9).builtin);
    }
}
