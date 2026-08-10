package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FrameHistory")
class FrameHistoryTest {
    private static FrameHistory steady(int seconds, float frameMs) {
        FrameHistory history = new FrameHistory();
        long now = 1_000_000L;
        for (int step = 0; step < seconds * 1000 / 5; step++) {
            history.record(now, frameMs, 0.0f, 0, 0);
            now += 5;
        }
        return history;
    }

    @Test
    @DisplayName("the window holds two minutes")
    void theWindowHoldsTwoMinutes() {
        assertEquals(120_000, FrameHistory.WINDOW_MS);
        assertEquals(100, FrameHistory.BUCKET_MS);
    }

    @Test
    @DisplayName("a bucket averages its frames and keeps the worst one")
    void aBucketKeepsItsWorstFrame() {
        FrameHistory history = new FrameHistory();
        long now = 500L;
        history.record(now, 5.0f, 0.0f, 0, 0);
        history.record(now + 10, 80.0f, 0.0f, 0, 0);
        history.record(now + 20, 5.0f, 0.0f, 0, 0);

        FrameHistory.Bucket bucket = history.at(history.size() - 1);
        assertEquals(3, bucket.frames());
        assertEquals(80.0f, bucket.max(), "the spike must survive averaging");
        assertEquals(5.0f, bucket.min());
        assertEquals(30.0f, bucket.average(), 0.01f);
    }

    @Test
    @DisplayName("time moves the history forward one bucket per hundred milliseconds")
    void timeAdvancesBuckets() {
        FrameHistory history = new FrameHistory();
        long now = 0L;
        for (int i = 0; i < 10; i++) {
            history.record(now, 8.0f, 0.0f, 0, 0);
            now += FrameHistory.BUCKET_MS;
        }
        assertEquals(10, history.size());
    }

    @Test
    @DisplayName("a short gap scrolls normally, so the window follows real play time")
    void theWindowFollowsRealTime() {
        FrameHistory history = new FrameHistory();
        long now = 1_000_000L;
        history.record(now, 90.0f, 0.0f, 0, 0);
        for (int step = 0; step < 200; step++) {
            now += 5;
            history.record(now, 8.0f, 0.0f, 0, 0);
        }
        int before = history.size();

        history.advanceTo(now + 500L);

        assertTrue(history.size() > before, "half a second of play must scroll the window");
        assertEquals(90.0f, history.peak(),
                "a spike inside the two-minute window must survive the scroll");
    }

    @Test
    @DisplayName("time spent in a menu costs one bucket, not a field of empty ones")
    void aLongGapDoesNotPunchAHole() {
        FrameHistory history = new FrameHistory();
        long now = 1_000_000L;
        for (int step = 0; step < 100; step++) {
            history.record(now, 8.0f, 0.0f, 0, 0);
            now += 20;
        }
        int before = history.size();
        float peakBefore = history.peak();

        history.record(now + 90_000L, 8.0f, 0.0f, 0, 0);

        assertEquals(before + 1, history.size(),
                "ninety seconds away must not scroll nine hundred empty buckets in");
        assertEquals(peakBefore, history.peak(), "the history we came back to read is still there");
    }

    @Test
    @DisplayName("a paused history does not scroll either")
    void pauseAlsoStopsTime() {
        FrameHistory history = steady(2, 8.0f);
        int frozen = history.size();

        history.setPaused(true);
        history.advanceTo(2_000_000L + 60_000L);

        assertEquals(frozen, history.size());
    }

    @Test
    @DisplayName("pausing stops recording, resuming picks it back up")
    void pauseStopsRecording() {
        FrameHistory history = steady(2, 8.0f);
        int frozen = history.size();

        history.setPaused(true);
        long now = 2_000_000L;
        for (int i = 0; i < 50; i++) {
            history.record(now, 40.0f, 0.0f, 0, 0);
            now += FrameHistory.BUCKET_MS;
        }
        assertTrue(history.paused());
        assertEquals(frozen, history.size(), "a paused history must not move");

        history.setPaused(false);
        history.record(now, 8.0f, 0.0f, 0, 0);
        assertTrue(history.size() > frozen);
    }

    @Test
    @DisplayName("merging columns keeps the worst frame, never averages a spike away")
    void mergingKeepsTheSpike() {
        FrameHistory history = new FrameHistory();
        long now = 0L;
        for (int bucket = 0; bucket < 40; bucket++) {
            history.record(now, bucket == 17 ? 90.0f : 8.0f, 0.0f, 0, 0);
            now += FrameHistory.BUCKET_MS;
        }

        List<FrameHistory.Bucket> columns = history.columns(8);
        assertTrue(columns.size() <= 8);
        assertEquals(90.0f, columns.stream().map(FrameHistory.Bucket::max).max(Float::compare).orElse(0.0f),
                "the spike must survive the merge");
    }

    @Test
    @DisplayName("the worst buckets come back sorted, and only those above the floor")
    void worstReportsTheRealStutters() {
        FrameHistory history = new FrameHistory();
        long now = 0L;
        float[] series = {8, 8, 60, 8, 90, 8, 30, 8};
        for (float value : series) {
            history.record(now, value, 0.0f, 0, 0);
            now += FrameHistory.BUCKET_MS;
        }

        List<FrameHistory.Bucket> worst = history.worst(3, 25.0f);
        assertEquals(3, worst.size());
        assertEquals(90.0f, worst.get(0).max());
        assertEquals(60.0f, worst.get(1).max());
        assertEquals(30.0f, worst.get(2).max());
        assertTrue(history.worst(5, 200.0f).isEmpty(), "nothing above the floor means no stutters");
    }

    @Test
    @DisplayName("causes are attributed to the bucket they happened in")
    void causesRideWithTheirBucket() {
        FrameHistory history = new FrameHistory();
        history.record(0L, 80.0f, 74.0f, 2, 1);

        FrameHistory.Bucket bucket = history.at(history.size() - 1);
        assertEquals(74.0f, bucket.gcMs());
        assertEquals(2, bucket.uploads());
        assertEquals(1, bucket.builds());
    }

    @Test
    @DisplayName("an empty history reports nothing rather than throwing")
    void emptyHistoryIsSafe() {
        FrameHistory history = new FrameHistory();

        assertEquals(0, history.size());
        assertTrue(history.columns(100).isEmpty());
        assertTrue(history.worst(5, 1.0f).isEmpty());
        assertTrue(history.at(0).empty());
        assertEquals(-1.0f, history.medianAverage());
    }

    @Test
    @DisplayName("a nonsense frame time is ignored rather than poisoning the window")
    void nonsenseFramesAreIgnored() {
        FrameHistory history = new FrameHistory();
        history.record(0L, 0.0f, 0.0f, 0, 0);
        history.record(0L, Float.NaN, 0.0f, 0, 0);
        history.record(0L, Float.POSITIVE_INFINITY, 0.0f, 0, 0);

        assertEquals(0, history.size());
    }

    @Test
    @DisplayName("the ring never grows past its window")
    void theRingStaysBounded() {
        FrameHistory history = new FrameHistory();
        long now = 0L;
        for (int bucket = 0; bucket < FrameHistory.BUCKETS * 2; bucket++) {
            history.record(now, 8.0f, 0.0f, 0, 0);
            now += FrameHistory.BUCKET_MS;
        }
        assertEquals(FrameHistory.BUCKETS, history.size());
        assertFalse(history.columns(300).isEmpty());
    }
}
