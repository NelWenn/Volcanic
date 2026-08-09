package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HoverStateTest {
    private static final String ROW = "vulkanmod:graphics.fog";
    private static final String OTHER = "vulkanmod:graphics.mipmap";

    @Test
    void startsAtZeroAndRisesGradually() {
        HoverState state = new HoverState(200);
        assertEquals(0.0f, state.advance(ROW, true, 0), 1.0e-6f);

        float partial = state.advance(ROW, true, 50);
        assertTrue(partial > 0.0f && partial < 1.0f, "expected a partial fade, got " + partial);
    }

    @Test
    void reachesOneAndStops() {
        HoverState state = new HoverState(200);
        state.advance(ROW, true, 150);

        assertEquals(1.0f, state.advance(ROW, true, 150), 1.0e-6f);
        assertEquals(1.0f, state.advance(ROW, true, 10_000), 1.0e-6f);
        assertTrue(state.advance(ROW, false, 100) < 1.0f, "a saturated fade never comes back down");
    }

    @Test
    void reachesZeroAndStops() {
        HoverState state = new HoverState(200);
        state.advance(ROW, true, 200);

        assertEquals(0.0f, state.advance(ROW, false, 200), 1.0e-6f);
        assertEquals(0.0f, state.advance(ROW, false, 10_000), 1.0e-6f);
        assertTrue(state.advance(ROW, true, 100) > 0.0f, "a drained fade never comes back up");
    }

    @Test
    void reversingMidFadeDoesNotJump() {
        HoverState state = new HoverState(200);
        float half = state.advance(ROW, true, 100);
        float further = state.advance(ROW, true, 50);

        assertTrue(further > half, "the fade should still be rising");
        assertEquals(half, state.advance(ROW, false, 50), 1.0e-6f);
    }

    @Test
    void forgetsAKeyItNeverSeesAgain() {
        HoverState state = new HoverState(200);
        state.advance(ROW, true, 200);
        state.endFrame();

        state.advance(OTHER, true, 200);
        state.endFrame();

        assertEquals(0.0f, state.advance(ROW, true, 0), 1.0e-6f);
    }

    @Test
    void keepsAKeyItStillSees() {
        HoverState state = new HoverState(200);
        state.advance(ROW, true, 200);
        state.endFrame();

        assertEquals(1.0f, state.advance(ROW, true, 0), 1.0e-6f);
        state.endFrame();
        assertEquals(1.0f, state.advance(ROW, true, 0), 1.0e-6f);
    }

    @Test
    void keysFadeIndependently() {
        HoverState state = new HoverState(200);
        state.advance(ROW, true, 200);

        assertEquals(0.0f, state.advance(OTHER, false, 200), 1.0e-6f);
        assertEquals(1.0f, state.advance(ROW, true, 0), 1.0e-6f);
    }

    @Test
    void aShorterDurationFadesFaster() {
        HoverState slow = new HoverState(200);
        HoverState quick = new HoverState(100);

        assertTrue(quick.advance(ROW, true, 50) > slow.advance(ROW, true, 50));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> new HoverState(0));
        assertThrows(IllegalArgumentException.class, () -> new HoverState(-1));

        HoverState state = new HoverState(200);
        assertThrows(IllegalArgumentException.class, () -> state.advance(null, true, 10));
        assertThrows(IllegalArgumentException.class, () -> state.advance("  ", true, 10));
        assertThrows(IllegalArgumentException.class, () -> state.advance(ROW, true, -1));
    }
}
