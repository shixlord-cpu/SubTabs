package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class SubtabRulesDefaults {
    private SubtabRulesDefaults() {
    }

    public static @NotNull List<CustomSubtabRule> createDefaults() {
        List<CustomSubtabRule> rules = new ArrayList<>(9);
        rules.add(npmRule());
        rules.add(tsconfigRule());
        rules.add(envRule());
        rules.add(stateRule());
        rules.add(stateFeatureRule());
        rules.add(modelRule());
        rules.add(htmlRule());
        rules.add(componentRule());
        rules.add(userGroupsRule());
        rules.add(folderRule());
        return rules;
    }

    static @NotNull CustomSubtabRule userGroupsRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Eigene Gruppen";
        rule.type = CustomSubtabRule.Type.USER_GROUPS;
        rule.nameSegments = "1";
        rule.enabled = true;
        rule.builtin = true;
        return rule;
    }

    static @NotNull CustomSubtabRule folderRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Ordner";
        rule.type = CustomSubtabRule.Type.FOLDER;
        rule.nameSegments = "1";
        rule.enabled = true;
        rule.builtin = true;
        return rule;
    }

    private static @NotNull CustomSubtabRule npmRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "npm";
        rule.patterns = """
                package.json, package-lock.json, npm-shrinkwrap.json, yarn.lock, pnpm-lock.yaml,\
                 bun.lock, bun.lockb, .npmrc, .nvmrc, .node-version""".replace('\n', ' ').trim();
        rule.nameSegments = "1, 1, 1, 1, 1, 1, 1, 2, 2, 2";
        rule.groupNameSegments = "1";
        rule.slotKeys = "package.json, lock, lock, yarn.lock, pnpm-lock.yaml, bun.lock, bun.lockb, .npmrc, nvm, nvm";
        return rule;
    }

    private static @NotNull CustomSubtabRule tsconfigRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "tsconfig";
        rule.patterns = """
                tsconfig.json, tsconfig.base.json, tsconfig.app.json, tsconfig.spec.json,\
                 tsconfig.lib.json, tsconfig.editor.json, tsconfig.build.json""".replace('\n', ' ').trim();
        rule.nameSegments = "1, 2, 2, 2, 2, 2, 2";
        rule.groupNameSegments = "1";
        return rule;
    }

    private static @NotNull CustomSubtabRule envRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "env";
        rule.patterns = """
                .env, .env.local, .env.example, .env.sample, .env.development, .env.production, .env.test""".replace('\n', ' ').trim();
        rule.nameSegments = "2, 3, 3, 3, 3, 3, 3";
        rule.groupNameSegments = "2";
        return rule;
    }

    private static @NotNull CustomSubtabRule stateRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "State Central";
        rule.patterns = """
                .actions.ts, .reducer.ts, .reducers.ts, .effects.ts, .selectors.ts, .state.ts, .store.ts, .facade.ts""".replace('\n', ' ').trim();
        rule.nameSegments = "2, 2, 2, 2, 2, 2, 2, 2";
        rule.groupNameSegments = "1";
        rule.slotKeys = """
                .actions.ts, .reducer.ts, .reducer.ts, .effects.ts, .selectors.ts, .state.ts, .store.ts, .facade.ts""".replace('\n', ' ').trim();
        rule.searchNeighbors = true;
        rule.groupSuffix = "state";
        return rule;
    }

    static @NotNull CustomSubtabRule stateFeatureRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "State Feature";
        rule.patterns = """
                .actions.ts, .reducer.ts, .reducers.ts, .effects.ts, .selectors.ts, .state.ts, .store.ts, .facade.ts""".replace('\n', ' ').trim();
        rule.nameSegments = "1, 1, 1, 1, 1, 1, 1, 1";
        rule.groupNameSegments = "2";
        rule.slotKeys = """
                .actions.ts, .reducer.ts, .reducer.ts, .effects.ts, .selectors.ts, .state.ts, .store.ts, .facade.ts""".replace('\n', ' ').trim();
        rule.searchNeighbors = false;
        rule.groupSuffix = "state";
        return rule;
    }

    private static @NotNull CustomSubtabRule modelRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Model";
        rule.patterns = """
                .interface.ts, .entity.ts, .mapper.ts, .model.ts, .mock.ts, .dto.ts, .type.ts""".replace('\n', ' ').trim();
        rule.nameSegments = "2, 2, 2, 2, 2, 2, 2";
        rule.groupNameSegments = "1";
        return rule;
    }

    static @NotNull String defaultStateExcludePatterns() {
        return """
                .actions.ts, .reducer.ts, .reducers.ts, .effects.ts, .selectors.ts, .state.ts, .store.ts, .facade.ts""".replace('\n', ' ').trim();
    }

    private static @NotNull CustomSubtabRule componentRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Komponente";
        rule.patterns = ".spec.ts, .test.ts, .ts, .html, .scss, .sass, .css, .less";
        rule.nameSegments = "-2, -2, 2, -1, -1, -1, -1, -1";
        rule.groupNameSegments = "1";
        rule.slotKeys = ".spec.ts, .test.ts, .ts, .html, style, style, style, style";
        rule.excludePatterns = defaultStateExcludePatterns();
        rule.groupSuffix = "component";
        return rule;
    }

    static @NotNull CustomSubtabRule htmlRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "HTML";
        rule.patterns = ".html, .htm, .xhtml, .css, .js";
        rule.nameSegments = "1, 1, 1, -1, -1";
        rule.groupNameSegments = "1";
        rule.slotKeys = ".html, .htm, .xhtml, style, script";
        rule.excludePatterns = ".component.html, .component.htm, .component.xhtml, .component.css, .component.js";
        rule.enabled = true;
        return rule;
    }
}
