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
                        "cart.selectors.ts"
                )
        );

        List<SubtabCandidateResolver.Located> located = SubtabCandidateResolver.resolve(
                STATE_GROUP,
                "central",
                "cart.actions.ts",
                files
        );

        assertEquals(4, located.size());
        assertEquals("central", SubtabCandidateResolver.commonDirectory(located, "central"));
        assertEquals(".actions.ts", located.get(0).slotId());
        assertEquals(".reducer.ts", located.get(1).slotId());
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
    void keepsModelsInTheSameFolderOnly() {
        Map<String, Set<String>> files = Map.of(
                "models/user", Set.of("user.model.ts", "user.dto.ts"),
                "models/order", Set.of("order.model.ts", "user.interface.ts")
        );

        List<SubtabCandidateResolver.Located> located = SubtabCandidateResolver.resolve(
                "rule:4:user",
                "models/user",
                "user.model.ts",
                files
        );

        assertEquals(2, located.size());
        assertEquals("models/user", located.get(0).directory());
        assertEquals("models/user", located.get(1).directory());
    }
}
