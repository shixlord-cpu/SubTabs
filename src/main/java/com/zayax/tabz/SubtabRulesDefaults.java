package com.zayax.tabz;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class SubtabRulesDefaults {
    static final String SEARCH_FOLDER_LABEL = "Folder";
    static final String SEARCH_NEIGHBORS_LABEL = "Neighbors";

    private SubtabRulesDefaults() {
    }

    static boolean isNeighborsSearchLabel(@NotNull String value) {
        return SEARCH_NEIGHBORS_LABEL.equals(value) || "Nachbarn".equals(value);
    }

    static @NotNull String searchScopeLabel(boolean searchNeighbors) {
        return searchNeighbors ? SEARCH_NEIGHBORS_LABEL : SEARCH_FOLDER_LABEL;
    }

    public static @NotNull List<CustomSubtabRule> createDefaults() {
        List<CustomSubtabRule> rules = new ArrayList<>(17);
        rules.add(npmRule());
        rules.add(tsconfigRule());
        rules.add(envRule());
        rules.add(stateRule());
        rules.add(stateFeatureRule());
        rules.add(modelRule());
        rules.add(springBootRule());
        rules.add(aspNetRule());
        rules.add(reactRule());
        rules.add(vueRule());
        rules.add(nestRule());
        rules.add(playwrightRule());
        rules.add(cypressRule());
        rules.add(htmlRule());
        rules.add(componentRule());
        rules.add(userGroupsRule());
        rules.add(folderRule());
        return rules;
    }

    static int indexOfRule(@NotNull List<CustomSubtabRule> rules, @NotNull String ruleName) {
        for (int index = 0; index < rules.size(); index++) {
            if (ruleName.equals(rules.get(index).name)) {
                return index;
            }
        }
        return -1;
    }

    static @NotNull CustomSubtabRule userGroupsRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Custom groups";
        rule.type = CustomSubtabRule.Type.USER_GROUPS;
        rule.nameSegments = "1";
        rule.enabled = true;
        rule.builtin = true;
        return rule;
    }

    static @NotNull CustomSubtabRule folderRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Folder";
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
        rule.searchNeighbors = true;
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

    private static @NotNull CustomSubtabRule springBootRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Spring Boot";
        rule.patterns = """
                Controller.java, Service.java, Repository.java, Test.java, Tests.java, IntegrationTest.java,\
                 Impl.java, .java, .properties, .yml, .yaml""".replace('\n', ' ').trim();
        rule.nameSegments = "-2, -2, -2, -2, -2, -2, -2, 2, 1, 1, 1";
        rule.groupNameSegments = "1";
        rule.slotKeys = """
                controller, service, repository, test, test, integration, impl, java, config, config, config""".replace('\n', ' ').trim();
        rule.excludePatterns = "Application.java, ApplicationTests.java";
        rule.groupSuffix = "spring";
        return rule;
    }

    private static @NotNull CustomSubtabRule aspNetRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "ASP.NET";
        rule.patterns = """
                Controller.cs, Service.cs, Repository.cs, Tests.cs, Test.cs, .cshtml, .csproj,\
                 appsettings.json, appsettings.Development.json, appsettings.Production.json""".replace('\n', ' ').trim();
        rule.nameSegments = "-2, -2, -2, -2, -2, -1, 1, 1, 1, 1";
        rule.groupNameSegments = "1";
        rule.slotKeys = """
                controller, service, repository, test, test, view, project, config, config, config""".replace('\n', ' ').trim();
        rule.excludePatterns = "Program.cs, Startup.cs, Global.asax.cs";
        rule.groupSuffix = "aspnet";
        return rule;
    }

    private static @NotNull CustomSubtabRule reactRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "React";
        rule.patterns = """
                .stories.tsx, .stories.jsx, .test.tsx, .spec.tsx, .test.jsx, .spec.jsx, .tsx, .jsx,\
                 .module.css, .module.scss, .module.sass, .module.less""".replace('\n', ' ').trim();
        rule.nameSegments = "-2, -2, -2, -2, -2, -2, 2, 2, -1, -1, -1, -1";
        rule.groupNameSegments = "1";
        rule.slotKeys = """
                stories, stories, test, test, test, test, tsx, jsx, style, style, style, style""".replace('\n', ' ').trim();
        rule.groupSuffix = "react";
        return rule;
    }

    private static @NotNull CustomSubtabRule vueRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Vue";
        rule.patterns = ".vue, .spec.ts, .test.ts, .spec.js, .test.js";
        rule.nameSegments = "2, -2, -2, -2, -2";
        rule.groupNameSegments = "1";
        rule.slotKeys = ".vue, test, test, test, test";
        rule.excludePatterns = ".component.spec.ts, .component.test.ts, .component.spec.js, .component.test.js";
        rule.groupSuffix = "vue";
        return rule;
    }

    private static @NotNull CustomSubtabRule nestRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Nest";
        rule.patterns = """
                .module.ts, .controller.ts, .service.ts, .gateway.ts, .resolver.ts, .controller.spec.ts,\
                 .service.spec.ts, .dto.ts, .entity.ts, .interface.ts""".replace('\n', ' ').trim();
        rule.nameSegments = "1, 1, 1, 1, 1, 1, 1, 1, 1, 1";
        rule.groupNameSegments = "1";
        rule.slotKeys = """
                module, controller, service, gateway, resolver, test, test, dto, entity, interface""".replace('\n', ' ').trim();
        rule.groupSuffix = "nest";
        return rule;
    }

    private static @NotNull CustomSubtabRule playwrightRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Playwright";
        rule.patterns = ".e2e-spec.ts, .e2e.spec.ts, .e2e.ts, playwright.config.ts, playwright.config.js";
        rule.nameSegments = "1, 1, 1, 1, 1";
        rule.groupNameSegments = "1";
        rule.slotKeys = "e2e, e2e, e2e, config, config";
        rule.groupSuffix = "e2e";
        return rule;
    }

    private static @NotNull CustomSubtabRule cypressRule() {
        CustomSubtabRule rule = new CustomSubtabRule();
        rule.name = "Cypress";
        rule.patterns = ".cy.ts, .cy.js, cypress.config.ts, cypress.config.js";
        rule.nameSegments = "1, 1, 1, 1";
        rule.groupNameSegments = "1";
        rule.slotKeys = "spec, spec, config, config";
        rule.groupSuffix = "cypress";
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
