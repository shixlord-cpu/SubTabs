package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubtabCandidateResolverTest {
    private static final String STATE_GROUP = "rule:3:cart";

    @Test
    void groupsStateFilesInTheSameFolder() {
        Map<String, Set<String>> files = Map.of(
                "central", Set.of(
                        "cart.actions.ts",
                        "cart.reducer.ts",
                        "cart.effects.ts",
                        "cart.selectors.ts",
                        "cart.state.ts",
                        "user.actions.ts",
                        "user.reducer.ts",
                        "user.effects.ts",
                        "user.selectors.ts",
                        "user.state.ts",
                        "catalog.actions.ts",
                        "catalog.reducer.ts",
                        "catalog.effects.ts",
                        "catalog.selectors.ts",
                        "catalog.state.ts",
                        "catalog.facade.ts"
                )
        );

        List<SubtabCandidateResolver.Located> located = SubtabCandidateResolver.resolve(
                STATE_GROUP,
                "central",
                "cart.actions.ts",
                files
        );

        assertEquals(5, located.size());
        assertEquals("central", SubtabCandidateResolver.commonDirectory(located, "central"));
        assertEquals(".actions.ts", located.get(0).slotId());
        assertEquals(".reducer.ts", located.get(1).slotId());

        List<SubtabCandidateResolver.Located> userLocated = SubtabCandidateResolver.resolve(
                "rule:3:user",
                "central",
                "user.actions.ts",
                files
        );
        assertEquals(5, userLocated.size());
        assertEquals("user.actions.ts", userLocated.get(0).fileName());
        assertEquals("user.state.ts", userLocated.get(4).fileName());

        List<SubtabCandidateResolver.Located> catalogLocated = SubtabCandidateResolver.resolve(
                "rule:3:catalog",
                "central",
                "catalog.facade.ts",
                files
        );
        assertEquals(6, catalogLocated.size());
        assertEquals("catalog.facade.ts", catalogLocated.get(5).fileName());
    }

    @Test
    void groupsStateFilesInNeighboringFolders() {
        Map<String, Set<String>> files = new LinkedHashMap<>();
        files.put("feature-based/products", Set.of(
                "products.component.ts",
                "products.actions.ts",
                "products.selectors.ts"
        ));
        files.put("feature-based/products-state", Set.of(
                "products.reducer.ts",
                "products.effects.ts",
                "products.state.ts"
        ));
        files.put("feature-based", Set.of());

        List<SubtabCandidateResolver.Located> located = SubtabCandidateResolver.resolve(
                "rule:3:products",
                "feature-based/products",
                "products.actions.ts",
                files
        );

        assertEquals(5, located.size());
        assertEquals("feature-based", SubtabCandidateResolver.commonDirectory(located, "feature-based/products"));
        assertEquals("products.actions.ts", located.get(0).fileName());
        assertEquals("feature-based/products", located.get(0).directory());
        assertEquals("products.reducer.ts", located.get(1).fileName());
        assertEquals("feature-based/products-state", located.get(1).directory());
    }

    @Test
    void groupsCheckoutStateAcrossFeatureFolders() {
        Map<String, Set<String>> files = new LinkedHashMap<>();
        files.put("feature-based/checkout", Set.of(
                "checkout.component.ts",
                "checkout.component.html",
                "checkout.component.scss",
                "checkout.component.spec.ts",
                "checkout.actions.ts",
                "checkout.selectors.ts"
        ));
        files.put("feature-based/checkout-state", Set.of(
                "checkout.reducer.ts",
                "checkout.effects.ts",
                "checkout.state.ts"
        ));
        files.put("feature-based", Set.of());

        List<SubtabCandidateResolver.Located> located = SubtabCandidateResolver.resolve(
                "rule:3:checkout",
                "feature-based/checkout",
                "checkout.actions.ts",
                files
        );

        assertEquals(5, located.size());
        assertEquals("feature-based", SubtabCandidateResolver.commonDirectory(located, "feature-based/checkout"));
        assertEquals("checkout.actions.ts", located.get(0).fileName());
        assertEquals("feature-based/checkout", located.get(0).directory());
        assertEquals("checkout.reducer.ts", located.get(1).fileName());
        assertEquals("feature-based/checkout-state", located.get(1).directory());
        assertEquals("checkout.state.ts", located.get(4).fileName());
    }

    @Test
    void groupsOrdersStateAcrossFeatureFolders() {
        Map<String, Set<String>> files = new LinkedHashMap<>();
        files.put("feature-based/orders", Set.of(
                "orders.component.ts",
                "orders.actions.ts",
                "orders.selectors.ts"
        ));
        files.put("feature-based/orders-state", Set.of(
                "orders.reducer.ts",
                "orders.effects.ts",
                "orders.state.ts"
        ));
        files.put("feature-based", Set.of());

        List<SubtabCandidateResolver.Located> located = SubtabCandidateResolver.resolve(
                "rule:3:orders",
                "feature-based/orders",
                "orders.actions.ts",
                files
        );

        assertEquals(5, located.size());
        assertEquals("orders.effects.ts", located.get(2).fileName());
        assertEquals("feature-based/orders-state", located.get(2).directory());
    }

    @Test
    void keepsModelsInTheSameFolderOnly() {
        Map<String, Set<String>> files = Map.of(
                "models/user", Set.of("user.model.ts", "user.dto.ts"),
                "models/order", Set.of("order.model.ts", "user.interface.ts")
        );

        List<SubtabCandidateResolver.Located> located = SubtabCandidateResolver.resolve(
                "rule:5:user",
                "models/user",
                "user.model.ts",
                files
        );

        assertEquals(2, located.size());
        assertEquals("models/user", located.get(0).directory());
        assertEquals("models/user", located.get(1).directory());
    }
}
