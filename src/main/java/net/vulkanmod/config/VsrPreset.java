package net.vulkanmod.config;

public enum VsrPreset {

    OFF(0, "vulkanmod.options.vsrPreset.off", 100, 0.0f),
    QUALITY(1, "vulkanmod.options.vsrPreset.quality", 77, 0.15f),
    BALANCED(2, "vulkanmod.options.vsrPreset.balanced", 67, 0.25f),
    PERFORMANCE(3, "vulkanmod.options.vsrPreset.performance", 50, 0.40f),
    CUSTOM(4, "vulkanmod.options.vsrPreset.custom", 100, 0.25f);

    public final int id;
    public final String translationKey;
    public final int scale;
    public final float sharpness;

    VsrPreset(int id, String translationKey, int scale, float sharpness) {
        this.id = id;
        this.translationKey = translationKey;
        this.scale = scale;
        this.sharpness = sharpness;
    }

    public boolean isCustom() {
        return this == CUSTOM;
    }

    public boolean isEnabled() {
        return this != OFF;
    }

    public void apply(Config config) {
        if (isCustom()) {
            return;
        }

        config.renderScale = RenderScale.clamp(this.scale);
        config.vsrSharpness = this.sharpness;
    }

    public static VsrPreset byId(int id) {
        for (VsrPreset preset : values()) {
            if (preset.id == id) {
                return preset;
            }
        }

        return OFF;
    }

    public static VsrPreset current(Config config) {
        return byId(config.vsrPreset);
    }
}
