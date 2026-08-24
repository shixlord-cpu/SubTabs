package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class SubtabRulesDefaults {
    private SubtabRulesDefaults() {
    }

    public static @NotNull List<CustomSubtabRule> createDefaults() {
        List<CustomSubtabRule> rules = new ArrayList<>(8);
        rules.add(npmRule());
        rules.add(tsconfigRule());
        rules.add(envRule());
        rules.add(stateRule());
        rules.add(modelRule());
        rules.add(componentRule());
        rules.add(customGroupsRule());
        rules.add(folderRule());
        return rules;
    }

    static @NotNull CustomSubtabRule customGroupsRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Eigene Gruppen";
        rule.type = CustomSubtabRule.Type.CUSTOM_GROUPS;
        rule.enabled = true;
        rule.builtin = true;
        return rule;
    }

    static @NotNull CustomSubtabRule folderRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Ordner";
        rule.type = CustomSubtabRule.Type.FOLDER;
        rule.enabled = true;
        rule.builtin = true;
        return rule;
    }

    private static @NotNull CustomSubtabRule npmRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "npm";
        rule.type = CustomSubtabRule.Type.FILES;
        rule.patterns = """
                package.json, package-lock.json, npm-shrinkwrap.json, yarn.lock, pnpm-lock.yaml,\
                 bun.lock, bun.lockb, .npmrc, .nvmrc, .node-version""".replace('\n', ' ').trim();
        rule.labels = "Package, Lock, Lock, Yarn, pnpm, Bun, Bun, npmrc, nvm, nvm";
        rule.slotKeys = "package.json, lock, lock, yarn.lock, pnpm-lock.yaml, bun.lock, bun.lockb, .npmrc, nvm, nvm";
        return rule;
    }

    private static @NotNull CustomSubtabRule tsconfigRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "tsconfig";
        rule.type = CustomSubtabRule.Type.FILES;
        rule.patterns = """
                tsconfig.json, tsconfig.base.json, tsconfig.app.json, tsconfig.spec.json,\
                 tsconfig.lib.json, tsconfig.editor.json, tsconfig.build.json""".replace('\n', ' ').trim();
        rule.labels = "JSON, Base, App, Spec, Lib, Editor, Build";
        return rule;
    }

    private static @NotNull CustomSubtabRule envRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "env";
        rule.type = CustomSubtabRule.Type.FILES;
        rule.patterns = """
                .env, .env.local, .env.example, .env.sample, .env.development, .env.production, .env.test""".replace('\n', ' ').trim();
        rule.labels = "env, local, example, example, dev, prod, test";
        return rule;
    }

    private static @NotNull CustomSubtabRule stateRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "State";
        rule.type = CustomSubtabRule.Type.STEM;
        rule.patterns = """
                .actions.ts, .reducer.ts, .reducers.ts, .effects.ts, .selectors.ts, .state.ts, .store.ts, .facade.ts""".replace('\n', ' ').trim();
        rule.labels = "Actions, Reducer, Reducer, Effects, Selectors, State, Store, Facade";
        rule.slotKeys = """
                .actions.ts, .reducer.ts, .reducer.ts, .effects.ts, .selectors.ts, .state.ts, .store.ts, .facade.ts""".replace('\n', ' ').trim();
        rule.searchNeighbors = true;
        rule.groupSuffix = "state";
        return rule;
    }

    private static @NotNull CustomSubtabRule modelRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Model";
        rule.type = CustomSubtabRule.Type.STEM;
        rule.patterns = """
                .interface.ts, .entity.ts, .mapper.ts, .model.ts, .mock.ts, .dto.ts, .type.ts""".replace('\n', ' ').trim();
        rule.labels = "Interface, Entity, Mapper, Model, Mock, DTO, Type";
        return rule;
    }

    private static @NotNull CustomSubtabRule componentRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Komponente";
        rule.type = CustomSubtabRule.Type.STEM;
        rule.patterns = ".spec.ts, .test.ts, .ts, .html, .scss, .sass, .css, .less";
        rule.labels = "Test, Test, TS, HTML, Style, Style, Style, Style";
        rule.slotKeys = ".spec.ts, .test.ts, .ts, .html, style, style, style, style";
        rule.stripComponentSuffix = true;
        rule.groupSuffix = "components";
        return rule;
    }
}
