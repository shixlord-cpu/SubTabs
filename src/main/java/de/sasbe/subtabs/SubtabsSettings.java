package de.sasbe.subtabs;

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
@State(name = "ComponentSubtabsSettings", storages = @Storage("componentSubtabs.xml"))
public final class SubtabsSettings implements PersistentStateComponent<SubtabsSettings.State> {
    private static final int CURRENT_RULES_VERSION = 6;

    private State state = new State();

    public static @NotNull SubtabsSettings getInstance() {
        return ApplicationManager.getApplication().getService(SubtabsSettings.class);
    }

    public boolean isSubtabsActive() {
        return state.subtabsActive;
    }

    public void setSubtabsActive(boolean active) {
        state.subtabsActive = active;
    }

    public boolean isFamiliaEnabled() {
        return state.familiaEnabled;
    }

    public void setFamiliaEnabled(boolean enabled) {
        state.familiaEnabled = enabled;
        state.familiaEnabledKnown = true;
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

    public boolean isGroupRelatedFilesInProjectView() {
        return state.groupRelatedFilesInProjectView;
    }

    public void setGroupRelatedFilesInProjectView(boolean group) {
        state.groupRelatedFilesInProjectView = group;
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

    public @NotNull List<CustomSubtabRule> getRules() {
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
        state.familiaEnabledKnown = true;
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
        migrateFamiliaEnabledIfNeeded();
        migrateRulesIfNeeded();
    }

    private void migrateFamiliaEnabledIfNeeded() {
        if (!state.familiaEnabledKnown) {
            state.familiaEnabled = true;
            state.familiaEnabledKnown = true;
        }
    }

    private void migrateRulesIfNeeded() {
        if (state.rules.isEmpty()) {
            state.rules = SubtabRulesDefaults.createDefaults();
            state.rulesVersion = CURRENT_RULES_VERSION;
        } else if (state.rulesVersion < CURRENT_RULES_VERSION) {
            normalizeRuleFields(state.rules);
            applyDefaultGroupSuffixes(state.rules);
            removeLegacyCustomGroupRules(state.rules);
            ensureHtmlRule(state.rules);
            ensureSpecialRules(state.rules);
            state.rulesVersion = CURRENT_RULES_VERSION;
        } else {
            ensureHtmlRule(state.rules);
            ensureSpecialRules(state.rules);
        }
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
        if (state.sidetabLayoutMode == null || state.sidetabLayoutMode.isBlank()) {
            migrateSidetabLayoutMode();
        }
        migrateSidetabRulesIfNeeded();
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

    private static void normalizeRuleFields(@NotNull List<CustomSubtabRule> rules) {
        for (CustomSubtabRule rule : rules) {
            if (rule.groupSuffix == null) {
                rule.groupSuffix = "";
            }
            if (rule.excludeStemSuffixes == null) {
                rule.excludeStemSuffixes = "";
            }
        }
    }

    private static void ensureHtmlRule(@NotNull List<CustomSubtabRule> rules) {
        for (CustomSubtabRule rule : rules) {
            if ("HTML".equalsIgnoreCase(rule.name) && rule.type == CustomSubtabRule.Type.STEM) {
                rule.builtin = true;
                if (rule.excludeStemSuffixes == null || rule.excludeStemSuffixes.isBlank()) {
                    rule.excludeStemSuffixes = ".component";
                }
                return;
            }
        }

        int componentIndex = -1;
        for (int index = 0; index < rules.size(); index++) {
            if ("Komponente".equalsIgnoreCase(rules.get(index).name)) {
                componentIndex = index;
                break;
            }
        }
        if (componentIndex >= 0) {
            rules.add(componentIndex, SubtabRulesDefaults.htmlRule());
        } else {
            int folderIndex = rules.size();
            for (int index = 0; index < rules.size(); index++) {
                if (rules.get(index).type == CustomSubtabRule.Type.FOLDER) {
                    folderIndex = index;
                    break;
                }
            }
            rules.add(Math.max(0, folderIndex), SubtabRulesDefaults.htmlRule());
        }
    }

    private static void ensureSpecialRules(@NotNull List<CustomSubtabRule> rules) {
        boolean hasUserGroupsRule = false;
        boolean hasFolderRule = false;
        for (CustomSubtabRule rule : rules) {
            if (rule.type == CustomSubtabRule.Type.USER_GROUPS) {
                hasUserGroupsRule = true;
                rule.builtin = true;
                if (rule.name.isBlank()) {
                    rule.name = "Eigene Gruppen";
                }
            } else if (rule.type == CustomSubtabRule.Type.FOLDER) {
                hasFolderRule = true;
                rule.builtin = true;
                if (rule.name.isBlank()) {
                    rule.name = "Ordner";
                }
            }
        }
        if (!hasUserGroupsRule) {
            int insertIndex = hasFolderRule ? Math.max(0, rules.size() - 1) : rules.size();
            rules.add(insertIndex, SubtabRulesDefaults.userGroupsRule());
        }
        if (!hasFolderRule) {
            rules.add(SubtabRulesDefaults.folderRule());
        }
    }

    private static void removeLegacyCustomGroupRules(@NotNull List<CustomSubtabRule> rules) {
        rules.removeIf(rule -> "CUSTOM_GROUPS".equals(String.valueOf(rule.type)));
    }

    private static void applyDefaultGroupSuffixes(@NotNull List<CustomSubtabRule> rules) {
        for (CustomSubtabRule rule : rules) {
            if (rule.groupSuffix != null && !rule.groupSuffix.isBlank()) {
                continue;
            }
            if ("State".equalsIgnoreCase(rule.name)) {
                rule.groupSuffix = "state";
            } else if ("Komponente".equalsIgnoreCase(rule.name)) {
                rule.groupSuffix = "component";
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class State {
        public boolean subtabsActive = true;
        public boolean familiaEnabled = true;
        public boolean familiaEnabledKnown = false;
        public boolean showCollapseButton = true;
        public boolean groupRelatedFilesInProjectView = true;
        public boolean fitTabsToEditorWidth = true;
        public String overflowMode = "SCROLLBAR";
        public String groupTreeControlStyle = "DEFAULT";
        public boolean invertGroupTreeControlFill = false;
        public boolean groupColorsEnabled = true;
        public boolean hoverViewEnabled = true;
        public boolean showSubtabNameInMainTab = false;
        public Map<String, String> groupColorHexes = new LinkedHashMap<>();
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
    }
}
