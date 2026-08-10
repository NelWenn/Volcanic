package net.vulkanmod.config.ui.core;

import java.util.Arrays;

public final class FrameSamples {
    public static final int CAPACITY = 2048;
    public static final int READY_AT = 300;
    public static final int P1_AT = 1000;

    private final float[] millis = new float[CAPACITY];
    private int count;
    private int next;
    private int fingerprint;
    private boolean measured;

    public void record(int contextFingerprint, float frameMs) {
        if (frameMs <= 0.0f || Float.isNaN(frameMs) || Float.isInfinite(frameMs)) {
            return;
        }
        if (contextFingerprint != fingerprint) {
            clear();
            this.fingerprint = contextFingerprint;
        }
        millis[next] = frameMs;
        next = (next + 1) % CAPACITY;
        if (count < CAPACITY) {
            count++;
        }
    }

    public void clear() {
        this.count = 0;
        this.next = 0;
        this.measured = false;
    }

    public int count() {
        return count;
    }

    public int fingerprint() {
        return fingerprint;
    }

    public boolean measured() {
        return measured;
    }

    public void markMeasured() {
        this.measured = true;
    }

    public boolean ready() {
        return count >= READY_AT;
    }

    public boolean hasLowPercentile() {
        return count >= P1_AT;
    }

    public float median() {
        return percentile(50.0f);
    }

    public float p95() {
        return percentile(95.0f);
    }

    public float p1() {
        return hasLowPercentile() ? percentile(1.0f) : -1.0f;
    }

    public float fps() {
        float median = median();
        return median <= 0.0f ? -1.0f : 1000.0f / median;
    }

    public float percentile(float rank) {
        if (rank < 0.0f || rank > 100.0f) {
            throw new IllegalArgumentException("rank must be within 0..100: " + rank);
        }
        if (count == 0) {
            return -1.0f;
        }
        float[] sorted = sorted();
        int index = Math.round((rank / 100.0f) * (sorted.length - 1));
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }

    public float[] recent(int wanted) {
        if (wanted < 0) {
            throw new IllegalArgumentException("wanted must not be negative: " + wanted);
        }
        int taken = Math.min(wanted, count);
        float[] out = new float[taken];
        for (int i = 0; i < taken; i++) {
            int at = Math.floorMod(next - taken + i, CAPACITY);
            out[i] = millis[at];
        }
        return out;
    }

    public record Summary(int count, float average, float median, float p95, float p1,
                          float low1, float low01, float max, int spikes) {
    }

    public Summary summary() {
        if (count == 0) {
            return new Summary(0, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 0);
        }
        float[] sorted = sorted();
        float total = 0.0f;
        for (float value : sorted) {
            total += value;
        }
        float median = at(sorted, 50.0f);
        float floor = spikeFloor(median);
        int spikes = 0;
        for (int i = sorted.length - 1; i >= 0 && sorted[i] >= floor; i--) {
            spikes++;
        }
        return new Summary(count, total / sorted.length, median, at(sorted, 95.0f),
                count >= P1_AT ? at(sorted, 1.0f) : -1.0f,
                tail(sorted, 1.0f), count >= P1_AT ? tail(sorted, 0.1f) : -1.0f,
                sorted[sorted.length - 1], spikes);
    }

    public static float spikeFloor(float medianMs) {
        return medianMs <= 0.0f ? -1.0f : Math.max(medianMs * 2.0f, medianMs + 5.0f);
    }

    private static float tail(float[] sorted, float percent) {
        int taken = Math.max(1, Math.round(sorted.length * percent / 100.0f));
        float total = 0.0f;
        for (int i = sorted.length - taken; i < sorted.length; i++) {
            total += sorted[i];
        }
        return total / taken;
    }

    private static float at(float[] sorted, float rank) {
        int index = Math.round((rank / 100.0f) * (sorted.length - 1));
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }

    private float[] sorted() {
        float[] copy = new float[count];
        for (int i = 0; i < count; i++) {
            copy[i] = millis[Math.floorMod(next - count + i, CAPACITY)];
        }
        Arrays.sort(copy);
        return copy;
    }
}
