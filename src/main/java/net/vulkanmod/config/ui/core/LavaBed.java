package net.vulkanmod.config.ui.core;

public final class LavaBed {
    public static final int CELL_W = 5;
    public static final int BAND_H = 18;
    private static final int GLOW_H = 96;
    private static final float MAX_ALPHA = 0.5f;
    private static final float GLOW_ALPHA = 0.16f;
    private static final int HOT = 0xFF7A2A;

    private final long seed;
    private float clock;

    public LavaBed(long seed) {
        this.seed = seed;
    }

    public void advance(long deltaMs) {
        if (deltaMs < 0) {
            throw new IllegalArgumentException("deltaMs must not be negative: " + deltaMs);
        }
        this.clock += Math.min(deltaMs, 100L) / 1000.0f;
    }

    public int cells(int width) {
        return width <= 0 ? 0 : (width + CELL_W - 1) / CELL_W;
    }

    public int heightOf(int cell) {
        return 3 + Math.round(noise(cell, 11) * (BAND_H - 3));
    }

    public int colorOf(int cell) {
        float base = 0.30f + 0.70f * noise(cell, 3);
        float beat = 0.62f + 0.38f * (float) Math.sin(clock * (0.5f + noise(cell, 7))
                + noise(cell, 5) * 6.2831855f);
        return Motion.fade(0xFF000000 | HOT, base * beat * MAX_ALPHA);
    }

    public int glowTop(Rect area) {
        return Math.max(area.y(), area.bottom() - GLOW_H);
    }

    public int glowArgb() {
        float beat = 0.85f + 0.15f * (float) Math.sin(clock * 0.4f);
        return Motion.fade(0xFF000000 | HOT, GLOW_ALPHA * beat);
    }

    private float noise(int cell, int salt) {
        long h = seed * 0x9E3779B97F4A7C15L + cell * 0xC2B2AE3D27D4EB4FL + salt * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return ((h >>> 40) & 0xFFFFFF) / (float) 0x1000000;
    }
}
