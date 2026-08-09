package net.vulkanmod.config.ui.core;

public final class Motion {
    public static final int HOVER_MS = 200;
    public static final int SELECTION_MS = 120;

    private Motion() {
    }

    public static float ease(long elapsedMs, int durationMs) {
        if (durationMs <= 0 || elapsedMs >= durationMs) {
            return 1.0f;
        }
        if (elapsedMs <= 0) {
            return 0.0f;
        }
        float t = (float) elapsedMs / (float) durationMs;
        return t * t * (3.0f - 2.0f * t);
    }
}
