package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileMatcherTest {
    private static final SettingId A = SettingId.parse("vulkanmod:a");
    private static final SettingId B = SettingId.parse("vulkanmod:b");

    private static Map<String, Map<SettingId, Object>> profiles() {
        Map<String, Map<SettingId, Object>> profiles = new LinkedHashMap<>();
        profiles.put("balanced", Map.of(A, 5, B, true));
        profiles.put("quality", Map.of(A, 16, B, true));
        return profiles;
    }

    @Test
    void currentValuesMatchingAProfileExactlyReportThatProfile() {
        assertEquals(Optional.of("balanced"), ProfileMatcher.match(profiles(), Map.of(A, 5, B, true)));
        assertEquals(Optional.of("quality"), ProfileMatcher.match(profiles(), Map.of(A, 16, B, true)));
    }

    @Test
    void oneValueOffMeansNoProfileMatches() {
        assertEquals(Optional.empty(), ProfileMatcher.match(profiles(), Map.of(A, 5, B, false)));
    }

    @Test
    void restoringEveryValueByHandMatchesAgain() {
        Map<SettingId, Object> current = new LinkedHashMap<>(Map.of(A, 5, B, true));
        current.put(A, 9);
        assertEquals(Optional.empty(), ProfileMatcher.match(profiles(), current));
        current.put(A, 5);
        assertEquals(Optional.of("balanced"), ProfileMatcher.match(profiles(), current));
    }

    @Test
    void aValueMissingFromTheCurrentSetCannotMatch() {
        assertEquals(Optional.empty(), ProfileMatcher.match(profiles(), Map.of(A, 5)));
    }

    @Test
    void currentValuesBeyondAProfilesGovernedSetDoNotPreventAMatch() {
        Map<SettingId, Object> current = new LinkedHashMap<>(Map.of(A, 5, B, true));
        current.put(SettingId.parse("vulkanmod:unrelated"), 42);
        assertEquals(Optional.of("balanced"), ProfileMatcher.match(profiles(), current));
    }

    @Test
    void equalityIsByValueNotByIdentity() {
        Map<SettingId, Object> current = new LinkedHashMap<>();
        current.put(A, Integer.valueOf(5000));
        current.put(B, Boolean.valueOf(true));
        Map<String, Map<SettingId, Object>> profiles = new LinkedHashMap<>();
        profiles.put("wide", Map.of(A, Integer.valueOf(5000), B, Boolean.valueOf(true)));
        assertEquals(Optional.of("wide"), ProfileMatcher.match(profiles, current));
    }

    @Test
    void theFirstProfileWinsWhenTwoDeclareTheSameValues() {
        Map<String, Map<SettingId, Object>> profiles = new LinkedHashMap<>();
        profiles.put("first", Map.of(A, 1));
        profiles.put("second", Map.of(A, 1));
        assertEquals(Optional.of("first"), ProfileMatcher.match(profiles, Map.of(A, 1)));
    }

    @Test
    void rejectsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> ProfileMatcher.match(null, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> ProfileMatcher.match(profiles(), null));
    }

    @Test
    void rejectsAProfileWithoutValues() {
        Map<String, Map<SettingId, Object>> nullValues = new LinkedHashMap<>();
        nullValues.put("broken", null);
        assertThrows(IllegalArgumentException.class, () -> ProfileMatcher.match(nullValues, Map.of(A, 5)));

        Map<String, Map<SettingId, Object>> empty = new LinkedHashMap<>();
        empty.put("empty", Map.of());
        assertThrows(IllegalArgumentException.class, () -> ProfileMatcher.match(empty, Map.of(A, 5)));
    }
}
