package de.sasbe.subtabs;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtabGroupColorsTest {
    @Test
    void paletteProvidesManyDistinctBaseColors() {
        Map<Integer, Color> colors = new LinkedHashMap<>();
        for (int index = 0; index < 18; index++) {
            colors.put(index, paletteColor(index));
        }
        assertEquals(18, colors.size());
        long unique = colors.values().stream().distinct().count();
        assertEquals(18, unique);
    }

    @Test
    void wrapsPaletteIndexWhenExhausted() {
        Color first = paletteColor(0);
        Color wrapped = paletteColor(18);
        assertEquals(first.getRed(), wrapped.getRed());
        assertEquals(first.getGreen(), wrapped.getGreen());
        assertEquals(first.getBlue(), wrapped.getBlue());
    }

    private static Color paletteColor(int index) {
        int[] palette = {
                0xE11D48, 0x2563EB, 0x059669, 0xD97706, 0x7C3AED, 0x0891B2,
                0xDB2777, 0x4F46E5, 0x16A34A, 0xEA580C, 0x9333EA, 0x0D9488,
                0xBE123C, 0x1D4ED8, 0x047857, 0xB45309, 0x6D28D9, 0x0E7490
        };
        int paletteIndex = Math.floorMod(index, palette.length);
        return new Color(palette[paletteIndex]);
    }
}
