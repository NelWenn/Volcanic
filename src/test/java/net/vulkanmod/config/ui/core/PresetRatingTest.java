package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PresetRatingTest {
    private static final String P = "vulkanmod.options.performancePreset.";

    @Test
    void framesFallAndLooksRiseAcrossTheFourPresets() {
        int[] frames = {
                PresetRating.of(P + "performance").frames(),
                PresetRating.of(P + "balanced").frames(),
                PresetRating.of(P + "quality").frames(),
                PresetRating.of(P + "ultra").frames()};
        int[] looks = {
                PresetRating.of(P + "performance").looks(),
                PresetRating.of(P + "balanced").looks(),
                PresetRating.of(P + "quality").looks(),
                PresetRating.of(P + "ultra").looks()};

        for (int i = 1; i < frames.length; i++) {
            assertTrue(frames[i] < frames[i - 1], "frames must fall as the preset gets heavier");
            assertTrue(looks[i] > looks[i - 1], "looks must rise as the preset gets heavier");
        }
    }

    @Test
    void customCarriesNoRatingBecauseNobodyCanRateIt() {
        assertNull(PresetRating.of(P + "custom"));
        assertFalse(PresetRating.rated(P + "custom"));
        assertTrue(PresetRating.rated(P + "balanced"));
    }

    @Test
    void everyRatingStaysWithinTheScale() {
        for (String preset : new String[] {"performance", "balanced", "quality", "ultra"}) {
            PresetRating.Rating rating = PresetRating.of(P + preset);
            assertTrue(rating.frames() >= 1 && rating.frames() <= PresetRating.LEVELS);
            assertTrue(rating.looks() >= 1 && rating.looks() <= PresetRating.LEVELS);
        }
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> PresetRating.of(null));
        assertThrows(IllegalArgumentException.class, () -> new PresetRating.Rating(6, 1));
        assertThrows(IllegalArgumentException.class, () -> new PresetRating.Rating(1, -1));
    }
}
