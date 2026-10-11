package de.sasbe.subtabs;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtabStackRulesTest {
    private static final List<CustomSubtabRule> DEFAULTS = SubtabRulesDefaults.createDefaults();

    private static int indexOf(@NotNull String name) {
        return SubtabRulesDefaults.indexOfRule(DEFAULTS, name);
    }

    @Test
    void defaultRulesIncludeStackPresetsInOrder() {
        assertEquals("Spring Boot", DEFAULTS.get(6).name);
        assertEquals("ASP.NET", DEFAULTS.get(7).name);
        assertEquals("React", DEFAULTS.get(8).name);
        assertEquals("Vue", DEFAULTS.get(9).name);
        assertEquals("Nest", DEFAULTS.get(10).name);
        assertEquals("Playwright", DEFAULTS.get(11).name);
        assertEquals("Cypress", DEFAULTS.get(12).name);
    }

    @Test
    void springBootGroupsLayeredJavaFiles() {
        CustomSubtabRuleMatcher.Match controller = CustomSubtabRuleMatcher.match("OrderController.java", DEFAULTS);
        CustomSubtabRuleMatcher.Match service = CustomSubtabRuleMatcher.match("OrderService.java", DEFAULTS);
        assertNotNull(controller);
        assertNotNull(service);
        assertTrue(
                CustomSubtabRuleMatcher.sameFolderGroupIdentity(
                        controller.groupKey(),
                        service.groupKey()
                ),
                () -> controller.groupKey() + " vs " + service.groupKey()
        );
        assertNotNull(CustomSubtabRuleMatcher.match("OrderRepository.java", DEFAULTS));
        assertNotNull(CustomSubtabRuleMatcher.match("OrderControllerTest.java", DEFAULTS));
        assertNull(CustomSubtabRuleMatcher.matchAtIndex(
                "DemoApplication.java",
                DEFAULTS,
                indexOf("Spring Boot")
        ));
    }

    @Test
    void aspNetGroupsMvcAndConfigFiles() {
        CustomSubtabRuleMatcher.Match controller = CustomSubtabRuleMatcher.match("OrdersController.cs", DEFAULTS);
        CustomSubtabRuleMatcher.Match service = CustomSubtabRuleMatcher.match("OrdersService.cs", DEFAULTS);
        assertNotNull(controller);
        assertNotNull(service);
        assertTrue(
                CustomSubtabRuleMatcher.sameFolderGroupIdentity(
                        controller.groupKey(),
                        service.groupKey()
                ),
                () -> controller.groupKey() + " vs " + service.groupKey()
        );
        assertNotNull(CustomSubtabRuleMatcher.match("OrdersRepository.cs", DEFAULTS));
        assertNotNull(CustomSubtabRuleMatcher.match("OrdersControllerTests.cs", DEFAULTS));
        assertNotNull(CustomSubtabRuleMatcher.match("Index.cshtml", DEFAULTS));
        assertNull(CustomSubtabRuleMatcher.matchAtIndex(
                "Program.cs",
                DEFAULTS,
                indexOf("ASP.NET")
        ));
    }

    @Test
    void reactGroupsComponentAssets() {
        CustomSubtabRuleMatcher.Match component = CustomSubtabRuleMatcher.match("UserCard.tsx", DEFAULTS);
        CustomSubtabRuleMatcher.Match test = CustomSubtabRuleMatcher.match("UserCard.test.tsx", DEFAULTS);
        assertNotNull(component);
        assertNotNull(test);
        assertTrue(CustomSubtabRuleMatcher.sameFolderGroupIdentity(
                component.groupKey(),
                test.groupKey()
        ));
        assertNotNull(CustomSubtabRuleMatcher.match("UserCard.module.css", DEFAULTS));
    }

    @Test
    void vueGroupsSingleFileComponents() {
        CustomSubtabRuleMatcher.Match component = CustomSubtabRuleMatcher.match("ProductCard.vue", DEFAULTS);
        CustomSubtabRuleMatcher.Match test = CustomSubtabRuleMatcher.match("ProductCard.spec.ts", DEFAULTS);
        assertNotNull(component);
        assertNotNull(test);
        assertTrue(CustomSubtabRuleMatcher.sameFolderGroupIdentity(
                component.groupKey(),
                test.groupKey()
        ));
    }

    @Test
    void nestGroupsModuleLayerFiles() {
        assertNotNull(CustomSubtabRuleMatcher.match("users.module.ts", DEFAULTS));
        assertNotNull(CustomSubtabRuleMatcher.match("users.controller.ts", DEFAULTS));
        assertNotNull(CustomSubtabRuleMatcher.match("users.service.ts", DEFAULTS));
        assertNotNull(CustomSubtabRuleMatcher.match("users.controller.spec.ts", DEFAULTS));
    }

    @Test
    void playwrightAndCypressGroupE2eFiles() {
        assertNotNull(CustomSubtabRuleMatcher.match("checkout.e2e-spec.ts", DEFAULTS));
        assertNotNull(CustomSubtabRuleMatcher.match("playwright.config.ts", DEFAULTS));
        assertNotNull(CustomSubtabRuleMatcher.match("login.cy.ts", DEFAULTS));
        assertNotNull(CustomSubtabRuleMatcher.match("cypress.config.ts", DEFAULTS));
    }
}
