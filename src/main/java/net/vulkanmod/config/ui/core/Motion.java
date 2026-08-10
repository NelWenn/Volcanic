package net.vulkanmod.config.ui.core;

public final class Motion {
    public static final int HOVER_MS = 200;
    public static final int SELECTION_MS = 120;
    public static final int PAGE_MS = 210;
    public static final int REVEAL_MS = 300;
    public static final int STAGGER_MS = 20;
    public static final int STAGGER_CAP = 8;
    public static final int PAGE_TRAVEL = 14;
    public static final int ROW_TRAVEL = 7;
    public static final int ROWS_MS = 190;
    public static final int SEQUENCE_MS = REVEAL_MS + STAGGER_CAP * STAGGER_MS;

    private Motion() {
    }

    public static float ease(long elapsedMs, int durationMs) {
        float t = progress(elapsedMs, durationMs);
        return t * t * (3.0f - 2.0f * t);
    }

    public static float easeOut(long elapsedMs, int durationMs) {
        float inverse = 1.0f - progress(elapsedMs, durationMs);
        return 1.0f - inverse * inverse * inverse;
    }

    public static float progress(long elapsedMs, int durationMs) {
        if (durationMs <= 0 || elapsedMs >= durationMs) {
            return 1.0f;
        }
        if (elapsedMs <= 0) {
            return 0.0f;
        }
        return (float) elapsedMs / (float) durationMs;
    }

    public static float rowReveal(long elapsedMs, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative: " + index);
        }
        return easeOut(elapsedMs - (long) Math.min(index, STAGGER_CAP) * STAGGER_MS, REVEAL_MS);
    }

    public static int slide(float reveal, int direction, int travel) {
        return Math.round((1.0f - clamp(reveal)) * direction * travel);
    }

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * clamp(t);
    }

    public static int fade(int argb, float alpha) {
        int current = argb >>> 24;
        int faded = Math.round(current * clamp(alpha));
        return (argb & 0x00FFFFFF) | (Math.max(0, Math.min(255, faded)) << 24);
    }

    public static int blend(int fromArgb, int toArgb, float t) {
        float amount = clamp(t);
        return channel(fromArgb, toArgb, amount, 24) | channel(fromArgb, toArgb, amount, 16)
                | channel(fromArgb, toArgb, amount, 8) | channel(fromArgb, toArgb, amount, 0);
    }

    private static int channel(int fromArgb, int toArgb, float t, int shift) {
        int from = (fromArgb >>> shift) & 0xFF;
        int to = (toArgb >>> shift) & 0xFF;
        return Math.round(from + (to - from) * t) << shift;
    }

    private static float clamp(float t) {
        return t <= 0.0f ? 0.0f : t >= 1.0f ? 1.0f : t;
    }
}
