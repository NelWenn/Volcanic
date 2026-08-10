package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotionTest {

    @Test
    void everyCurveStartsAtZeroAndEndsAtOne() {
        assertEquals(0.0f, Motion.ease(0, 200));
        assertEquals(1.0f, Motion.ease(200, 200));
        assertEquals(0.0f, Motion.easeOut(0, 200));
        assertEquals(1.0f, Motion.easeOut(200, 200));
    }

    @Test
    void aCurveNeverLeavesTheUnitRangeEvenOutsideItsWindow() {
        assertEquals(0.0f, Motion.ease(-5000, 200));
        assertEquals(1.0f, Motion.ease(5000, 200));
        assertEquals(0.0f, Motion.easeOut(-5000, 200));
        assertEquals(1.0f, Motion.easeOut(5000, 200));
    }

    @Test
    void aZeroLengthAnimationIsAlreadyOver() {
        assertEquals(1.0f, Motion.ease(0, 0));
        assertEquals(1.0f, Motion.easeOut(0, 0));
    }

    @Test
    void easeOutLeavesFasterThanItArrivesWhichIsWhatMakesEntrancesReadAsQuick() {
        assertTrue(Motion.easeOut(50, 200) > Motion.ease(50, 200),
                "an entrance covers more ground early than a symmetric curve");
        assertTrue(Motion.easeOut(150, 200) > Motion.ease(150, 200));
    }

    @Test
    void everyCurveRisesWithoutEverGoingBackwards() {
        float previousEase = -1.0f;
        float previousOut = -1.0f;
        for (long ms = 0; ms <= 200; ms += 5) {
            float ease = Motion.ease(ms, 200);
            float out = Motion.easeOut(ms, 200);
            assertTrue(ease >= previousEase, "ease dipped at " + ms);
            assertTrue(out >= previousOut, "easeOut dipped at " + ms);
            previousEase = ease;
            previousOut = out;
        }
    }

    @Test
    void aStaggeredRowWaitsItsTurnAndTheWaitStopsGrowingAfterTheCap() {
        assertEquals(0.0f, Motion.rowReveal(0, 4));
        assertTrue(Motion.rowReveal(Motion.STAGGER_MS * 4L + 10, 4) > 0.0f);
        assertTrue(Motion.rowReveal(30, 0) > Motion.rowReveal(30, 3),
                "a later row is always behind an earlier one");
        assertEquals(Motion.rowReveal(120, Motion.STAGGER_CAP),
                Motion.rowReveal(120, Motion.STAGGER_CAP + 40),
                "past the cap every row shares the same delay so long pages still finish together");
    }

    @Test
    void aRowIndexBelowZeroIsAProgrammingErrorNotAZeroDelay() {
        assertThrows(IllegalArgumentException.class, () -> Motion.rowReveal(0, -1));
    }

    @Test
    void aSlideStartsOffsetByItsTravelAndLandsExactlyOnZero() {
        assertEquals(14, Motion.slide(0.0f, 1, 14));
        assertEquals(-14, Motion.slide(0.0f, -1, 14));
        assertEquals(0, Motion.slide(1.0f, 1, 14));
        assertEquals(0, Motion.slide(1.0f, -1, 14));
        assertEquals(0, Motion.slide(0.0f, 0, 14), "no direction means no travel");
    }

    @Test
    void fadingScalesOnlyTheAlphaChannelAndLeavesTheColourAlone() {
        assertEquals(0x80FF8040, Motion.fade(0xFFFF8040, 0.5019608f));
        assertEquals(0x00FF8040, Motion.fade(0xFFFF8040, 0.0f));
        assertEquals(0xFFFF8040, Motion.fade(0xFFFF8040, 1.0f));
        assertEquals(0xFFFF8040, Motion.fade(0xFFFF8040, 5.0f), "an over-bright alpha is clamped");
    }

    @Test
    void blendingEndsExactlyOnEachEndpointSoAColourNeverArrivesOffByOne() {
        assertEquals(0xFF102030, Motion.blend(0xFF102030, 0x80A0B0C0, 0.0f));
        assertEquals(0x80A0B0C0, Motion.blend(0xFF102030, 0x80A0B0C0, 1.0f));
        assertEquals(0xFF000000, Motion.blend(0xFF000000, 0xFFFFFFFF, 0.0f));
        assertEquals(0xFFFFFFFF, Motion.blend(0xFF000000, 0xFFFFFFFF, 1.0f));
    }

    @Test
    void blendingHalfwayLandsHalfwayOnEveryChannelIncludingAlpha() {
        assertEquals(0x80808080, Motion.blend(0x00000000, 0xFFFFFFFF, 0.5019608f));
    }
}
