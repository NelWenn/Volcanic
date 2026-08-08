package net.vulkanmod.config.ui.core;

public final class FocusHandoff {
    private FocusHandoff() {
    }

    public static boolean enter(FocusModel focus, String regionId, String preferredId) {
        if (focus == null) {
            throw new IllegalArgumentException("focus must not be null");
        }
        if (regionId == null || regionId.isBlank()) {
            throw new IllegalArgumentException("region id must not be blank");
        }
        if (preferredId == null || preferredId.isBlank()) {
            throw new IllegalArgumentException("preferred focus id must not be blank");
        }
        if (!focus.focusRegion(regionId)) {
            throw new IllegalArgumentException("unknown region id " + regionId);
        }
        FocusRing ring = focus.ring(regionId);
        return ring.focused() != null || ring.focus(preferredId);
    }
}
