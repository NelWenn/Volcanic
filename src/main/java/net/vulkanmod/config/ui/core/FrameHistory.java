package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class FrameHistory {
    public static final int BUCKET_MS = 100;
    public static final int BUCKETS = 1200;
    public static final int WINDOW_MS = BUCKET_MS * BUCKETS;
    private static final int RESUME_STEPS = 10;


    private final float[] min = new float[BUCKETS];
    private final float[] max = new float[BUCKETS];
    private final float[] sum = new float[BUCKETS];
    private final int[] count = new int[BUCKETS];
    private final float[] gcMs = new float[BUCKETS];
    private final short[] uploads = new short[BUCKETS];
    private final short[] builds = new short[BUCKETS];
    private final long[] startMs = new long[BUCKETS];
    private final float[] heapMb = new float[BUCKETS];

    private int head = -1;
    private int filled;
    private long bucketStartMs;
    private boolean paused;

    public record Bucket(long startMs, float min, float average, float max, float gcMs,
                         int uploads, int builds, int frames, float heapMb) {
        public boolean empty() {
            return frames == 0;
        }
    }

    public void record(long nowMs, float frameMs, float gcDeltaMs, int uploadCount, int buildCount) {
        record(nowMs, frameMs, gcDeltaMs, uploadCount, buildCount, 0.0f);
    }

    public void record(long nowMs, float frameMs, float gcDeltaMs, int uploadCount, int buildCount,
                       float heapUsedMb) {
        if (paused || frameMs <= 0.0f || Float.isNaN(frameMs) || Float.isInfinite(frameMs)) {
            return;
        }
        advanceTo(nowMs);
        min[head] = count[head] == 0 ? frameMs : Math.min(min[head], frameMs);
        max[head] = Math.max(max[head], frameMs);
        sum[head] += frameMs;
        count[head]++;
        gcMs[head] += Math.max(0.0f, gcDeltaMs);
        uploads[head] = clamp(uploads[head] + Math.max(0, uploadCount));
        builds[head] = clamp(builds[head] + Math.max(0, buildCount));
        heapMb[head] = heapUsedMb;
    }

    public Bucket busiestAllocator() {
        Bucket worst = null;
        float previous = -1.0f;
        float grown = 0.0f;
        for (int age = 0; age < filled; age++) {
            Bucket bucket = at(age);
            if (bucket.heapMb() <= 0.0f) {
                previous = -1.0f;
                continue;
            }
            if (previous > 0.0f && bucket.heapMb() - previous > grown) {
                grown = bucket.heapMb() - previous;
                worst = bucket;
            }
            previous = bucket.heapMb();
        }
        return worst;
    }

    public float allocationRateMbPerSecond() {
        float grown = 0.0f;
        float previous = -1.0f;
        int counted = 0;
        for (int age = 0; age < filled; age++) {
            Bucket bucket = at(age);
            if (bucket.heapMb() <= 0.0f) {
                previous = -1.0f;
                continue;
            }
            if (previous > 0.0f && bucket.heapMb() > previous) {
                grown += bucket.heapMb() - previous;
            }
            previous = bucket.heapMb();
            counted++;
        }
        return counted < 2 ? -1.0f : grown / (counted * BUCKET_MS / 1000.0f);
    }

    public void advanceTo(long nowMs) {
        if (paused) {
            return;
        }
        if (head < 0) {
            open(0, nowMs - nowMs % BUCKET_MS);
            return;
        }
        long steps = (nowMs - bucketStartMs) / BUCKET_MS;
        if (steps <= 0) {
            return;
        }
        if (steps > RESUME_STEPS) {
            open(Math.floorMod(head + 1, BUCKETS), nowMs - nowMs % BUCKET_MS);
            return;
        }
        for (long step = 0; step < steps; step++) {
            open(Math.floorMod(head + 1, BUCKETS), bucketStartMs + BUCKET_MS);
        }
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean paused() {
        return paused;
    }

    public void clear() {
        this.head = -1;
        this.filled = 0;
        this.paused = false;
    }

    public int size() {
        return filled;
    }

    public List<Bucket> columns(int wanted) {
        if (wanted <= 0 || filled == 0) {
            return List.of();
        }
        int merge = Math.max(1, (filled + wanted - 1) / wanted);
        int groups = (filled + merge - 1) / merge;
        List<Bucket> out = new ArrayList<>(groups);
        for (int group = 0; group < groups; group++) {
            int from = filled - (groups - group) * merge;
            out.add(merged(Math.max(0, from), merge));
        }
        return List.copyOf(out);
    }

    public List<Bucket> worst(int wanted, float floorMs) {
        if (wanted <= 0 || filled == 0) {
            return List.of();
        }
        List<Bucket> hits = new ArrayList<>();
        for (int age = filled - 1; age >= 0; age--) {
            Bucket bucket = at(age);
            if (!bucket.empty() && bucket.max() >= floorMs) {
                hits.add(bucket);
            }
        }
        hits.sort((left, right) -> Float.compare(right.max(), left.max()));
        return List.copyOf(hits.subList(0, Math.min(wanted, hits.size())));
    }

    public Bucket at(int age) {
        if (age < 0 || age >= filled) {
            return new Bucket(0L, 0.0f, 0.0f, 0.0f, 0.0f, 0, 0, 0, 0.0f);
        }
        return bucketAt(Math.floorMod(head - (filled - 1 - age), BUCKETS));
    }

    public float peak() {
        float peak = 0.0f;
        for (int age = 0; age < filled; age++) {
            peak = Math.max(peak, at(age).max());
        }
        return peak;
    }

    public float medianAverage() {
        if (filled == 0) {
            return -1.0f;
        }
        float[] values = new float[filled];
        int taken = 0;
        for (int age = 0; age < filled; age++) {
            Bucket bucket = at(age);
            if (!bucket.empty()) {
                values[taken++] = bucket.average();
            }
        }
        if (taken == 0) {
            return -1.0f;
        }
        java.util.Arrays.sort(values, 0, taken);
        return values[taken / 2];
    }

    private Bucket merged(int from, int span) {
        float low = Float.MAX_VALUE;
        float high = 0.0f;
        float total = 0.0f;
        float gc = 0.0f;
        int up = 0;
        int build = 0;
        int frames = 0;
        long start = 0L;
        for (int offset = 0; offset < span && from + offset < filled; offset++) {
            Bucket bucket = at(from + offset);
            if (start == 0L) {
                start = bucket.startMs();
            }
            if (bucket.empty()) {
                continue;
            }
            low = Math.min(low, bucket.min());
            high = Math.max(high, bucket.max());
            total += bucket.average() * bucket.frames();
            gc += bucket.gcMs();
            up += bucket.uploads();
            build += bucket.builds();
            frames += bucket.frames();
        }
        return frames == 0
                ? new Bucket(start, 0.0f, 0.0f, 0.0f, gc, up, build, 0, 0.0f)
                : new Bucket(start, low, total / frames, high, gc, up, build, frames, 0.0f);
    }

    private Bucket bucketAt(int index) {
        int frames = count[index];
        return new Bucket(startMs[index], frames == 0 ? 0.0f : min[index],
                frames == 0 ? 0.0f : sum[index] / frames, max[index], gcMs[index],
                uploads[index], builds[index], frames, heapMb[index]);
    }

    private void open(int index, long startAtMs) {
        this.head = index;
        this.bucketStartMs = startAtMs;
        min[index] = 0.0f;
        max[index] = 0.0f;
        sum[index] = 0.0f;
        count[index] = 0;
        gcMs[index] = 0.0f;
        uploads[index] = 0;
        builds[index] = 0;
        startMs[index] = startAtMs;
        heapMb[index] = 0.0f;
        if (filled < BUCKETS) {
            filled++;
        }
    }

    private static short clamp(int value) {
        return (short) Math.max(0, Math.min(Short.MAX_VALUE, value));
    }
}
