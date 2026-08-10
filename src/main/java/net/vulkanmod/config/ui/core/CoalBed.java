package net.vulkanmod.config.ui.core;

public final class CoalBed {
    public static final int CHUNKS = 76;
    public static final int VENTS = 24;
    public static final int BAND_H = 46;
    public static final int LAVA_H = 30;
    private static final int GLOW_H = 150;

    private static final int CHUNK_MIN_W = 7;
    private static final int CHUNK_MAX_W = 24;
    private static final int CHUNK_MIN_H = 5;
    private static final int CHUNK_MAX_H = 13;
    private static final int CAP_INSET = 2;

    private static final int VENT_MIN_W = 10;
    private static final int VENT_MAX_W = 42;
    private static final int VENT_MIN_H = 3;
    private static final int VENT_MAX_H = 7;

    private static final int ROCK_DARK = 0xFF1A1310;
    private static final int ROCK_LIGHT = 0xFF3A2B24;
    private static final int LAVA_HOT = 0xFFFF8A2A;
    private static final int LAVA_DEEP = 0xFFB63207;
    private static final int GLOW = 0x8E2A0C;
    private static final float GLOW_ALPHA = 0.20f;
    private static final float VENT_ALPHA = 0.85f;
    private static final float LAVA_ALPHA = 0.62f;

    private final long seed;
    private float clock;

    public CoalBed(long seed) {
        this.seed = seed;
    }

    public void advance(long deltaMs) {
        if (deltaMs < 0) {
            throw new IllegalArgumentException("deltaMs must not be negative: " + deltaMs);
        }
        this.clock += Math.min(deltaMs, 100L) / 1000.0f;
    }

    public int glowTop(Rect area) {
        return Math.max(area.y(), area.bottom() - GLOW_H);
    }

    public int glowArgb() {
        float beat = 0.85f + 0.15f * (float) Math.sin(clock * 0.33f);
        return Motion.fade(0xFF000000 | GLOW, GLOW_ALPHA * beat);
    }

    public Rect lavaBand(Rect area) {
        int height = Math.min(LAVA_H, area.height());
        return new Rect(area.x(), area.bottom() - height, area.width(), height);
    }

    public int lavaTopArgb() {
        return Motion.fade(LAVA_DEEP, 0.0f);
    }

    public int lavaBottomArgb() {
        float beat = 0.88f + 0.12f * (float) Math.sin(clock * 0.5f);
        return Motion.fade(LAVA_HOT, LAVA_ALPHA * beat);
    }

    public int ventWidth(int vent) {
        return ventHeight(vent) * 3 + Math.round(noise(vent, 21) * (VENT_MAX_W - VENT_MIN_W));
    }

    public int ventHeight(int vent) {
        return VENT_MIN_H + Math.round(noise(vent, 22) * (VENT_MAX_H - VENT_MIN_H));
    }

    public int ventX(int vent, Rect area) {
        return area.x() + Math.round(noise(vent, 23) * Math.max(1, area.width() - ventWidth(vent)));
    }

    public int ventY(int vent, Rect area) {
        int lift = Math.round(noise(vent, 24) * (LAVA_H - ventHeight(vent)));
        return area.bottom() - ventHeight(vent) - Math.max(0, lift);
    }

    public int ventArgb(int vent) {
        float beat = 0.55f + 0.45f * (float) Math.sin(clock * (0.4f + 0.7f * noise(vent, 25))
                + noise(vent, 26) * 6.2831855f);
        return Motion.fade(Motion.blend(LAVA_DEEP, LAVA_HOT, noise(vent, 27)), VENT_ALPHA * beat);
    }

    public int chunkWidth(int chunk) {
        return CHUNK_MIN_W + Math.round(noise(chunk, 1) * (CHUNK_MAX_W - CHUNK_MIN_W));
    }

    public int chunkHeight(int chunk) {
        return CHUNK_MIN_H + Math.round(noise(chunk, 2) * (CHUNK_MAX_H - CHUNK_MIN_H));
    }

    public int chunkX(int chunk, Rect area) {
        return area.x() + Math.round(noise(chunk, 3) * Math.max(1, area.width() - chunkWidth(chunk)));
    }

    public int chunkY(int chunk, Rect area) {
        float bias = noise(chunk, 4);
        int reach = Math.max(0, BAND_H - chunkHeight(chunk));
        return area.bottom() - chunkHeight(chunk) - Math.round(bias * bias * reach);
    }

    public int chunkArgb(int chunk) {
        return Motion.blend(ROCK_DARK, ROCK_LIGHT, noise(chunk, 5) * 0.8f);
    }

    public int capInset(int chunk) {
        return chunkWidth(chunk) > CHUNK_MIN_W + 4 ? CAP_INSET : 1;
    }

    public int capHeight(int chunk) {
        return Math.max(1, chunkHeight(chunk) / 3);
    }

    private float noise(int index, int salt) {
        long h = seed * 0x9E3779B97F4A7C15L + index * 0xC2B2AE3D27D4EB4FL + salt * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return ((h >>> 40) & 0xFFFFFF) / (float) 0x1000000;
    }
}
