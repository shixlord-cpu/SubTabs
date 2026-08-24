package de.sasbe.subtabs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Service(Service.Level.APP)
@State(name = "ComponentSubtabsSettings", storages = @Storage("componentSubtabs.xml"))
public final class SubtabsSettings implements PersistentStateComponent<SubtabsSettings.State> {
    private static final int CURRENT_RULES_VERSION = 5;

    private State state = new State();

    public static @NotNull SubtabsSettings getInstance() {
        return ApplicationManager.getApplication().getService(SubtabsSettings.class);
    }

    public boolean isScrollProjectViewOnSubtabHover() {
        return state.scrollProjectViewOnSubtabHover;
    }

    public void setScrollProjectViewOnSubtabHover(boolean scroll) {
        state.scrollProjectViewOnSubtabHover = scroll;
    }

    public boolean isSubtabsActive() {
        return state.subtabsActive;
    }

    public void setSubtabsActive(boolean active) {
        state.subtabsActive = active;
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

    public @NotNull List<CustomSubtabRule> getRules() {
        return state.rules;
    }

    public void setRules(@NotNull List<CustomSubtabRule> rules) {
        state.rules = new ArrayList<>(rules);
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
        migrateRulesIfNeeded();
    }

    private void migrateRulesIfNeeded() {
        if (state.rules.isEmpty()) {
            state.rules = SubtabRulesDefaults.createDefaults();
            state.rulesVersion = CURRENT_RULES_VERSION;
        } else if (state.rulesVersion < CURRENT_RULES_VERSION) {
            normalizeRuleFields(state.rules);
            applyDefaultGroupSuffixes(state.rules);
            removeLegacyCustomGroupRules(state.rules);
            ensureSpecialRules(state.rules);
            state.rulesVersion = CURRENT_RULES_VERSION;
        } else {
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
        if (state.groupTreeControlStyle == null || state.groupTreeControlStyle.isBlank()) {
            state.groupTreeControlStyle = SubtabGroupTreeControlStyle.DEFAULT.name();
        }
    }

    private static void normalizeRuleFields(@NotNull List<CustomSubtabRule> rules) {
        for (CustomSubtabRule rule : rules) {
            if (rule.groupSuffix == null) {
                rule.groupSuffix = "";
            }
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
                rule.groupSuffix = "components";
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class State {
        public boolean scrollProjectViewOnSubtabHover = false;
        public boolean subtabsActive = true;
        public boolean showCollapseButton = true;
        public boolean groupRelatedFilesInProjectView = true;
        public boolean fitTabsToEditorWidth = true;
        public String overflowMode = "SCROLLBAR";
        public String groupTreeControlStyle = "DEFAULT";
        public boolean invertGroupTreeControlFill = false;
        public int barHeightPercent = 75;
        public int textSizePercent = 75;
        public int rulesVersion = 0;
        public List<CustomSubtabRule> rules = SubtabRulesDefaults.createDefaults();
    }
}
