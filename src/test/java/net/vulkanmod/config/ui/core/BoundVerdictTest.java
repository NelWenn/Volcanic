package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundVerdictTest {
    private static BoundVerdict.Signals signals(double wall, double cpu, double gpu) {
        return new BoundVerdict.Signals(wall, cpu, gpu, 0.0, true, 0.0);
    }

    @Test
    void theRenderThreadIsNamedWhenItBurnsTheFrame() {
        assertEquals(BoundVerdict.RENDER_CPU, BoundVerdict.of(signals(3.12, 2.37, 1.98)));
    }

    @Test
    void theGpuIsNamedWhenItOwnsMostOfTheFrame() {
        assertEquals(BoundVerdict.GPU, BoundVerdict.of(signals(10.0, 2.0, 8.0)));
    }

    @Test
    void anIdleFrameIsNotCalledAGpuWait() {
        BoundVerdict verdict = BoundVerdict.of(signals(67.4, 0.62, 0.66));

        assertEquals(BoundVerdict.IDLE, verdict);
        assertNotEquals(BoundVerdict.GPU, verdict, "the log's old classifier called this a GPU wait");
    }

    @Test
    void sittingOnTheFrameCapIsReportedAsSuch() {
        BoundVerdict.Signals atCap = new BoundVerdict.Signals(8.33, 2.0, 3.0, 8.33, true, 0.0);
        assertEquals(BoundVerdict.CAPPED, BoundVerdict.of(atCap));

        BoundVerdict.Signals nearCap = new BoundVerdict.Signals(8.9, 2.0, 3.0, 8.33, true, 0.0);
        assertEquals(BoundVerdict.CAPPED, BoundVerdict.of(nearCap));

        BoundVerdict.Signals wellBelow = new BoundVerdict.Signals(3.0, 2.4, 2.0, 8.33, true, 0.0);
        assertNotEquals(BoundVerdict.CAPPED, BoundVerdict.of(wellBelow));
    }

    @Test
    void theCapIsCheckedBeforeAnythingElse() {
        BoundVerdict.Signals gpuHeavyButCapped =
                new BoundVerdict.Signals(16.7, 2.0, 15.0, 16.7, true, 0.0);
        assertEquals(BoundVerdict.CAPPED, BoundVerdict.of(gpuHeavyButCapped));
    }

    @Test
    void aWorldThatNeverFinishesMeshingOutranksTheRenderer() {
        BoundVerdict.Signals meshing = new BoundVerdict.Signals(20.0, 18.0, 2.0, 0.0, false, 0.0);
        assertEquals(BoundVerdict.MESHING, BoundVerdict.of(meshing));
    }

    @Test
    void anExpensiveServerTickOutranksEverythingButTheCap() {
        BoundVerdict.Signals tick = new BoundVerdict.Signals(60.0, 50.0, 5.0, 0.0, true, 80.0);
        assertEquals(BoundVerdict.SERVER_TICK, BoundVerdict.of(tick));
    }

    @Test
    void anAbsentGpuReadingNeverMakesItTheCulprit() {
        assertNotEquals(BoundVerdict.GPU, BoundVerdict.of(signals(10.0, 1.0, -1.0)));
        assertEquals(BoundVerdict.IDLE, BoundVerdict.of(signals(10.0, 0.5, -1.0)));
    }

    @Test
    void anUnremarkableFrameSaysSoRatherThanGuessing() {
        assertEquals(BoundVerdict.UNKNOWN, BoundVerdict.of(signals(10.0, 5.0, 4.0)));
    }

    @Test
    void everyVerdictNamesADistinctMessageKey() {
        assertEquals(BoundVerdict.values().length,
                java.util.Arrays.stream(BoundVerdict.values())
                        .map(BoundVerdict::messageKey).distinct().count());
        assertEquals("vulkanmod.overview.bound.render_cpu", BoundVerdict.RENDER_CPU.messageKey());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> BoundVerdict.of(null));
        assertThrows(IllegalArgumentException.class,
                () -> new BoundVerdict.Signals(0.0, 1.0, 1.0, 0.0, true, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> new BoundVerdict.Signals(-5.0, 1.0, 1.0, 0.0, true, 0.0));
    }
}
