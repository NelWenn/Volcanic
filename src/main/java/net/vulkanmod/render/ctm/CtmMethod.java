package net.vulkanmod.render.ctm;

public enum CtmMethod {
    FIXED,
    RANDOM,
    REPEAT,
    OVERLAY_FIXED,
    OVERLAY_RANDOM,
    UNSUPPORTED;

    public static CtmMethod fromString(String s) {
        if (s == null) return UNSUPPORTED;
        return switch (s.trim().toLowerCase()) {
            case "fixed" -> FIXED;
            case "random" -> RANDOM;
            case "repeat" -> REPEAT;
            case "overlay_fixed" -> OVERLAY_FIXED;
            case "overlay_random" -> OVERLAY_RANDOM;
            default -> UNSUPPORTED;
        };
    }

    public boolean isOverlay() {
        return this == OVERLAY_FIXED || this == OVERLAY_RANDOM;
    }
}
