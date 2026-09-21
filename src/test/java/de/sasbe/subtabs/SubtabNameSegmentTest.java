package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubtabNameSegmentTest {
    @Test
    void resolvesPositiveSegmentsFromStart() {
        assertEquals("cart", SubtabNameSegment.resolve("cart.actions.ts", 1));
        assertEquals("actions", SubtabNameSegment.resolve("cart.actions.ts", 2));
        assertEquals("ts", SubtabNameSegment.resolve("cart.actions.ts", 3));
    }

    @Test
    void resolvesNegativeSegmentsFromEnd() {
        assertEquals("ts", SubtabNameSegment.resolve("cart.actions.ts", -1));
        assertEquals("actions", SubtabNameSegment.resolve("cart.actions.ts", -2));
        assertEquals("cart", SubtabNameSegment.resolve("cart.actions.ts", -3));
    }

    @Test
    void zeroProducesQuestionMark() {
        assertEquals("?", SubtabNameSegment.resolve("cart.actions.ts", 0));
    }

    @Test
    void positiveOutOfRangeUsesLastSegment() {
        assertEquals("ts", SubtabNameSegment.resolve("cart.actions.ts", 5));
    }

    @Test
    void negativeOutOfRangeUsesFirstSegment() {
        assertEquals("cart", SubtabNameSegment.resolve("cart.actions.ts", -5));
    }

    @Test
    void resolvesDotEnvSegments() {
        assertEquals("", SubtabNameSegment.resolve(".env", 1));
        assertEquals("env", SubtabNameSegment.resolve(".env", 2));
        assertEquals("local", SubtabNameSegment.resolve(".env.local", 3));
    }

    @Test
    void resolvesComponentStyleSegment() {
        assertEquals("scss", SubtabNameSegment.resolve("user-card.component.scss", -1));
        assertEquals("component", SubtabNameSegment.resolve("user-card.component.ts", 2));
        assertEquals("spec", SubtabNameSegment.resolve("user-card.component.spec.ts", -2));
    }
}
