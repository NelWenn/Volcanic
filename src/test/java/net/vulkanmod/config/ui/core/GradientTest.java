package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GradientTest {
    @Test
    void keepsBothStops() {
        Gradient gradient = new Gradient(0xFF0E0A09, 0xFF150E0C);
        assertEquals(0xFF0E0A09, gradient.topArgb());
        assertEquals(0xFF150E0C, gradient.bottomArgb());
    }

    @Test
    void equalStopsMeanFlat() {
        assertTrue(new Gradient(0xFF112233, 0xFF112233).isFlat());
        assertFalse(new Gradient(0xFF112233, 0xFF112234).isFlat());
    }
}
