package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetFxTest {
    private static final Rect CARD = new Rect(100, 60, 120, 140);

    private static PresetFx running(int effect, int frames) {
        PresetFx fx = new PresetFx();
        fx.trigger(0, effect);
        for (int frame = 0; frame < frames; frame++) {
            fx.advance(PresetFx.TICK_MS);
        }
        return fx;
    }

    @Test
    void nothingMovesUntilAWholeFrameHasPassedBecauseTheClockIsStepped() {
        PresetFx fx = new PresetFx();
        fx.trigger(0, PresetFx.SKIP);
        fx.advance(PresetFx.TICK_MS - 1);
        assertEquals(0, fx.frame(0), "a partial step must not advance the animation");
        fx.advance(1);
        assertEquals(1, fx.frame(0));
    }

    @Test
    void manySmallFramesAndOneBigFrameLandOnTheSameStep() {
        PresetFx many = new PresetFx();
        PresetFx once = new PresetFx();
        many.trigger(0, PresetFx.HAZE);
        once.trigger(0, PresetFx.HAZE);
        for (int frame = 0; frame < 20; frame++) {
            many.advance(16);
        }
        for (int frame = 0; frame < 4; frame++) {
            once.advance(80);
        }
        assertEquals(once.frame(0), many.frame(0), "320 ms is 320 ms however it is chopped up");
    }

    @Test
    void aStalledFrameCannotSkipTheWholeAnimation() {
        PresetFx fx = new PresetFx();
        fx.trigger(0, PresetFx.ERUPT);
        fx.advance(60_000);
        assertTrue(fx.playing(0), "one huge delta swallowed the entire effect");
    }

    @Test
    void anEffectEndsOnItsOwnAndReleasesTheCard() {
        PresetFx fx = running(PresetFx.SKIP, PresetFx.TICK_MS);
        assertFalse(fx.playing(0));
        assertEquals(PresetFx.NONE, fx.effect(0));
    }

    @Test
    void onlyTheCardThatWasPressedPlaysAnything() {
        PresetFx fx = new PresetFx();
        fx.trigger(2, PresetFx.ERUPT);
        fx.advance(PresetFx.TICK_MS);
        assertTrue(fx.playing(2));
        assertFalse(fx.playing(0));
        assertFalse(fx.playing(3));
        assertEquals(0, fx.frame(1));
    }

    @Test
    void pressingASecondCardTakesTheEffectWithIt() {
        PresetFx fx = new PresetFx();
        fx.trigger(0, PresetFx.ERUPT);
        fx.advance(PresetFx.TICK_MS * 3L);
        fx.trigger(1, PresetFx.SKIP);
        assertFalse(fx.playing(0));
        assertTrue(fx.playing(1));
        assertEquals(0, fx.frame(1), "the new effect starts from its first frame");
    }

    @Test
    void eachPresetGetsItsOwnEffectAndAnUnknownOneFallsBackToTheScanner() {
        assertEquals(PresetFx.SKIP, PresetFx.effectFor("vulkanmod.options.performancePreset.performance"));
        assertEquals(PresetFx.ROCK, PresetFx.effectFor("vulkanmod.options.performancePreset.balanced"));
        assertEquals(PresetFx.HAZE, PresetFx.effectFor("vulkanmod.options.performancePreset.quality"));
        assertEquals(PresetFx.ERUPT, PresetFx.effectFor("vulkanmod.options.performancePreset.ultra"));
        assertEquals(PresetFx.SCAN, PresetFx.effectFor("vulkanmod.options.performancePreset.custom"));
        assertEquals(PresetFx.SCAN, PresetFx.effectFor(null));
    }

    @Test
    void theTiltSnapsToStepsSoTheCardMovesLikeASpriteNotLikeAPointer() {
        assertEquals(0, PresetFx.tiltStep(CARD, CARD.x() + CARD.width() / 2));
        assertEquals(-PresetFx.TILT_STEPS, PresetFx.tiltStep(CARD, CARD.x()));
        assertEquals(PresetFx.TILT_STEPS, PresetFx.tiltStep(CARD, CARD.right()));

        int changes = 0;
        int previous = PresetFx.tiltStep(CARD, CARD.x());
        for (int x = CARD.x(); x <= CARD.right(); x++) {
            int step = PresetFx.tiltStep(CARD, x);
            if (step != previous) {
                changes++;
            }
            previous = step;
        }
        assertEquals(PresetFx.TILT_STEPS * 2, changes,
                "the tilt must cross exactly one boundary per step, never slide");
    }

    @Test
    void theTiltNeverLeansFurtherThanItsLimitEvenOffTheCard() {
        assertEquals(-PresetFx.TILT_STEPS, PresetFx.tiltStep(CARD, CARD.x() - 900));
        assertEquals(PresetFx.TILT_STEPS, PresetFx.tiltStep(CARD, CARD.right() + 900));
        assertEquals(0, PresetFx.tiltStep(new Rect(0, 0, 0, 10), 5), "a card with no width cannot lean");
        assertEquals(PresetFx.TILT_DEGREES, PresetFx.tiltDegrees(PresetFx.TILT_STEPS), 0.001f);
        assertEquals(0.0f, PresetFx.tiltDegrees(0));
    }

    @Test
    void theBalanceRocksBothWaysWithShrinkingSwingsAndComesToRestLevel() {
        int previousSwing = Integer.MAX_VALUE;
        int direction = 0;
        int reversals = 0;
        for (int frame = 0; frame < 10; frame++) {
            int angle = running(PresetFx.ROCK, frame).rockAngle(0);
            assertTrue(Math.abs(angle) <= 4, "frame " + frame + " leaned " + angle + " degrees");
            if (angle != 0) {
                assertTrue(Math.abs(angle) <= previousSwing, "the swing grew back at frame " + frame);
                previousSwing = Math.abs(angle);
                if (direction != 0 && Integer.signum(angle) != direction) {
                    reversals++;
                }
                direction = Integer.signum(angle);
            }
        }
        assertTrue(reversals >= 2, "a balance must tip back and forth, got " + reversals + " reversals");
        assertEquals(0, running(PresetFx.ROCK, 9).rockAngle(0), "it must end perfectly level");
        assertEquals(0, running(PresetFx.ROCK, 12).rockAngle(0), "and stay level through the settle");
    }

    @Test
    void theWaveFrontsOnlyMarchOnceTheRockingIsOverAndMeetInTheMiddle() {
        for (int frame = 0; frame < 8; frame++) {
            assertEquals(0, running(PresetFx.ROCK, frame).convergeStep(0),
                    "a front moved while still tipping, frame " + frame);
        }
        for (int frame = 8; frame < 12; frame++) {
            assertEquals(frame - 7, running(PresetFx.ROCK, frame).convergeStep(0),
                    "the fronts must advance one whole step per frame");
        }
        assertEquals(0, running(PresetFx.ROCK, 12).convergeStep(0),
                "once they have met there is nothing left to march");
        assertEquals(0, new PresetFx().convergeStep(0));
    }

    @Test
    void theBlastFiresExactlyWhenTheFrontsMeetAndThenBurnsOut() {
        for (int frame = 0; frame < 12; frame++) {
            assertEquals(-1, running(PresetFx.ROCK, frame).blastAge(0),
                    "the collision happened before the fronts met, frame " + frame);
        }
        for (int frame = 12; frame < 17; frame++) {
            assertEquals(frame - 12, running(PresetFx.ROCK, frame).blastAge(0));
        }
        assertFalse(running(PresetFx.ROCK, 17).playing(0), "the effect must end after the blast");
        assertEquals(-1, new PresetFx().blastAge(0));
    }

    @Test
    void theHeatClimbsThenFallsBackSoTheCardCoolsDownAgain() {
        int peak = 0;
        for (int frame = 0; frame < PresetFx.TICK_MS; frame++) {
            peak = Math.max(peak, running(PresetFx.HAZE, frame).heat(0));
        }
        assertEquals(3, peak);
        assertEquals(1, running(PresetFx.HAZE, 1).heat(0), "it must start warm, not scalding");
        assertEquals(0, running(PresetFx.HAZE, 40).heat(0), "and end cold");
    }

    @Test
    void everyBandShiftIsAWholePixelSoTheHazeStaysOnTheGrid() {
        PresetFx fx = running(PresetFx.HAZE, 4);
        boolean moved = false;
        for (int band = 0; band < PresetFx.BANDS; band++) {
            int shift = fx.bandShift(0, band);
            assertTrue(Math.abs(shift) <= 3, "band " + band + " slid " + shift + "px");
            moved |= shift != 0;
        }
        assertTrue(moved, "no band moved at all");
    }

    @Test
    void theEruptionFlashesFirstThenShattersThenPutsItselfBackTogether() {
        assertTrue(running(PresetFx.ERUPT, 0).flashing(0));
        assertTrue(running(PresetFx.ERUPT, 1).flashing(0));
        assertFalse(running(PresetFx.ERUPT, 4).flashing(0), "the flash is two frames, not a fade");
        assertTrue(running(PresetFx.ERUPT, 4).shattered(0));
        assertFalse(running(PresetFx.ERUPT, 18).shattered(0), "the card must come back before it ends");
    }

    @Test
    void theBlocksLeaveTheCentreAndFallAsTheyGo() {
        PresetFx early = running(PresetFx.ERUPT, 4);
        PresetFx late = running(PresetFx.ERUPT, 10);
        int spread = 0;
        int fell = 0;
        for (int block = 0; block < PresetFx.BLOCKS; block++) {
            int near = Math.abs(early.blockX(0, block, CARD) - (CARD.x() + CARD.width() / 2));
            int far = Math.abs(late.blockX(0, block, CARD) - (CARD.x() + CARD.width() / 2));
            if (far > near) {
                spread++;
            }
            if (late.blockY(0, block, CARD) > early.blockY(0, block, CARD)) {
                fell++;
            }
        }
        assertTrue(spread >= PresetFx.BLOCKS - 2, "the blocks are not flying outwards");
        assertTrue(fell >= PresetFx.BLOCKS / 2, "gravity is not pulling any of them down");
    }

    @Test
    void theScannerCrossesTheCardOnceFromTopToBottom() {
        assertEquals(-1, new PresetFx().scanY(0, CARD), "an idle card has no scan line");
        int first = running(PresetFx.SCAN, 0).scanY(0, CARD);
        int last = running(PresetFx.SCAN, 8).scanY(0, CARD);
        assertEquals(CARD.y(), first);
        assertTrue(last > first);
        assertTrue(last < CARD.bottom(), "the line must stay on the card");
    }

    @Test
    void badInputIsAProgrammingErrorRatherThanASilentNoOp() {
        PresetFx fx = new PresetFx();
        assertThrows(IllegalArgumentException.class, () -> fx.trigger(-1, PresetFx.SKIP));
        assertThrows(IllegalArgumentException.class, () -> fx.trigger(0, 99));
        assertThrows(IllegalArgumentException.class, () -> fx.advance(-1));
    }
}
