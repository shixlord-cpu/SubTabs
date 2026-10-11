package com.zayax.tabz;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabzSettingsTest {
    @Test
    void showsTabzByDefault() {
        assertTrue(new TabzSettings.State().subtabsActive);
    }

    @Test
    void showsCollapseButtonByDefault() {
        assertTrue(new TabzSettings.State().showCollapseButton);
    }

    @Test
    void offersProjectViewGroupingByDefault() {
        TabzSettings.State state = new TabzSettings.State();
        assertTrue(state.projectViewGroupingEnabled);
        assertTrue(state.projectViewGroupingActive);
    }

    @Test
    void fitsTabsToEditorWidthByDefault() {
        assertTrue(new TabzSettings.State().fitTabsToEditorWidth);
    }

    @Test
    void usesCompactBarHeightByDefault() {
        assertEquals(75, new TabzSettings.State().barHeightPercent);
    }

    @Test
    void usesIdeStandardTabFontStyleByDefault() {
        assertEquals(TabFontStyle.IDE_STANDARD.name(), new TabzSettings.State().tabFontStyle);
        assertEquals("IDE default", TabFontStyle.IDE_STANDARD.label());
        assertEquals("Monospace", TabFontStyle.MONOSPACED.label());
    }

    @Test
    void usesRelativeTextSizeByDefault() {
        assertEquals(75, new TabzSettings.State().textSizePercent);
    }

    @Test
    void usesScrollbarOverflowByDefault() {
        assertEquals("SCROLLBAR", new TabzSettings.State().overflowMode);
    }

    @Test
    void usesDefaultGroupTreeControlsByDefault() {
        assertEquals("DEFAULT", new TabzSettings.State().groupTreeControlStyle);
        assertFalse(new TabzSettings.State().invertGroupTreeControlFill);
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
        assertTrue(new TabzSettings.State().groupColorsEnabled);
        assertTrue(new TabzSettings.State().groupColorHexes.isEmpty());
        assertEquals(0, new TabzSettings.State().nextGroupColorIndex);
    }

    @Test
    void namesArrowOverflowRandpfeile() {
        assertEquals("Edge arrows", SubtabOverflowMode.ARROWS.label());
        assertEquals("Edge arrows", SubtabOverflowMode.ARROWS.toString());
        assertEquals("Scrollbar", SubtabOverflowMode.SCROLLBAR.label());
    }

    @Test
    void enablesSidetabsByDefault() {
        TabzSettings settings = new TabzSettings();
        assertTrue(settings.isSidetabsActive());
        assertTrue(settings.isSidetabsExpanded());
        assertEquals(SidetabLayoutMode.BESIDE, settings.getSidetabLayoutMode());
        assertFalse(settings.isSidetabsCombineComments());
    }

    @Test
    void defaultsSidetabLayoutModeToBeside() {
        assertEquals(SidetabLayoutMode.BESIDE, new TabzSettings().getSidetabLayoutMode());
    }

    @Test
    void includesTopCommentsAsSpecialBuiltinRule() {
        List<CustomSidetabRule> rules = new TabzSettings.State().sidetabRules;
        assertEquals(CustomSidetabRule.TOP_RULE_NAME, rules.get(0).name);
        assertTrue(rules.get(0).isTopRule());
        assertTrue(rules.get(0).builtin);
    }

    @Test
    void shipsWithBuiltInSidetabRules() {
        List<CustomSidetabRule> rules = new TabzSettings.State().sidetabRules;
        assertEquals(16, rules.size());
        assertEquals(CustomSidetabRule.TOP_RULE_NAME, rules.get(0).name);
        assertEquals("Tests", rules.get(1).name);
        assertEquals("TS-Component", rules.get(2).name);
        assertEquals("HTML", rules.get(3).name);
        assertEquals("CSS", rules.get(4).name);
        assertEquals("Override file rules", rules.get(0).sectionsSummary());
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
        TabzSettings.State state = new TabzSettings.State();
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

        TabzSettings settings = new TabzSettings();
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
        TabzSettings.State state = new TabzSettings.State();
        state.sidetabRulesVersion = 6;
        CustomSidetabRule html = state.sidetabRules.stream()
                .filter(rule -> "HTML".equals(rule.name))
                .findFirst()
                .orElseThrow();
        html.sectionSpecs = List.of(
                new SidetabSectionSpec("Head", "@tag head"),
                new SidetabSectionSpec("Body", "@tag body")
        );

        TabzSettings settings = new TabzSettings();
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
        TabzSettings settings = new TabzSettings();
        settings.setSubtabsActive(false);
        settings.setSidetabsActive(true);
        settings.setSidetabLayoutMode(SidetabLayoutMode.OVERLAY);
        settings.setSidetabsOnRight(false);
        settings.setBarHeightPercent(40);
        settings.setGroupColorsEnabled(true);
        settings.setGroupColorHex("demo", "#ff0000");

        settings.resetToDefaults();

        assertTrue(settings.isSubtabsActive());
        assertTrue(settings.isTabzEnabled());
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
        TabzSettings.State state = new TabzSettings.State();
        state.sidetabRulesVersion = SidetabRulesDefaults.VERSION;
        CustomSidetabRule html = state.sidetabRules.stream()
                .filter(rule -> "HTML".equals(rule.name))
                .findFirst()
                .orElseThrow();
        List<SidetabSectionSpec> originalSpecs = html.sectionSpecs;

        TabzSettings settings = new TabzSettings();
        settings.loadState(state);

        CustomSidetabRule loaded = settings.getSidetabRules().stream()
                .filter(rule -> "HTML".equals(rule.name))
                .findFirst()
                .orElseThrow();
        assertTrue(originalSpecs == loaded.sectionSpecs);
    }

    @Test
    void shipsWithBuiltInRules() {
        List<CustomSubtabRule> rules = new TabzSettings.State().rules;
        assertEquals(17, rules.size());
        assertEquals("npm", rules.get(0).name);
        assertEquals("tsconfig", rules.get(1).name);
        assertEquals("State Central", rules.get(3).name);
        assertEquals("State Feature", rules.get(4).name);
        assertEquals("2, 2, 2, 2, 2, 2, 2, 2", rules.get(3).nameSegments);
        assertEquals("1", rules.get(3).groupNameSegments);
        assertEquals("1, 1, 1, 1, 1, 1, 1, 1", rules.get(4).nameSegments);
        assertEquals("2", rules.get(4).groupNameSegments);
        assertTrue(rules.get(4).searchNeighbors);
        assertEquals("state", rules.get(4).groupSuffix);
        assertFalse(rules.get(4).builtin);
        assertEquals("state", rules.get(3).groupSuffix);
        assertEquals("Spring Boot", rules.get(6).name);
        assertEquals("ASP.NET", rules.get(7).name);
        assertEquals("React", rules.get(8).name);
        assertEquals("Vue", rules.get(9).name);
        assertEquals("Nest", rules.get(10).name);
        assertEquals("Playwright", rules.get(11).name);
        assertEquals("Cypress", rules.get(12).name);
        CustomSubtabRule html = rules.stream().filter(rule -> "HTML".equals(rule.name)).findFirst().orElseThrow();
        assertFalse(html.builtin);
        assertTrue(html.excludePatterns.contains(".component.html"));
        CustomSubtabRule component = rules.stream().filter(rule -> "Komponente".equals(rule.name)).findFirst().orElseThrow();
        assertTrue(component.excludePatterns.contains(".actions.ts"));
        CustomSubtabRule userGroups = rules.stream().filter(rule -> "Custom groups".equals(rule.name)).findFirst().orElseThrow();
        assertTrue(userGroups.enabled);
        assertTrue(userGroups.builtin);
        CustomSubtabRule folder = rules.stream().filter(rule -> "Folder".equals(rule.name)).findFirst().orElseThrow();
        assertTrue(folder.enabled);
        assertTrue(folder.builtin);
    }

    @Test
    void opensOtherSavedSplitPairFileNormallyByDefault() {
        assertEquals(
                SplittabOtherPairFileMode.OPEN_NORMALLY.name(),
                new TabzSettings.State().splittabOtherPairFileMode
        );
        assertEquals(SplittabOtherPairFileMode.OPEN_NORMALLY, new TabzSettings().getSplittabOtherPairFileMode());
        assertEquals(
                "Switch to the split pair that owns the file",
                SplittabOtherPairFileMode.SWITCH_TO_PAIR.label()
        );
        assertEquals(
                "Leave split pair and open the file normally",
                SplittabOtherPairFileMode.OPEN_NORMALLY.label()
        );
    }
}
