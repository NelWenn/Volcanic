package net.vulkanmod.config.ui.core;

public final class LavaBed {
    public static final int PATCHES = 30;
    public static final int BAND_H = 14;
    private static final int GLOW_H = 110;
    private static final int MIN_W = 9;
    private static final int MAX_W = 38;
    private static final int MIN_H = 2;
    private static final int MAX_H = 5;
    private static final float MAX_ALPHA = 0.30f;
    private static final float GLOW_ALPHA = 0.15f;
    private static final int HOT = 0xFF8A3A;
    private static final int COOL = 0x8E2A0C;

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

    public int widthOf(int patch) {
        return MIN_W + Math.round(noise(patch, 2) * (MAX_W - MIN_W));
    }

    public int heightOf(int patch) {
        return MIN_H + Math.round(noise(patch, 3) * (MAX_H - MIN_H));
    }

    public int xOf(int patch, Rect area) {
        int span = Math.max(1, area.width() - widthOf(patch));
        return area.x() + Math.round(noise(patch, 1) * span);
    }

    public int yOf(int patch, Rect area) {
        int lift = Math.round(noise(patch, 4) * (BAND_H - heightOf(patch)));
        return area.bottom() - heightOf(patch) - Math.max(0, lift);
    }

    public int colorOf(int patch) {
        float base = 0.22f + 0.78f * noise(patch, 5);
        float beat = 0.55f + 0.45f * (float) Math.sin(clock * (0.30f + 0.55f * noise(patch, 7))
                + noise(patch, 6) * 6.2831855f);
        int argb = Motion.blend(0xFF000000 | COOL, 0xFF000000 | HOT, noise(patch, 8));
        return Motion.fade(argb, base * beat * MAX_ALPHA);
    }

    public int glowTop(Rect area) {
        return Math.max(area.y(), area.bottom() - GLOW_H);
    }

    public int glowArgb() {
        float beat = 0.86f + 0.14f * (float) Math.sin(clock * 0.35f);
        return Motion.fade(0xFF000000 | COOL, GLOW_ALPHA * beat);
    }

    private float noise(int patch, int salt) {
        long h = seed * 0x9E3779B97F4A7C15L + patch * 0xC2B2AE3D27D4EB4FL + salt * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return ((h >>> 40) & 0xFFFFFF) / (float) 0x1000000;
    }
}
