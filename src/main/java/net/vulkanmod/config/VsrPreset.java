package net.vulkanmod.config;

import net.vulkanmod.render.vsr.Vsr;

public enum VsrPreset {

    OFF(0, "vulkanmod.options.vsrPreset.off", 100, Vsr.BILINEAR, 0.0f),
    QUALITY(1, "vulkanmod.options.vsrPreset.quality", 77, Vsr.FSR1, 0.15f),
    BALANCED(2, "vulkanmod.options.vsrPreset.balanced", 67, Vsr.FSR1, 0.20f),
    PERFORMANCE(3, "vulkanmod.options.vsrPreset.performance", 50, Vsr.FSR1, 0.30f),
    TEMPORAL(5, "vulkanmod.options.vsrPreset.temporal", 67, Vsr.VTU, 0.30f),
    CUSTOM(4, "vulkanmod.options.vsrPreset.custom", 100, Vsr.FSR1, 0.20f);

    public final int id;
    public final String translationKey;
    public final int scale;
    public final int backend;
    public final float sharpness;

    VsrPreset(int id, String translationKey, int scale, int backend, float sharpness) {
        this.id = id;
        this.translationKey = translationKey;
        this.scale = scale;
        this.backend = backend;
        this.sharpness = sharpness;
    }

    public boolean isCustom() {
        return this == CUSTOM;
    }

    public void apply(Config config) {
        if (isCustom()) {
            return;
        }

        config.renderScale = RenderScale.clamp(this.scale);
        config.vsrBackend = this.backend;
        config.vsrSharpness = this.sharpness;
    }

    public static VsrPreset byId(int id) {
        for (VsrPreset preset : values()) {
            if (preset.id == id) {
                return preset;
            }
        }

        return CUSTOM;
    }

    public static VsrPreset current(Config config) {
        return byId(config.vsrPreset);
    }
}
