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
    void connectedMethodAndItsAliasesParseToCtm() {
        assertEquals(CtmMethod.CTM, CtmMethod.fromString("ctm"));
        assertEquals(CtmMethod.CTM, CtmMethod.fromString("glass"));
        assertEquals(CtmMethod.CTM, CtmMethod.fromString("full"));
    }

    @Test
    void unknownAndAbsentMethodsAreUnsupported() {
        assertEquals(CtmMethod.UNSUPPORTED, CtmMethod.fromString("horizontal"));
        assertEquals(CtmMethod.UNSUPPORTED, CtmMethod.fromString("vertical"));
        assertEquals(CtmMethod.UNSUPPORTED, CtmMethod.fromString(null));
    }

    @Test
    void parsingIgnoresCaseAndSurroundingSpace() {
        assertEquals(CtmMethod.REPEAT, CtmMethod.fromString("  Repeat "));
        assertEquals(CtmMethod.OVERLAY_FIXED, CtmMethod.fromString("OVERLAY_FIXED"));
    }

    @Test
    void overlayFlag() {
        assertTrue(CtmMethod.OVERLAY_FIXED.isOverlay());
        assertFalse(CtmMethod.RANDOM.isOverlay());
    }
}
