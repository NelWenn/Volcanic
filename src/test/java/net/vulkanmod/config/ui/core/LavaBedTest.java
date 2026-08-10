package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LavaBedTest {
    private static final Rect AREA = new Rect(30, 40, 480, 300);

    @Test
    void theCrustCoversTheWholeWidthWithoutOverrunningIt() {
        LavaBed bed = new LavaBed(1L);
        for (int width : new int[] {1, 4, 5, 6, 100, 481}) {
            int cells = bed.cells(width);
            assertTrue((cells - 1) * LavaBed.CELL_W < width, "width " + width + " grew a cell too many");
            assertTrue(cells * LavaBed.CELL_W >= width, "width " + width + " left a gap at the right edge");
        }
        assertEquals(0, bed.cells(0));
        assertEquals(0, bed.cells(-20));
    }

    @Test
    void neighbouringCellsDifferSoTheCrustNeverReadsAsAFlatBar() {
        LavaBed bed = new LavaBed(99L);
        int sameHeight = 0;
        int sameColour = 0;
        for (int cell = 1; cell < 80; cell++) {
            if (bed.heightOf(cell) == bed.heightOf(cell - 1)) {
                sameHeight++;
            }
            if (bed.colorOf(cell) == bed.colorOf(cell - 1)) {
                sameColour++;
            }
        }
        assertTrue(sameHeight < 30, "the crust height repeats too often: " + sameHeight + "/79");
        assertTrue(sameColour < 8, "the crust colour repeats too often: " + sameColour + "/79");
    }

    @Test
    void everyCellStaysInsideTheBandSoNothingClimbsOverTheSettings() {
        LavaBed bed = new LavaBed(4L);
        for (int cell = 0; cell < 200; cell++) {
            int tall = bed.heightOf(cell);
            assertTrue(tall >= 1 && tall <= LavaBed.BAND_H, "cell " + cell + " is " + tall + " px tall");
        }
    }

    @Test
    void theCrustBreathesWithoutEverGoingOpaque() {
        LavaBed bed = new LavaBed(6L);
        int brightest = 0;
        for (int frame = 0; frame < 600; frame++) {
            bed.advance(16);
            for (int cell = 0; cell < 60; cell++) {
                brightest = Math.max(brightest, bed.colorOf(cell) >>> 24);
            }
        }
        assertTrue(brightest > 20, "the crust never lit up at all");
        assertTrue(brightest <= 130, "the crust reached alpha " + brightest + ", too solid to sit behind text");
    }

    @Test
    void aCellActuallyChangesOverTimeRatherThanSittingStill() {
        LavaBed bed = new LavaBed(8L);
        int first = bed.colorOf(3);
        boolean moved = false;
        for (int frame = 0; frame < 200 && !moved; frame++) {
            bed.advance(16);
            moved = bed.colorOf(3) != first;
        }
        assertTrue(moved, "cell 3 never pulsed");
    }

    @Test
    void theGlowSitsAtTheBottomAndNeverEscapesTheArea() {
        LavaBed bed = new LavaBed(2L);
        assertTrue(bed.glowTop(AREA) >= AREA.y());
        assertTrue(bed.glowTop(AREA) < AREA.bottom());
        Rect shallow = new Rect(0, 0, 200, 20);
        assertEquals(shallow.y(), bed.glowTop(shallow), "a short page clamps the glow to its own top");
    }

    @Test
    void theGlowStaysFaintEnoughToReadAsLightRatherThanAPanel() {
        LavaBed bed = new LavaBed(3L);
        for (int frame = 0; frame < 400; frame++) {
            bed.advance(16);
            assertTrue((bed.glowArgb() >>> 24) <= 48, "the glow became a solid band");
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
