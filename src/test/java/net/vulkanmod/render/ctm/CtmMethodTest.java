package net.vulkanmod.render.ctm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CtmMethodTest {
    @Test
    void parsesKnownMethods() {
        assertEquals(CtmMethod.RANDOM, CtmMethod.fromString("random"));
        assertEquals(CtmMethod.OVERLAY_RANDOM, CtmMethod.fromString("overlay_random"));
        assertEquals(CtmMethod.FIXED, CtmMethod.fromString("fixed"));
    }

    @Test
    void connectedMethodsAreUnsupportedInPhase1() {
        assertEquals(CtmMethod.UNSUPPORTED, CtmMethod.fromString("ctm"));
        assertEquals(CtmMethod.UNSUPPORTED, CtmMethod.fromString("horizontal"));
    }

    @Test
    void overlayFlag() {
        assertTrue(CtmMethod.OVERLAY_FIXED.isOverlay());
        assertFalse(CtmMethod.RANDOM.isOverlay());
    }
}
