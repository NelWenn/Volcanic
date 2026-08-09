package net.vulkanmod.config.ui.core;

import java.util.Locale;

public enum ImpactLevel {
    NONE(0.0f),
    LOW(0.25f),
    MEDIUM(0.6f),
    HIGH(0.85f);

    private final float fill;

    ImpactLevel(float fill) {
        this.fill = fill;
    }

    public float fill() {
        return fill;
    }

    public String labelKey() {
        return "vulkanmod.impact." + name().toLowerCase(Locale.ROOT);
    }
}
