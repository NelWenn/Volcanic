package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PresetSuggestionTest {
    private static final String P = "vulkanmod.options.performancePreset.";

    private static PresetSuggestion.Reading at(float medianMs, float lowMs, double target, String playing) {
        return new PresetSuggestion.Reading(1200, medianMs, lowMs, target, playing);
    }

    @Test
    void threeHundredAndSixtyFpsOnQualityPointsUpwardsNotDown() {
        String suggested = PresetSuggestion.suggest(at(2.8f, 4.0f, 120, P + "quality"));

        assertEquals(P + "ultra", suggested, "with that much headroom the only sane advice is to go up");
    }

    @Test
    void aStruggingMachineIsToldToStepDown() {
        assertEquals(P + "quality", PresetSuggestion.suggest(at(40.0f, 60.0f, 60, P + "ultra")));
        assertEquals(P + "performance", PresetSuggestion.suggest(at(45.0f, 70.0f, 60, P + "balanced")));
    }

    @Test
    void aComfortableMachineIsLeftAlone() {
        assertNull(PresetSuggestion.suggest(at(14.0f, 20.0f, 60, P + "quality")),
                "no suggestion is better than a pointless one");
    }

    @Test
    void unevenFramesBlockAnUpgradeEvenWhenTheAverageIsHigh() {
        assertNull(PresetSuggestion.suggest(at(3.0f, 30.0f, 60, P + "balanced")),
                "a high average with terrible lows must not read as headroom");
    }

    @Test
    void theSuggestionNeverWalksOffEitherEndOfTheLadder() {
        assertNull(PresetSuggestion.suggest(at(1.0f, 1.2f, 60, P + "ultra")));
        assertNull(PresetSuggestion.suggest(at(200.0f, 400.0f, 60, P + "performance")));
    }

    @Test
    void itJumpsTwoRungsOnlyWhenThereIsRoomForBoth() {
        assertEquals(P + "quality", PresetSuggestion.suggest(at(2.5f, 3.0f, 60, P + "performance")));
        assertEquals(P + "balanced", PresetSuggestion.suggest(at(7.0f, 9.0f, 60, P + "performance")));
    }

    @Test
    void withoutEnoughFramesItSaysNothing() {
        assertNull(PresetSuggestion.suggest(
                new PresetSuggestion.Reading(299, 2.8f, 4.0f, 120, P + "quality")));
    }

    @Test
    void aCustomConfigurationIsNotOnTheLadderSoNothingIsSuggested() {
        assertNull(PresetSuggestion.suggest(at(2.8f, 4.0f, 120, P + "custom")));
        assertNull(PresetSuggestion.suggest(at(2.8f, 4.0f, 120, null)));
        assertEquals(-1, PresetSuggestion.rung(P + "custom"));
    }

    @Test
    void anUncappedMachineFallsBackToASensibleTarget() {
        assertEquals(P + "ultra", PresetSuggestion.suggest(at(3.0f, 4.0f, 0, P + "quality")));
    }

    @Test
    void theLadderRunsFromPerformanceToUltra() {
        assertEquals(0, PresetSuggestion.rung(P + "performance"));
        assertEquals(1, PresetSuggestion.rung(P + "balanced"));
        assertEquals(2, PresetSuggestion.rung(P + "quality"));
        assertEquals(3, PresetSuggestion.rung(P + "ultra"));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> PresetSuggestion.suggest(null));
        assertThrows(IllegalArgumentException.class,
                () -> new PresetSuggestion.Reading(-1, 1, 1, 60, P + "quality"));
    }
}
