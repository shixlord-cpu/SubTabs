package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RulesTableDragSupportTest {
    @Test
    void movesASingleRowByDragHandleDrop() {
        List<CustomSubtabRule> rules = namedRules("a", "b", "c");
        RulesTableDragSupport.moveRowsTo(rules, new int[]{0}, 3);
        assertEquals(List.of("b", "c", "a"), names(rules));
    }

    @Test
    void movesASelectedBlockTogether() {
        List<CustomSubtabRule> rules = namedRules("a", "b", "c", "d");
        RulesTableDragSupport.moveRowsTo(rules, new int[]{0, 1}, 4);
        assertEquals(List.of("c", "d", "a", "b"), names(rules));
    }

    private static List<CustomSubtabRule> namedRules(String... names) {
        List<CustomSubtabRule> rules = new ArrayList<>();
        for (String name : names) {
            CustomSubtabRule rule = new CustomSubtabRule();
            rule.name = name;
            rules.add(rule);
        }
        return rules;
    }

    private static List<String> names(List<CustomSubtabRule> rules) {
        List<String> names = new ArrayList<>();
        for (CustomSubtabRule rule : rules) {
            names.add(rule.name);
        }
        return names;
    }
}
