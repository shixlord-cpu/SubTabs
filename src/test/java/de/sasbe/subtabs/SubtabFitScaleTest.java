package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtabFitScaleTest {
    @Test
    void keepsFullSizeWhenEverythingFits() {
        SubtabFitScale.Result result = SubtabFitScale.compute(400, 300, true);
        assertEquals(1f, result.fontScale(), 0.001f);
        assertEquals(1f, result.paddingScale(), 0.001f);
    }

    @Test
    void ignoresFitWhenDisabled() {
        SubtabFitScale.Result result = SubtabFitScale.compute(100, 400, false);
        assertTrue(result.isFull());
    }

    @Test
    void shrinksPaddingBeforeFont() {
        SubtabFitScale.Result result = SubtabFitScale.compute(360, 400, true);
        assertEquals(1f, result.fontScale(), 0.001f);
        assertTrue(result.paddingScale() < 1f);
        assertTrue(result.paddingScale() >= SubtabFitScale.MIN_PADDING_SCALE);
    }

    @Test
    void shrinksFontOnlyAfterPaddingIsExhausted() {
        SubtabFitScale.Result result = SubtabFitScale.compute(280, 400, true);
        assertTrue(result.fontScale() < 1f);
        assertEquals(SubtabFitScale.MIN_PADDING_SCALE, result.paddingScale(), 0.001f);
        assertTrue(result.fontScale() >= SubtabFitScale.MIN_FONT_SCALE);
    }

    @Test
    void usesMinimumsWhenWindowIsExtremelyNarrow() {
        SubtabFitScale.Result result = SubtabFitScale.compute(50, 400, true);
        assertEquals(SubtabFitScale.MIN_FONT_SCALE, result.fontScale(), 0.001f);
        assertEquals(SubtabFitScale.MIN_PADDING_SCALE, result.paddingScale(), 0.001f);
    }
}
