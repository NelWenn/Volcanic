package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MotionTest {
    @Test
    void startsAtZeroAndEndsAtOne() {
        assertEquals(0.0f, Motion.ease(0, 200), 1.0e-6f);
        assertEquals(1.0f, Motion.ease(200, 200), 1.0e-6f);
    }

    @Test
    void clampsBeyondDuration() {
        assertEquals(1.0f, Motion.ease(10_000, 200), 1.0e-6f);
        assertEquals(0.0f, Motion.ease(-50, 200), 1.0e-6f);
    }

    @Test
    void isMonotonic() {
        float previous = -1.0f;
        for (int elapsed = 0; elapsed <= 200; elapsed += 10) {
            float value = Motion.ease(elapsed, 200);
            assertTrue(value >= previous, "not monotonic at " + elapsed);
            previous = value;
        }
    }

    @Test
    void zeroDurationIsInstant() {
        assertEquals(1.0f, Motion.ease(0, 0), 1.0e-6f);
    }

    @Test
    void durationsMatchTheDesignSystem() {
        assertEquals(200, Motion.HOVER_MS);
        assertEquals(120, Motion.SELECTION_MS);
    }
}
