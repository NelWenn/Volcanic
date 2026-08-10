package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoalBedTest {
    private static final Rect AREA = new Rect(30, 40, 620, 400);

    @Test
    void theRocksAreOpaqueEnoughToHideTheLavaTheySitOn() {
        CoalBed bed = new CoalBed(1L);
        for (int chunk = 0; chunk < CoalBed.CHUNKS; chunk++) {
            assertEquals(255, bed.chunkArgb(chunk) >>> 24,
                    "chunk " + chunk + " is translucent, so the glow would wash straight through it");
        }
    }

    @Test
    void theRocksLeaveGapsForTheLavaToShowThrough() {
        CoalBed bed = new CoalBed(12L);
        int covered = IntStream.range(0, CoalBed.CHUNKS).map(bed::chunkWidth).sum();
        assertTrue(covered < AREA.width() * 3,
                "the rocks would tile into a solid crust: " + covered + "px over " + AREA.width());
        assertTrue(covered > AREA.width(), "too few rocks to read as a pile: " + covered);
    }

    @Test
    void theRidgeIsUnevenRatherThanAFlatLine() {
        CoalBed bed = new CoalBed(5L);
        long tops = IntStream.range(0, CoalBed.CHUNKS).map(chunk -> bed.chunkY(chunk, AREA)).distinct().count();
        assertTrue(tops >= 10, "only " + tops + " distinct rock tops, the pile would look like a bar");
    }

    @Test
    void thePileIsDenseAtTheBottomAndThinsOutTowardsTheTop() {
        CoalBed bed = new CoalBed(5L);
        int low = 0;
        int high = 0;
        for (int chunk = 0; chunk < CoalBed.CHUNKS; chunk++) {
            int lift = AREA.bottom() - bed.chunkY(chunk, AREA);
            if (lift <= CoalBed.BAND_H / 2) {
                low++;
            } else {
                high++;
            }
        }
        assertTrue(low > high, "the pile floats: " + low + " low against " + high + " high");
    }

    @Test
    void everyRockAndEveryVentStaysInsideTheAreaItWasGiven() {
        CoalBed bed = new CoalBed(9L);
        for (Rect area : new Rect[] {AREA, new Rect(0, 0, 60, 90), new Rect(5, 5, 1, 200)}) {
            for (int chunk = 0; chunk < CoalBed.CHUNKS; chunk++) {
                assertTrue(bed.chunkX(chunk, area) >= area.x());
                assertTrue(bed.chunkX(chunk, area) <= area.right());
                assertTrue(bed.chunkY(chunk, area) + bed.chunkHeight(chunk) <= area.bottom());
                assertTrue(bed.chunkY(chunk, area) >= area.bottom() - CoalBed.BAND_H);
            }
            for (int vent = 0; vent < CoalBed.VENTS; vent++) {
                assertTrue(bed.ventX(vent, area) >= area.x());
                assertTrue(bed.ventY(vent, area) + bed.ventHeight(vent) <= area.bottom());
                assertTrue(bed.ventY(vent, area) >= area.bottom() - CoalBed.LAVA_H);
            }
        }
    }

    @Test
    void theVentsAreWiderThanTheyAreTallSoTheGlowPoolsInsteadOfSpiking() {
        CoalBed bed = new CoalBed(3L);
        for (int vent = 0; vent < CoalBed.VENTS; vent++) {
            assertTrue(bed.ventWidth(vent) > bed.ventHeight(vent) * 2,
                    "vent " + vent + " is " + bed.ventWidth(vent) + "x" + bed.ventHeight(vent));
        }
    }

    @Test
    void theLavaFadesUpwardsSoItNeverEndsOnAHardEdge() {
        CoalBed bed = new CoalBed(2L);
        assertEquals(0, bed.lavaTopArgb() >>> 24, "the top of the lava band must be fully transparent");
        assertTrue((bed.lavaBottomArgb() >>> 24) > 100, "the bottom of the lava band never lights up");
        Rect band = bed.lavaBand(AREA);
        assertEquals(AREA.bottom(), band.bottom());
        assertEquals(CoalBed.LAVA_H, band.height());
        assertEquals(12, bed.lavaBand(new Rect(0, 0, 40, 12)).height(), "a short page cannot overflow");
    }

    @Test
    void theVentsPulseOutOfStepSoTheBedNeverFlashesAsOneBlock() {
        CoalBed bed = new CoalBed(8L);
        for (int frame = 0; frame < 40; frame++) {
            bed.advance(16);
        }
        long shades = IntStream.range(0, CoalBed.VENTS).map(vent -> bed.ventArgb(vent) >>> 24).distinct().count();
        assertTrue(shades >= 8, "only " + shades + " distinct vent brightnesses");
    }

    @Test
    void theAmbientGlowStaysFaintEnoughToReadAsLightRatherThanAPanel() {
        CoalBed bed = new CoalBed(4L);
        assertTrue(bed.glowTop(AREA) >= AREA.y() && bed.glowTop(AREA) < AREA.bottom());
        assertEquals(0, bed.glowTop(new Rect(0, 0, 200, 20)), "a short page clamps the glow to its own top");
        for (int frame = 0; frame < 400; frame++) {
            bed.advance(16);
            assertTrue((bed.glowArgb() >>> 24) <= 60, "the glow became a solid band");
        }
    }

    @Test
    void theSameElapsedTimeLandsInTheSamePlaceWhateverTheFrameRate() {
        CoalBed slow = new CoalBed(5L);
        CoalBed fast = new CoalBed(5L);
        for (int frame = 0; frame < 8; frame++) {
            slow.advance(50);
        }
        for (int frame = 0; frame < 40; frame++) {
            fast.advance(10);
        }
        assertEquals(slow.ventArgb(7) >>> 24, fast.ventArgb(7) >>> 24, 1);
    }

    @Test
    void aStalledFrameCannotJumpThePulseAndANegativeFrameIsAProgrammingError() {
        CoalBed huge = new CoalBed(7L);
        CoalBed capped = new CoalBed(7L);
        huge.advance(9000);
        capped.advance(100);
        assertEquals(capped.ventArgb(2), huge.ventArgb(2));
        assertThrows(IllegalArgumentException.class, () -> new CoalBed(1L).advance(-1));
    }
}
