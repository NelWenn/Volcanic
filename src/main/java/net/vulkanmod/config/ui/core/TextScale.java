package net.vulkanmod.config.ui.core;

public final class TextScale {
    public static final int GLYPH_HEIGHT = 8;
    public static final int LINE_HEIGHT = 9;

    private TextScale() {
    }

    public static float small(double guiScale) {
        if (guiScale <= 0.0) {
            throw new IllegalArgumentException("guiScale must be positive: " + guiScale);
        }
        int whole = (int) Math.floor(guiScale);
        if (whole <= 1) {
            return 1.0f;
        }
        return (float) ((whole - 1) / guiScale);
    }

    public static int physicalPixels(double guiScale, float textScale) {
        if (guiScale <= 0.0) {
            throw new IllegalArgumentException("guiScale must be positive: " + guiScale);
        }
        if (textScale <= 0.0f) {
            throw new IllegalArgumentException("textScale must be positive: " + textScale);
        }
        return (int) Math.round(guiScale * textScale);
    }

    public static boolean isCrisp(double guiScale, float textScale) {
        double physical = guiScale * textScale;
        return Math.abs(physical - Math.round(physical)) < 1.0e-4 && Math.round(physical) >= 1;
    }

    public static int lineHeight(float textScale) {
        if (textScale <= 0.0f) {
            throw new IllegalArgumentException("textScale must be positive: " + textScale);
        }
        return Math.max(1, Math.round(LINE_HEIGHT * textScale));
    }

    public static int scaled(int value, float textScale) {
        if (textScale <= 0.0f) {
            throw new IllegalArgumentException("textScale must be positive: " + textScale);
        }
        return Math.round(value * textScale);
    }
}
