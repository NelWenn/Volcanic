package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PresetCardModelTest {
    private static final SettingId A = SettingId.parse("vulkanmod:a.one");
    private static final SettingId B = SettingId.parse("vulkanmod:a.two");
    private static final String CUSTOM = "preset.custom";

    private static Map<SettingId, Object> values(Object a, Object b) {
        Map<SettingId, Object> map = new LinkedHashMap<>();
        map.put(A, a);
        map.put(B, b);
        return map;
    }

    private static Map<String, Map<SettingId, Object>> profiles() {
        Map<String, Map<SettingId, Object>> profiles = new LinkedHashMap<>();
        profiles.put("preset.fast", values(1, false));
        profiles.put("preset.balanced", values(2, true));
        return profiles;
    }

    @Test
    void theMatchingProfileIsTheOnePlayingAndNoCustomCardAppears() {
        List<PresetCardModel.Card> cards =
                PresetCardModel.cards(profiles(), values(2, true), CUSTOM);

        assertEquals(2, cards.size());
        assertFalse(cards.get(0).playing());
        assertTrue(cards.get(1).playing());
        assertEquals(0, cards.get(1).differing());
    }

    @Test
    void stagingAProfileDoesNotMoveWhatIsPlaying() {
        List<PresetCardModel.Card> cards = PresetCardModel.cards(profiles(),
                values(2, true), values(1, false), CUSTOM, null);

        PresetCardModel.Card fast = cards.get(0);
        PresetCardModel.Card balanced = cards.get(1);

        assertTrue(fast.staged(), "the clicked profile is staged");
        assertFalse(fast.playing(), "but it is not what the game is running");
        assertTrue(balanced.playing(), "the running profile keeps its badge and its figure");
        assertFalse(balanced.staged());
    }

    @Test
    void aProfileCannotBeBothPlayingAndStaged() {
        for (PresetCardModel.Card card : PresetCardModel.cards(profiles(), values(2, true), CUSTOM)) {
            assertFalse(card.playing() && card.staged());
        }
    }

    @Test
    void theSuggestionNeverLandsOnWhatIsAlreadyPlaying() {
        List<PresetCardModel.Card> cards = PresetCardModel.cards(profiles(),
                values(2, true), values(2, true), CUSTOM, "preset.balanced");

        assertFalse(cards.get(1).suggested(), "suggesting what you already run is noise");
    }

    @Test
    void everyCardCountsHowFarItIsFromWhereYouAre() {
        List<PresetCardModel.Card> cards =
                PresetCardModel.cards(profiles(), values(2, true), CUSTOM);

        assertEquals(2, cards.get(0).differing(), "fast differs on both settings");
        assertEquals(0, cards.get(1).differing());
    }

    @Test
    void aConfigurationMatchingNoProfileGrowsAnUnselectableCustomCard() {
        List<PresetCardModel.Card> cards =
                PresetCardModel.cards(profiles(), values(9, true), CUSTOM);

        assertEquals(3, cards.size());
        PresetCardModel.Card custom = cards.get(2);
        assertEquals(CUSTOM, custom.key());
        assertTrue(custom.playing());
        assertFalse(custom.selectable(), "custom is a statement of fact, not an option");
        assertEquals(1, custom.differing(), "it reports the distance to the nearest profile");
    }

    @Test
    void aSettingAbsentFromTheCurrentMapCountsAsDifferent() {
        assertEquals(2, PresetCardModel.differing(values(1, false), Map.of()));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class,
                () -> PresetCardModel.cards(Map.of(), values(1, true), CUSTOM));
        assertThrows(IllegalArgumentException.class,
                () -> PresetCardModel.cards(profiles(), null, CUSTOM));
        assertThrows(IllegalArgumentException.class,
                () -> PresetCardModel.cards(profiles(), values(1, true), " "));
        Map<String, Map<SettingId, Object>> withCustom = profiles();
        withCustom.put(CUSTOM, values(1, true));
        assertThrows(IllegalArgumentException.class,
                () -> PresetCardModel.cards(withCustom, values(1, true), CUSTOM));
    }
}
