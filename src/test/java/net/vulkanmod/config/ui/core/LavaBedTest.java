package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LavaBedTest {
    private static final Rect AREA = new Rect(30, 40, 480, 300);

    @Test
    void everyPatchIsWiderThanItIsTallSoTheBedNeverReadsAsBars() {
        LavaBed bed = new LavaBed(1L);
        for (int patch = 0; patch < LavaBed.PATCHES; patch++) {
            assertTrue(bed.widthOf(patch) > bed.heightOf(patch) * 2,
                    "patch " + patch + " is " + bed.widthOf(patch) + "x" + bed.heightOf(patch)
                            + ", tall enough to draw a column");
        }
    }

    @Test
    void patchesSitAtDifferentHeightsSoTheTopEdgeIsNotAStraightLine() {
        LavaBed bed = new LavaBed(21L);
        int distinct = (int) java.util.stream.IntStream.range(0, LavaBed.PATCHES)
                .map(patch -> bed.yOf(patch, AREA)).distinct().count();
        assertTrue(distinct >= 5, "only " + distinct + " distinct tops, the bed would look like a bar");
    }

    @Test
    void everyPatchStaysInsideTheBandAtTheBottomOfTheArea() {
        LavaBed bed = new LavaBed(4L);
        for (int patch = 0; patch < LavaBed.PATCHES; patch++) {
            int top = bed.yOf(patch, AREA);
            int bottom = top + bed.heightOf(patch);
            assertTrue(bottom <= AREA.bottom(), "patch " + patch + " hangs below the area");
            assertTrue(top >= AREA.bottom() - LavaBed.BAND_H,
                    "patch " + patch + " climbed " + (AREA.bottom() - top) + "px, past the band");
        }
    }

    @Test
    void everyPatchStaysInsideTheAreaHorizontally() {
        LavaBed bed = new LavaBed(9L);
        for (Rect area : new Rect[] {AREA, new Rect(0, 0, 40, 60), new Rect(5, 5, 1, 30)}) {
            for (int patch = 0; patch < LavaBed.PATCHES; patch++) {
                assertTrue(bed.xOf(patch, area) >= area.x(), "patch " + patch + " started left of the area");
                assertTrue(bed.xOf(patch, area) <= area.right(), "patch " + patch + " started past the area");
            }
        }
    }

    @Test
    void theBedBreathesWithoutEverGoingSolid() {
        LavaBed bed = new LavaBed(6L);
        int brightest = 0;
        for (int frame = 0; frame < 600; frame++) {
            bed.advance(16);
            for (int patch = 0; patch < LavaBed.PATCHES; patch++) {
                brightest = Math.max(brightest, bed.colorOf(patch) >>> 24);
            }
        }
        assertTrue(brightest > 18, "the bed never lit up at all");
        assertTrue(brightest <= 80, "a patch reached alpha " + brightest + ", too solid to sit under text");
    }

    @Test
    void patchesPulseOutOfStepSoTheBedNeverFlashesAsOneBlock() {
        LavaBed bed = new LavaBed(8L);
        for (int frame = 0; frame < 40; frame++) {
            bed.advance(16);
        }
        int distinct = (int) java.util.stream.IntStream.range(0, LavaBed.PATCHES)
                .map(patch -> bed.colorOf(patch) >>> 24).distinct().count();
        assertTrue(distinct >= 8, "only " + distinct + " distinct brightnesses, the bed pulses in unison");
    }

    @Test
    void theGlowSitsAtTheBottomAndStaysFaintEnoughToReadAsLight() {
        LavaBed bed = new LavaBed(3L);
        assertTrue(bed.glowTop(AREA) >= AREA.y() && bed.glowTop(AREA) < AREA.bottom());
        assertEquals(0, bed.glowTop(new Rect(0, 0, 200, 20)), "a short page clamps the glow to its own top");
        for (int frame = 0; frame < 400; frame++) {
            bed.advance(16);
            assertTrue((bed.glowArgb() >>> 24) <= 46, "the glow became a solid band");
        }
    }

    @Test
    void theSameElapsedTimeLandsInTheSamePlaceWhateverTheFrameRate() {
        LavaBed slow = new LavaBed(5L);
        LavaBed fast = new LavaBed(5L);
        for (int frame = 0; frame < 8; frame++) {
            slow.advance(50);
        }
        for (int frame = 0; frame < 40; frame++) {
            fast.advance(10);
        }
        assertEquals(slow.colorOf(7) >>> 24, fast.colorOf(7) >>> 24, 1);
    }

    @Test
    void aStalledFrameCannotJumpThePulseAndANegativeFrameIsAProgrammingError() {
        LavaBed huge = new LavaBed(7L);
        LavaBed capped = new LavaBed(7L);
        huge.advance(9000);
        capped.advance(100);
        assertEquals(capped.colorOf(2), huge.colorOf(2));
        assertThrows(IllegalArgumentException.class, () -> new LavaBed(1L).advance(-1));
    }
}
