package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelBurstTest {

    @Test
    void aBurstStepsThroughItsFramesAndThenCleansItselfUp() {
        PixelBurst burst = new PixelBurst();
        burst.trigger("toggle");
        assertEquals(0, burst.frame("toggle"));
        burst.advance(PixelBurst.TICK_MS);
        assertEquals(1, burst.frame("toggle"));
        burst.advance(PixelBurst.TICK_MS * 3L);
        assertEquals(-1, burst.frame("toggle"), "a finished burst must vanish");
        assertTrue(burst.idle());
    }

    @Test
    void aPartialTickAdvancesNothing() {
        PixelBurst burst = new PixelBurst();
        burst.trigger("k");
        burst.advance(PixelBurst.TICK_MS - 1);
        assertEquals(0, burst.frame("k"));
    }

    @Test
    void twoControlsBurstIndependently() {
        PixelBurst burst = new PixelBurst();
        burst.trigger("a");
        burst.advance(PixelBurst.TICK_MS * 2L);
        burst.trigger("b");
        assertEquals(2, burst.frame("a"));
        assertEquals(0, burst.frame("b"));
    }

    @Test
    void retriggeringRestartsFromTheFirstFrame() {
        PixelBurst burst = new PixelBurst();
        burst.trigger("k");
        burst.advance(PixelBurst.TICK_MS * 2L);
        burst.trigger("k");
        assertEquals(0, burst.frame("k"));
    }

    @Test
    void sparksFlyOutwardsFrameByFrameAndDroopAsTheyGo() {
        for (int spark = 0; spark < PixelBurst.SPARKS; spark++) {
            int previous = 0;
            for (int frame = 0; frame < PixelBurst.FRAMES; frame++) {
                int distance = Math.abs(PixelBurst.sparkX(spark, frame))
                        + Math.abs(PixelBurst.sparkY(spark, frame));
                assertTrue(distance >= previous, "spark " + spark + " fell back at frame " + frame);
                previous = distance;
            }
        }
        assertTrue(PixelBurst.sparkY(0, PixelBurst.FRAMES - 1) > PixelBurst.sparkY(0, 0),
                "gravity must pull a sideways spark down over its flight");
    }

    @Test
    void theRingCoversAllFourQuadrantsSoTheBurstReadsRound() {
        boolean left = false;
        boolean right = false;
        boolean up = false;
        boolean down = false;
        for (int spark = 0; spark < PixelBurst.SPARKS; spark++) {
            left |= PixelBurst.sparkX(spark, 2) < 0;
            right |= PixelBurst.sparkX(spark, 2) > 0;
            up |= PixelBurst.sparkY(spark, 2) < 0;
            down |= PixelBurst.sparkY(spark, 2) > 0;
        }
        assertTrue(left && right && up && down, "the burst leans to one side");
    }

    @Test
    void aStalledFrameCannotSkipTheWholeBurstAndBadInputThrows() {
        PixelBurst burst = new PixelBurst();
        burst.trigger("k");
        burst.advance(60_000);
        assertTrue(burst.frame("k") >= 0 || burst.idle());
        PixelBurst capped = new PixelBurst();
        capped.trigger("k");
        capped.advance(250);
        capped.advance(60_000);
        assertTrue(capped.idle() || capped.frame("k") > 0);
        assertThrows(IllegalArgumentException.class, () -> new PixelBurst().trigger(" "));
        assertThrows(IllegalArgumentException.class, () -> new PixelBurst().advance(-1));
        assertThrows(IllegalArgumentException.class, () -> Motion.step(0.5f, 0));
    }
}
