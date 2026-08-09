package net.vulkanmod.config.ui.core;

import java.util.Map;

public final class PresetRating {
    public static final int LEVELS = 5;

    public record Rating(int frames, int looks) {
        public Rating {
            if (frames < 0 || frames > LEVELS || looks < 0 || looks > LEVELS) {
                throw new IllegalArgumentException("ratings must be within 0.." + LEVELS);
            }
        }
    }

    private static final Map<String, Rating> BY_KEY = Map.of(
            "vulkanmod.options.performancePreset.performance", new Rating(5, 1),
            "vulkanmod.options.performancePreset.balanced", new Rating(4, 3),
            "vulkanmod.options.performancePreset.quality", new Rating(2, 4),
            "vulkanmod.options.performancePreset.ultra", new Rating(1, 5));

    private PresetRating() {
    }

    public static Rating of(String presetKey) {
        if (presetKey == null) {
            throw new IllegalArgumentException("presetKey must not be null");
        }
        return BY_KEY.get(presetKey);
    }

    public static boolean rated(String presetKey) {
        return of(presetKey) != null;
    }
}
