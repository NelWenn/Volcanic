package net.vulkanmod.config.ui.core;

public record DetailsItem(String text, ColorToken token, ImpactLevel bar, boolean accentBar, Glyph glyph) {
    public enum Glyph {
        NONE,
        CHECK,
        FLASK
    }

    private static final DetailsItem BLANK = new DetailsItem(null, null, null, false, Glyph.NONE);

    public static DetailsItem blank() {
        return BLANK;
    }

    public static DetailsItem line(String text, ColorToken token) {
        return line(text, token, Glyph.NONE);
    }

    public static DetailsItem line(String text, ColorToken token, Glyph glyph) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (token == null) {
            throw new IllegalArgumentException("token must not be null");
        }
        if (glyph == null) {
            throw new IllegalArgumentException("glyph must not be null");
        }
        return new DetailsItem(text, token, null, false, glyph);
    }

    public static DetailsItem bar(ImpactLevel level, boolean accent) {
        if (level == null) {
            throw new IllegalArgumentException("level must not be null");
        }
        return new DetailsItem(null, null, level, accent, Glyph.NONE);
    }

    public boolean isBlank() {
        return text == null && bar == null;
    }

    public boolean isBar() {
        return bar != null;
    }
}
