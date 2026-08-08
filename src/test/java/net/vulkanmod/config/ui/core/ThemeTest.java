package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ThemeTest {
    @Test
    void everyTokenResolves() {
        Theme theme = Theme.volcanic();
        for (ColorToken token : ColorToken.values()) {
            assertNotEquals(0, theme.color(token), "unmapped token " + token);
        }
    }

    @Test
    void colorsAreFullyOpaqueByDefault() {
        Theme theme = Theme.volcanic();
        for (ColorToken token : ColorToken.values()) {
            assertEquals(0xFF, theme.color(token) >>> 24, "token not opaque " + token);
        }
    }

    @Test
    void surfaceBaseMatchesTheDesignSystem() {
        assertEquals(0xFF0E0A09, Theme.volcanic().color(ColorToken.SURFACE_BASE));
    }

    @Test
    void accentMatchesTheDesignSystem() {
        assertEquals(0xFFFF5A1F, Theme.volcanic().color(ColorToken.ACCENT));
    }

    @Test
    void alphaOverrideKeepsRgb() {
        Theme theme = Theme.volcanic();
        int faded = theme.color(ColorToken.ACCENT, 0.5f);
        assertEquals(0x7F, faded >>> 24);
        assertEquals(theme.color(ColorToken.ACCENT) & 0xFFFFFF, faded & 0xFFFFFF);
    }

    @Test
    void alphaIsClamped() {
        Theme theme = Theme.volcanic();
        assertEquals(0xFF, theme.color(ColorToken.ACCENT, 4.0f) >>> 24);
        assertEquals(0x00, theme.color(ColorToken.ACCENT, -1.0f) >>> 24);
    }
}
