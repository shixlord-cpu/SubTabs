package com.zayax.tabz;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service(Service.Level.APP)
@State(name = "ComponentSubtabsSettings", storages = @Storage("componentTabz.xml"))
public final class TabzSettings implements PersistentStateComponent<TabzSettings.State> {
    private static final int CURRENT_RULES_VERSION = 19;

    private State state = new State();

    public static @NotNull TabzSettings getInstance() {
        return ApplicationManager.getApplication().getService(TabzSettings.class);
    }

    public boolean isSubtabsActive() {
        return state.subtabsActive;
    }

    public void setSubtabsActive(boolean active) {
        state.subtabsActive = active;
    }

    /**
     * When enabled, opening a file that belongs to a tabz group reuses an already open main tab of
     * that group (in-tab swap) instead of adding another main tab. Does not apply during an active
     * splittab pair session ({@link ComponentSubtabEditorSplitNavigation#blocksExternalFileOpen}).
     */
    public boolean isReuseOpenSubtabGroupMainTab() {
        return state.reuseOpenSubtabGroupMainTab;
    }

    public void setReuseOpenSubtabGroupMainTab(boolean reuse) {
        state.reuseOpenSubtabGroupMainTab = reuse;
    }

    public boolean isTabzEnabled() {
        return state.tabzEnabled;
    }

    public void setTabzEnabled(boolean enabled) {
        state.tabzEnabled = enabled;
        state.tabzEnabledKnown = true;
    }

    public boolean isShowCollapseButton() {
        return state.showCollapseButton;
    }

    public void setShowCollapseButton(boolean show) {
        state.showCollapseButton = show;
    }

    public int getBarHeightPercent() {
        return state.barHeightPercent;
    }

    public void setBarHeightPercent(int percent) {
        state.barHeightPercent = clamp(percent, 25, 100);
    }

    public int getTextSizePercent() {
        return state.textSizePercent;
    }

    public void setTextSizePercent(int percent) {
        state.textSizePercent = clamp(percent, 50, 100);
    }

    public @NotNull TabFontStyle getTabFontStyle() {
        return TabFontStyle.fromPersisted(state.tabFontStyle);
    }

    public void setTabFontStyle(@NotNull TabFontStyle style) {
        state.tabFontStyle = style.name();
    }

    /** Whether the project-view grouping control (Gruppenverzeichnis) is offered. */
    public boolean isProjectViewGroupingEnabled() {
        return state.projectViewGroupingEnabled;
    }

    public void setProjectViewGroupingEnabled(boolean enabled) {
        state.projectViewGroupingEnabled = enabled;
    }

    /** Whether related files are currently grouped in the project view (icon toggle). */
    public boolean isProjectViewGroupingActive() {
        return state.projectViewGroupingActive;
    }

    public void setProjectViewGroupingActive(boolean active) {
        state.projectViewGroupingActive = active;
    }

    /** @deprecated use {@link #isProjectViewGroupingEnabled()} and {@link #isProjectViewGroupingActive()} */
    @Deprecated
    public boolean isGroupRelatedFilesInProjectView() {
        return isProjectViewGroupingEnabled() && isProjectViewGroupingActive();
    }

    /** @deprecated use {@link #setProjectViewGroupingEnabled(boolean)} / {@link #setProjectViewGroupingActive(boolean)} */
    @Deprecated
    public void setGroupRelatedFilesInProjectView(boolean group) {
        state.projectViewGroupingEnabled = group;
        state.projectViewGroupingActive = group;
    }

    public boolean isFitTabsToEditorWidth() {
        return state.fitTabsToEditorWidth;
    }

    public void setFitTabsToEditorWidth(boolean fit) {
        state.fitTabsToEditorWidth = fit;
    }

    public @NotNull SubtabOverflowMode getOverflowMode() {
        return SubtabOverflowMode.fromPersisted(state.overflowMode);
    }

    public void setOverflowMode(@NotNull SubtabOverflowMode mode) {
        state.overflowMode = mode.name();
    }

    public @NotNull SubtabGroupTreeControlStyle getGroupTreeControlStyle() {
        return SubtabGroupTreeControlStyle.fromPersisted(state.groupTreeControlStyle);
    }

    public void setGroupTreeControlStyle(@NotNull SubtabGroupTreeControlStyle style) {
        state.groupTreeControlStyle = style.name();
    }

    public boolean isInvertGroupTreeControlFill() {
        return state.invertGroupTreeControlFill;
    }

    public void setInvertGroupTreeControlFill(boolean invert) {
        state.invertGroupTreeControlFill = invert;
    }

    public boolean isGroupColorsEnabled() {
        return state.groupColorsEnabled;
    }

    public void setGroupColorsEnabled(boolean enabled) {
        state.groupColorsEnabled = enabled;
    }

    public boolean isHoverViewEnabled() {
        return state.hoverViewEnabled;
    }

    public void setHoverViewEnabled(boolean enabled) {
        state.hoverViewEnabled = enabled;
    }

    public boolean isHoverViewProjectToEditorEnabled() {
        return state.hoverViewProjectToEditorEnabled;
    }

    public void setHoverViewProjectToEditorEnabled(boolean enabled) {
        state.hoverViewProjectToEditorEnabled = enabled;
    }

    public boolean isShowSubtabNameInMainTab() {
        return state.showSubtabNameInMainTab;
    }

    public void setShowSubtabNameInMainTab(boolean show) {
        state.showSubtabNameInMainTab = show;
    }

    public @NotNull Map<String, String> getGroupColorHexes() {
        if (state.groupColorHexes == null) {
            state.groupColorHexes = new LinkedHashMap<>();
        }
        return state.groupColorHexes;
    }

    public @Nullable String getGroupColorHex(@NotNull String key) {
        return getGroupColorHexes().get(key);
    }

    public void setGroupColorHex(@NotNull String key, @NotNull String hex) {
        getGroupColorHexes().put(key, hex);
    }

    public int getNextGroupColorIndex() {
        return state.nextGroupColorIndex;
    }

    public void setNextGroupColorIndex(int index) {
        state.nextGroupColorIndex = Math.max(0, index);
    }

    public @NotNull List<String> getSubtabGroupOrder(@NotNull String groupKey) {
        if (state.subtabGroupOrders == null) {
            state.subtabGroupOrders = new LinkedHashMap<>();
        }
        List<String> order = state.subtabGroupOrders.get(groupKey);
        return order == null ? List.of() : List.copyOf(order);
    }

    public void setSubtabGroupOrder(@NotNull String groupKey, @NotNull List<String> filePaths) {
        if (state.subtabGroupOrders == null) {
            state.subtabGroupOrders = new LinkedHashMap<>();
        }
        state.subtabGroupOrders.put(groupKey, new ArrayList<>(filePaths));
    }

    public @NotNull List<CustomSubtabRule> getRules() {
        ensureSubtabRulesCurrent();
        return state.rules;
    }

    private transient int rulesGeneration;

    public int getRulesGeneration() {
        return rulesGeneration;
    }

    public void setRules(@NotNull List<CustomSubtabRule> rules) {
        state.rules = new ArrayList<>(rules);
        rulesGeneration++;
    }

    public boolean rotateMatchingSubtabRules(@NotNull String fileName) {
        List<CustomSubtabRule> rules = new ArrayList<>(getRules());
        if (!SubtabRuleRotation.rotateMatchingRules(fileName, rules)) {
            return false;
        }
        setRules(rules);
        return true;
    }

    public boolean isSidetabsActive() {
        return state.sidetabsActive;
    }

    public void setSidetabsActive(boolean active) {
        state.sidetabsActive = active;
    }

    public boolean isSidetabsExpanded() {
        return state.sidetabsExpanded;
    }

    public void setSidetabsExpanded(boolean expanded) {
        state.sidetabsExpanded = expanded;
    }

    public boolean isSidetabsOnRight() {
        return state.sidetabsOnRight;
    }

    public void setSidetabsOnRight(boolean onRight) {
        state.sidetabsOnRight = onRight;
    }

    public @NotNull SidetabLayoutMode getSidetabLayoutMode() {
        return SidetabLayoutMode.fromPersisted(state.sidetabLayoutMode);
    }

    public void setSidetabLayoutMode(@NotNull SidetabLayoutMode mode) {
        state.sidetabLayoutMode = mode.name();
    }

    public int getSidetabBesideColumnWidth() {
        int min = com.intellij.util.ui.JBUI.scale(48);
        int max = com.intellij.util.ui.JBUI.scale(480);
        int stored = state.sidetabBesideColumnWidth;
        if (stored <= 0) {
            stored = SidetabBarPanel.defaultBesideColumnWidth();
        }
        return Math.max(min, Math.min(max, stored));
    }

    public void setSidetabBesideColumnWidth(int width) {
        state.sidetabBesideColumnWidth = width;
    }

    public boolean isSplittabsEnabled() {
        return state.splittabsEnabled;
    }

    public void setSplittabsEnabled(boolean enabled) {
        state.splittabsEnabled = enabled;
    }

    public @NotNull SplittabBehaviorMode getSplittabBehaviorMode() {
        return SplittabBehaviorMode.fromPersisted(state.splittabBehaviorMode);
    }

    public void setSplittabBehaviorMode(@NotNull SplittabBehaviorMode mode) {
        state.splittabBehaviorMode = mode.name();
    }

    public boolean isRestoreSwitchSplittabSessionOnProjectOpen() {
        return state.restoreSwitchSplittabSessionOnProjectOpen;
    }

    public void setRestoreSwitchSplittabSessionOnProjectOpen(boolean restore) {
        state.restoreSwitchSplittabSessionOnProjectOpen = restore;
    }

    public @Nullable String getSettingsDialogSelectedTabTitle() {
        return state.settingsDialogSelectedTabTitle;
    }

    public void setSettingsDialogSelectedTabTitle(@Nullable String title) {
        if (title == null || title.isBlank()) {
            state.settingsDialogSelectedTabTitle = null;
        } else {
            state.settingsDialogSelectedTabTitle = title.trim();
        }
    }

    public @NotNull SplittabDissolveMode getSplittabDissolveMode() {
        return SplittabDissolveMode.fromPersisted(state.splittabDissolveMode);
    }

    public void setSplittabDissolveMode(@NotNull SplittabDissolveMode mode) {
        state.splittabDissolveMode = mode.name();
    }

    public @NotNull SplittabOtherPairFileMode getSplittabOtherPairFileMode() {
        return SplittabOtherPairFileMode.fromPersisted(state.splittabOtherPairFileMode);
    }

    public void setSplittabOtherPairFileMode(@NotNull SplittabOtherPairFileMode mode) {
        state.splittabOtherPairFileMode = mode.name();
    }

    public boolean isSidetabsCombineComments() {
        CustomSidetabRule family = SidetabRulesDefaults.findTopRule(getSidetabRules());
        return family != null && family.familyMode == TopCommentMode.COMBINE;
    }

    public void setSidetabsCombineComments(boolean combine) {
        CustomSidetabRule family = SidetabRulesDefaults.findTopRule(getSidetabRules());
        if (family != null) {
            family.familyMode = combine ? TopCommentMode.COMBINE : TopCommentMode.OVERRIDE;
        }
    }

    public @NotNull List<CustomSidetabRule> getSidetabRules() {
        if (state.sidetabRules == null) {
            state.sidetabRules = new ArrayList<>();
        }
        return state.sidetabRules;
    }

    private transient int sidetabRulesGeneration;

    public int getSidetabRulesGeneration() {
        return sidetabRulesGeneration;
    }

    public void setSidetabRules(@NotNull List<CustomSidetabRule> rules) {
        state.sidetabRules = new ArrayList<>(rules);
        sidetabRulesGeneration++;
    }

    public void resetToDefaults() {
        state = new State();
        state.tabzEnabledKnown = true;
        migrateRulesIfNeeded();
        rulesGeneration++;
        sidetabRulesGeneration++;
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
        if (this.state.rules == null) {
            this.state.rules = new ArrayList<>();
        }
        if (this.state.sidetabRules == null) {
            this.state.sidetabRules = new ArrayList<>();
        }
        migrateTabzEnabledIfNeeded();
        migrateProjectViewGroupingIfNeeded();
        migrateRulesIfNeeded();
    }

    private void migrateProjectViewGroupingIfNeeded() {
        if (state.projectViewGroupingSplitKnown) {
            return;
        }
        boolean legacy = state.groupRelatedFilesInProjectView;
        state.projectViewGroupingEnabled = legacy;
        state.projectViewGroupingActive = legacy;
        state.projectViewGroupingSplitKnown = true;
    }

    private void migrateTabzEnabledIfNeeded() {
        if (!state.tabzEnabledKnown) {
            state.tabzEnabled = true;
            state.tabzEnabledKnown = true;
        }
    }

    private void migrateRulesIfNeeded() {
        ensureSubtabRulesCurrent();
        if (state.barHeightPercent < 25) {
            state.barHeightPercent = 75;
        }
        if (state.textSizePercent <= 0) {
            state.textSizePercent = 75;
        } else if (state.textSizePercent > 100) {
            state.textSizePercent = 100;
        }
        if (state.overflowMode == null || state.overflowMode.isBlank()) {
            state.overflowMode = SubtabOverflowMode.SCROLLBAR.name();
        }
        if (state.tabFontStyle == null || state.tabFontStyle.isBlank()) {
            state.tabFontStyle = TabFontStyle.IDE_STANDARD.name();
        }
        if (state.groupTreeControlStyle == null || state.groupTreeControlStyle.isBlank()) {
            state.groupTreeControlStyle = SubtabGroupTreeControlStyle.DEFAULT.name();
        }
        if (state.groupColorHexes == null) {
            state.groupColorHexes = new LinkedHashMap<>();
        }
        if (state.subtabGroupOrders == null) {
            state.subtabGroupOrders = new LinkedHashMap<>();
        }
        if (state.sidetabLayoutMode == null || state.sidetabLayoutMode.isBlank()) {
            migrateSidetabLayoutMode();
        }
        if (state.splittabBehaviorMode == null || state.splittabBehaviorMode.isBlank()) {
            state.splittabBehaviorMode = SplittabBehaviorMode.INTEGRATED.name();
        }
        if (state.splittabDissolveMode == null || state.splittabDissolveMode.isBlank()) {
            state.splittabDissolveMode = SplittabDissolveMode.DISSOLVE.name();
        }
        if (state.splittabOtherPairFileMode == null || state.splittabOtherPairFileMode.isBlank()) {
            state.splittabOtherPairFileMode = SplittabOtherPairFileMode.OPEN_NORMALLY.name();
        }
        migrateSidetabRulesIfNeeded();
        migrateGermanBuiltinRuleNames();
    }

    private void migrateGermanBuiltinRuleNames() {
        if (state.rules == null) {
            return;
        }
        for (CustomSubtabRule rule : state.rules) {
            if ("Eigene Gruppen".equals(rule.name)) {
                rule.name = "Custom groups";
            } else if ("Ordner".equals(rule.name)) {
                rule.name = "Folder";
            }
        }
    }

    private void migrateSidetabRulesIfNeeded() {
        if (state.sidetabRules == null) {
            state.sidetabRules = new ArrayList<>();
        }
        if (state.sidetabRules.isEmpty()) {
            state.sidetabRules = SidetabRulesDefaults.createDefaults();
            state.sidetabRulesVersion = SidetabRulesDefaults.VERSION;
            return;
        }

        boolean upgraded = state.sidetabRulesVersion < SidetabRulesDefaults.VERSION;
        if (upgraded) {
            SidetabRulesDefaults.applyLatestDefaults(state.sidetabRules);
            state.sidetabRulesVersion = SidetabRulesDefaults.VERSION;
        }

        boolean filledLegacySpecs = false;
        for (CustomSidetabRule rule : state.sidetabRules) {
            if (!rule.isTopRule() && (rule.sectionSpecs == null || rule.sectionSpecs.isEmpty())) {
                rule.sectionSpecs = SidetabRulesDefaults.specsFromLegacy(rule);
                filledLegacySpecs = true;
            }
        }

        if (upgraded || filledLegacySpecs || SidetabRulesDefaults.needsBuiltinSectionRepair(state.sidetabRules)) {
            SidetabRulesDefaults.repairBuiltinSectionSpecs(state.sidetabRules);
        }

        SidetabRulesDefaults.ensureTopRule(state.sidetabRules);
        CustomSidetabRule topRule = SidetabRulesDefaults.findTopRule(state.sidetabRules);
        if (topRule != null && state.sidetabsCombineComments) {
            topRule.familyMode = TopCommentMode.COMBINE;
        }

        if (upgraded) {
            for (CustomSidetabRule stock : SidetabRulesDefaults.createDefaults()) {
                CustomSidetabRule existing = findSidetabRuleByName(state.sidetabRules, stock.name);
                if (existing == null && stock.isTopRule()) {
                    existing = SidetabRulesDefaults.findTopRule(state.sidetabRules);
                }
                if (existing != null) {
                    existing.builtin = stock.builtin;
                    if (stock.isTopRule()) {
                        existing.type = CustomSidetabRule.Type.TOP;
                        if (CustomSidetabRule.LEGACY_FAMILY_RULE_NAME.equals(existing.name)) {
                            existing.name = CustomSidetabRule.TOP_RULE_NAME;
                        }
                    }
                }
            }
        }

        state.sidetabRulesVersion = Math.max(state.sidetabRulesVersion, SidetabRulesDefaults.VERSION);
    }

    private void migrateSidetabLayoutMode() {
        String legacy = state.sidetabPlacement;
        if ("ABOVE".equalsIgnoreCase(legacy) || "OVERLAY".equalsIgnoreCase(legacy)) {
            state.sidetabLayoutMode = SidetabLayoutMode.OVERLAY.name();
        } else {
            state.sidetabLayoutMode = SidetabLayoutMode.BESIDE.name();
            if ("BESIDE_LEFT".equalsIgnoreCase(legacy)) {
                state.sidetabsOnRight = false;
            }
        }
    }

    private static @Nullable CustomSidetabRule findSidetabRuleByName(
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

    private void ensureSubtabRulesCurrent() {
        if (state.rules == null || state.rules.isEmpty()) {
            resetSubtabRulesToDefaults();
            return;
        }
        if (state.rulesVersion != CURRENT_RULES_VERSION || containsLegacySubtabRules(state.rules)) {
            if (state.rules != null && !state.rules.isEmpty() && needsStateRulesMigration(state.rulesVersion, state.rules)) {
                migrateStateRules(state.rules, state.rulesVersion);
                state.rulesVersion = CURRENT_RULES_VERSION;
                rulesGeneration++;
                return;
            }
            resetSubtabRulesToDefaults();
        }
    }

    private static boolean needsStateRulesMigration(int rulesVersion, @NotNull List<CustomSubtabRule> rules) {
        if (rulesVersion < CURRENT_RULES_VERSION) {
            return true;
        }
        for (CustomSubtabRule rule : rules) {
            if ("State".equals(rule.name) || "State Folder".equals(rule.name)) {
                return true;
            }
        }
        return false;
    }

    private static void migrateStateRules(@NotNull List<CustomSubtabRule> rules, int fromVersion) {
        CustomSubtabRule centralDefaults = SubtabRulesDefaults.createDefaults().stream()
                .filter(rule -> "State Central".equals(rule.name))
                .findFirst()
                .orElseThrow();
        CustomSubtabRule featureDefaults = SubtabRulesDefaults.stateFeatureRule();

        for (CustomSubtabRule rule : rules) {
            if (rule.isSpecial()) {
                continue;
            }
            if (fromVersion < 17) {
                if ("State".equals(rule.name) || "State Central".equals(rule.name)) {
                    rule.name = "State Central";
                    rule.nameSegments = centralDefaults.nameSegments;
                    rule.groupNameSegments = centralDefaults.groupNameSegments;
                    rule.searchNeighbors = centralDefaults.searchNeighbors;
                    rule.groupSuffix = centralDefaults.groupSuffix;
                } else if ("State Folder".equals(rule.name) || "State Feature".equals(rule.name)) {
                    rule.name = "State Feature";
                    rule.nameSegments = featureDefaults.nameSegments;
                    rule.groupNameSegments = featureDefaults.groupNameSegments;
                    rule.searchNeighbors = featureDefaults.searchNeighbors;
                    rule.groupSuffix = featureDefaults.groupSuffix;
                }
            }
            if (fromVersion < 18 && "State Feature".equals(rule.name)) {
                rule.searchNeighbors = featureDefaults.searchNeighbors;
            }
        }
    }

    private void resetSubtabRulesToDefaults() {
        state.rules = copyDefaultSubtabRules();
        state.rulesVersion = CURRENT_RULES_VERSION;
        rulesGeneration++;
    }

    private static @NotNull List<CustomSubtabRule> copyDefaultSubtabRules() {
        List<CustomSubtabRule> defaults = new ArrayList<>(SubtabRulesDefaults.createDefaults().size());
        for (CustomSubtabRule rule : SubtabRulesDefaults.createDefaults()) {
            defaults.add(rule.copy());
        }
        return defaults;
    }

    private static boolean containsLegacySubtabRules(@NotNull List<CustomSubtabRule> rules) {
        for (CustomSubtabRule rule : rules) {
            if (rule.type == CustomSubtabRule.Type.STEM || rule.type == CustomSubtabRule.Type.FILES) {
                return true;
            }
            if ("State".equals(rule.name)
                    || "State Folder".equals(rule.name)
                    || "State Typ".equalsIgnoreCase(rule.name)
                    || "State Ordner".equalsIgnoreCase(rule.name)) {
                return true;
            }
        }
        return false;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class State {
        public boolean subtabsActive = true;
        public boolean reuseOpenSubtabGroupMainTab = true;
        public boolean tabzEnabled = true;
        public boolean tabzEnabledKnown = false;
        public boolean showCollapseButton = true;
        public boolean groupRelatedFilesInProjectView = true;
        public boolean projectViewGroupingEnabled = true;
        public boolean projectViewGroupingActive = true;
        public boolean projectViewGroupingSplitKnown = false;
        public boolean fitTabsToEditorWidth = true;
        public String overflowMode = "SCROLLBAR";
        public String groupTreeControlStyle = "DEFAULT";
        public boolean invertGroupTreeControlFill = false;
        public boolean groupColorsEnabled = true;
        public boolean hoverViewEnabled = true;
        public boolean hoverViewProjectToEditorEnabled = true;
        public boolean showSubtabNameInMainTab = false;
        public Map<String, String> groupColorHexes = new LinkedHashMap<>();
        public Map<String, List<String>> subtabGroupOrders = new LinkedHashMap<>();
        public int nextGroupColorIndex = 0;
        public int barHeightPercent = 75;
        public int textSizePercent = 75;
        public String tabFontStyle = TabFontStyle.IDE_STANDARD.name();
        public int rulesVersion = 0;
        public List<CustomSubtabRule> rules = SubtabRulesDefaults.createDefaults();
        public boolean sidetabsActive = true;
        public boolean sidetabsExpanded = true;
        public boolean sidetabsOnRight = true;
        public String sidetabLayoutMode = SidetabLayoutMode.BESIDE.name();
        public String sidetabPlacement = "";
        public boolean sidetabsCombineComments = true;
        public int sidetabRulesVersion = SidetabRulesDefaults.VERSION;
        public List<CustomSidetabRule> sidetabRules = SidetabRulesDefaults.createDefaults();
        public int sidetabBesideColumnWidth = 0;
        public boolean splittabsEnabled = true;
        public String splittabBehaviorMode = SplittabBehaviorMode.INTEGRATED.name();
        public boolean restoreSwitchSplittabSessionOnProjectOpen = false;
        public @Nullable String settingsDialogSelectedTabTitle;
        public String splittabDissolveMode = SplittabDissolveMode.DISSOLVE.name();
        public String splittabOtherPairFileMode = SplittabOtherPairFileMode.OPEN_NORMALLY.name();
    }
}
