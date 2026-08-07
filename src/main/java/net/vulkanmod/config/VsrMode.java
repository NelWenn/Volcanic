package net.vulkanmod.config;

public enum VsrMode {

    NATIVE(0, 100, "vulkanmod.options.vsr.native"),
    QUALITY(1, 77, "vulkanmod.options.vsr.quality"),
    BALANCED(2, 66, "vulkanmod.options.vsr.balanced"),
    PERFORMANCE(3, 50, "vulkanmod.options.vsr.performance");

    public final int id;
    public final int scale;
    public final String translationKey;

    VsrMode(int id, int scale, String translationKey) {
        this.id = id;
        this.scale = scale;
        this.translationKey = translationKey;
    }

    public boolean isUpscaled() {
        return this.scale < NATIVE.scale;
    }

    public static VsrMode byId(int id) {
        for (VsrMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }

        return NATIVE;
    }

    public static VsrMode byScale(int scale) {
        int clamped = RenderScale.clamp(scale);
        VsrMode nearest = NATIVE;
        int bestDistance = Integer.MAX_VALUE;

        for (VsrMode mode : values()) {
            int distance = Math.abs(mode.scale - clamped);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = mode;
            }
        }

        return nearest;
    }
}
