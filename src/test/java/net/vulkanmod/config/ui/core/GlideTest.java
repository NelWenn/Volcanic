package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlideTest {

    @Test
    void theFirstFrameLandsOnTheTargetSoAScreenNeverOpensMidFlight() {
        Glide glide = new Glide(60.0f);
        assertEquals(400.0f, glide.advance(400.0f, 16));
        assertTrue(glide.settled(400.0f));
    }

    @Test
    void itHalvesTheRemainingDistanceOverOneHalfLife() {
        Glide glide = new Glide(100.0f);
        glide.advance(0.0f, 16);
        assertEquals(50.0f, glide.advance(100.0f, 100), 0.5f);
    }

    @Test
    void theSameElapsedTimeLandsInTheSamePlaceWhateverTheFrameRate() {
        Glide slow = new Glide(80.0f);
        Glide fast = new Glide(80.0f);
        slow.advance(0.0f, 16);
        fast.advance(0.0f, 16);

        for (int frame = 0; frame < 6; frame++) {
            slow.advance(500.0f, 40);
        }
        for (int frame = 0; frame < 48; frame++) {
            fast.advance(500.0f, 5);
        }
        assertEquals(slow.value(), fast.value(), 1.0f,
                "240 fps and 25 fps must reach the same place after 240 ms");
    }

    @Test
    void itSettlesExactlyOnTheTargetInsteadOfCreepingForever() {
        Glide glide = new Glide(50.0f);
        glide.advance(0.0f, 16);
        for (int frame = 0; frame < 200; frame++) {
            glide.advance(120.0f, 16);
        }
        assertEquals(120.0f, glide.value());
        assertTrue(glide.settled(120.0f));
    }

    @Test
    void aTargetThatMovesMidFlightIsFollowedFromWhereverItAlreadyIs() {
        Glide glide = new Glide(60.0f);
        glide.advance(0.0f, 16);
        glide.advance(300.0f, 60);
        float halfway = glide.value();
        assertTrue(halfway > 0.0f && halfway < 300.0f);

        glide.advance(0.0f, 60);
        assertTrue(glide.value() < halfway, "reversing eases back from the current value, it never jumps");
        assertTrue(glide.value() > 0.0f, "and it does not snap to the new target either");
    }

    @Test
    void aFrameThatTookNoTimeChangesNothing() {
        Glide glide = new Glide(60.0f);
        glide.advance(0.0f, 16);
        glide.advance(500.0f, 30);
        float before = glide.value();
        assertEquals(before, glide.advance(500.0f, 0));
    }

    @Test
    void jumpingSkipsTheAnimationOutrightForALayoutChangeThatMustNotBeSeenSliding() {
        Glide glide = new Glide(60.0f);
        glide.advance(0.0f, 16);
        glide.jumpTo(900.0f);
        assertEquals(900.0f, glide.value());
        assertTrue(glide.settled(900.0f));
    }

    @Test
    void theRenderedValueIsAWholePixelBecauseASubPixelEdgeShimmers() {
        Glide glide = new Glide(60.0f);
        glide.advance(0.0f, 16);
        glide.advance(10.0f, 30);
        assertEquals(Math.round(glide.value()), glide.rendered());
    }

    @Test
    void aNonPositiveHalfLifeOrANegativeFrameIsAProgrammingError() {
        assertThrows(IllegalArgumentException.class, () -> new Glide(0.0f));
        assertThrows(IllegalArgumentException.class, () -> new Glide(-5.0f));
        assertThrows(IllegalArgumentException.class, () -> new Glide(60.0f).advance(0.0f, -1));
    }
}
