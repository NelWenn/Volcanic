package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrameSamplesTest {
    private static FrameSamples filled(int fingerprint, int from, int to) {
        FrameSamples samples = new FrameSamples();
        for (int value = from; value <= to; value++) {
            samples.record(fingerprint, value);
        }
        return samples;
    }

    @Test
    void percentilesMatchAKnownSeries() {
        FrameSamples samples = filled(1, 1, 100);

        assertEquals(50.0f, samples.median(), 1.0f);
        assertEquals(95.0f, samples.p95(), 1.0f);
        assertEquals(100.0f, samples.percentile(100.0f));
        assertEquals(1.0f, samples.percentile(0.0f));
    }

    @Test
    void fpsIsTheInverseOfTheMedian() {
        FrameSamples samples = new FrameSamples();
        for (int i = 0; i < 400; i++) {
            samples.record(1, 20.0f);
        }
        assertEquals(50.0f, samples.fps(), 0.01f);
    }

    @Test
    void nothingIsReportedBeforeTheReadyGate() {
        FrameSamples samples = filled(1, 1, FrameSamples.READY_AT - 1);

        assertFalse(samples.ready());
        assertEquals(FrameSamples.READY_AT - 1, samples.count());

        samples.record(1, 5.0f);
        assertTrue(samples.ready());
    }

    @Test
    void theLowPercentileWaitsForItsOwnGate() {
        FrameSamples samples = filled(1, 1, FrameSamples.P1_AT - 1);

        assertFalse(samples.hasLowPercentile());
        assertEquals(-1.0f, samples.p1());

        samples.record(1, 5.0f);
        assertTrue(samples.hasLowPercentile());
        assertTrue(samples.p1() > 0.0f);
    }

    @Test
    void aChangedFingerprintThrowsTheWindowAway() {
        FrameSamples samples = filled(1, 1, 500);
        assertTrue(samples.ready());

        samples.record(2, 12.0f);

        assertEquals(1, samples.count());
        assertEquals(2, samples.fingerprint());
        assertFalse(samples.ready());
    }

    @Test
    void theRingKeepsTheMostRecentSamplesOnly() {
        FrameSamples samples = filled(1, 1, FrameSamples.CAPACITY + 500);

        assertEquals(FrameSamples.CAPACITY, samples.count());
        assertEquals(FrameSamples.CAPACITY + 500, samples.percentile(100.0f), 0.5f);
        assertEquals(501.0f, samples.percentile(0.0f), 0.5f);
    }

    @Test
    void recentReturnsTheTailInOrderAcrossTheWrap() {
        FrameSamples samples = filled(1, 1, FrameSamples.CAPACITY + 10);
        float[] tail = samples.recent(3);

        assertEquals(3, tail.length);
        assertEquals(FrameSamples.CAPACITY + 8, tail[0], 0.5f);
        assertEquals(FrameSamples.CAPACITY + 9, tail[1], 0.5f);
        assertEquals(FrameSamples.CAPACITY + 10, tail[2], 0.5f);
    }

    @Test
    void recentIsClampedToWhatExists() {
        assertEquals(0, new FrameSamples().recent(240).length);
        assertEquals(5, filled(1, 1, 5).recent(240).length);
    }

    @Test
    void nonsenseFrameTimesAreIgnored() {
        FrameSamples samples = new FrameSamples();
        samples.record(1, 0.0f);
        samples.record(1, -3.0f);
        samples.record(1, Float.NaN);
        samples.record(1, Float.POSITIVE_INFINITY);

        assertEquals(0, samples.count());
    }

    @Test
    void anEmptyWindowReportsNothingRatherThanZero() {
        FrameSamples samples = new FrameSamples();

        assertEquals(-1.0f, samples.median());
        assertEquals(-1.0f, samples.fps());
        assertEquals(-1.0f, samples.p1());
        assertFalse(samples.ready());
    }

    @Test
    void clearingKeepsTheFingerprintButDropsTheData() {
        FrameSamples samples = filled(7, 1, 400);
        samples.markMeasured();
        samples.clear();

        assertEquals(0, samples.count());
        assertEquals(7, samples.fingerprint());
        assertFalse(samples.measured());
    }

    @Test
    void rejectsAnImpossibleRank() {
        assertThrows(IllegalArgumentException.class, () -> new FrameSamples().percentile(-1.0f));
        assertThrows(IllegalArgumentException.class, () -> new FrameSamples().percentile(101.0f));
        assertThrows(IllegalArgumentException.class, () -> new FrameSamples().recent(-1));
    }

    @Test
    @DisplayName("one summary agrees with every percentile it replaces")
    void summaryAgreesWithTheIndividualPercentiles() {
        FrameSamples samples = new FrameSamples();
        for (int i = 1; i <= FrameSamples.P1_AT; i++) {
            samples.record(0, i);
        }
        FrameSamples.Summary summary = samples.summary();

        assertEquals(samples.count(), summary.count());
        assertEquals(samples.median(), summary.median());
        assertEquals(samples.p95(), summary.p95());
        assertEquals(samples.p1(), summary.p1());
    }

    @Test
    @DisplayName("the average is the mean of the window, not the median")
    void averageIsTheMean() {
        FrameSamples samples = new FrameSamples();
        for (float value : new float[] {10f, 10f, 10f, 50f}) {
            samples.record(0, value);
        }
        FrameSamples.Summary summary = samples.summary();

        assertEquals(20.0f, summary.average(), 0.001f);
        assertEquals(10.0f, summary.median());
        assertEquals(50.0f, summary.max());
    }

    @Test
    @DisplayName("the low averages read the worst tail, not a single sample")
    void lowAveragesReadTheTail() {
        FrameSamples samples = new FrameSamples();
        for (int i = 1; i <= 100; i++) {
            samples.record(0, i);
        }
        FrameSamples.Summary summary = samples.summary();

        assertEquals(100.0f, summary.low1(), 0.001f, "the worst 1% of 100 frames is the worst frame");
        assertTrue(summary.low1() > summary.median(), "a low must be worse than the median");
    }

    @Test
    @DisplayName("a fast frame is never counted as a spike")
    void spikeFloorHasAnAbsoluteGuard() {
        assertEquals(7.0f, FrameSamples.spikeFloor(2.0f));
        assertEquals(40.0f, FrameSamples.spikeFloor(20.0f));
        assertEquals(-1.0f, FrameSamples.spikeFloor(0.0f));
    }

    @Test
    @DisplayName("spikes are counted, and a steady run has none")
    void spikesAreCounted() {
        FrameSamples steady = new FrameSamples();
        for (int i = 0; i < 200; i++) {
            steady.record(0, 16.0f);
        }
        assertEquals(0, steady.summary().spikes());

        FrameSamples stuttering = new FrameSamples();
        for (int i = 0; i < 200; i++) {
            stuttering.record(0, i % 50 == 0 ? 120.0f : 16.0f);
        }
        assertEquals(4, stuttering.summary().spikes());
    }

    @Test
    @DisplayName("an empty buffer summarises to nothing rather than throwing")
    void emptyBufferSummarises() {
        FrameSamples.Summary summary = new FrameSamples().summary();

        assertEquals(0, summary.count());
        assertEquals(-1.0f, summary.median());
        assertEquals(0, summary.spikes());
    }

    @Test
    @DisplayName("the low tail stays hidden until there are enough frames to mean anything")
    void tailPercentilesWaitForEnoughFrames() {
        FrameSamples samples = new FrameSamples();
        for (int i = 0; i < FrameSamples.READY_AT; i++) {
            samples.record(0, 16.0f);
        }

        assertEquals(-1.0f, samples.summary().p1());
        assertEquals(-1.0f, samples.summary().low01());
        assertTrue(samples.summary().low1() > 0.0f, "the 1% low needs no extra warm-up");
    }
}
