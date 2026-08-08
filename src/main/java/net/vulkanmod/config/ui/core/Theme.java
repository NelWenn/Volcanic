package net.vulkanmod.config.ui.core;

import java.util.EnumMap;
import java.util.Map;

public final class Theme {
    private static final Theme VOLCANIC = buildVolcanic();

    private final Map<ColorToken, Integer> colors;

    private Theme(Map<ColorToken, Integer> colors) {
        this.colors = colors;
    }

    public static Theme volcanic() {
        return VOLCANIC;
    }

    public int color(ColorToken token) {
        Integer value = colors.get(token);
        if (value == null) {
            throw new IllegalStateException("no colour mapped for token " + token);
        }
        return value;
    }

    public int color(ColorToken token, float alpha) {
        float clamped = Math.min(1.0f, Math.max(0.0f, alpha));
        int alphaBits = (int)(clamped * 255.0f);
        return (alphaBits << 24) | (color(token) & 0xFFFFFF);
    }

    public Gradient gradient(ColorToken top, ColorToken bottom) {
        return new Gradient(color(top), color(bottom));
    }

    private static Theme buildVolcanic() {
        Map<ColorToken, Integer> colors = new EnumMap<>(ColorToken.class);
        colors.put(ColorToken.SURFACE_BASE, 0xFF0E0A09);
        colors.put(ColorToken.SURFACE_CHROME, 0xFF100B0A);
        colors.put(ColorToken.SURFACE_CARD, 0xFF171110);
        colors.put(ColorToken.SURFACE_CARD_HOVER, 0xFF1C1412);
        colors.put(ColorToken.SURFACE_SUNKEN, 0xFF0B0807);
        colors.put(ColorToken.SURFACE_NAV_ACTIVE, 0xFF221512);
        colors.put(ColorToken.SURFACE_SIDEBAR_BOTTOM, 0xFF150E0C);
        colors.put(ColorToken.BORDER_SUBTLE, 0xFF1E1413);
        colors.put(ColorToken.BORDER_DEFAULT, 0xFF2A1D1A);
        colors.put(ColorToken.BORDER_STRONG, 0xFF3A231D);
        colors.put(ColorToken.BORDER_ACCENT, 0xFF5A3025);
        colors.put(ColorToken.TEXT_PRIMARY, 0xFFF4ECE8);
        colors.put(ColorToken.TEXT_DEFAULT, 0xFFE9DDD7);
        colors.put(ColorToken.TEXT_SECONDARY, 0xFFA3918A);
        colors.put(ColorToken.TEXT_MUTED, 0xFF8A7770);
        colors.put(ColorToken.TEXT_FAINT, 0xFF6B544C);
        colors.put(ColorToken.ACCENT, 0xFFFF5A1F);
        colors.put(ColorToken.ACCENT_DEEP, 0xFFE0450F);
        colors.put(ColorToken.ACCENT_BRIGHT, 0xFFFF7A3C);
        colors.put(ColorToken.SUCCESS, 0xFF8FBC76);
        colors.put(ColorToken.SUCCESS_BG, 0xFF1C2A18);
        colors.put(ColorToken.WARNING, 0xFFE0A03A);
        return new Theme(colors);
    }
}
