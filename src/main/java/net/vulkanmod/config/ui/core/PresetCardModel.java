package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PresetCardModel {

    public record Card(String key, boolean playing, boolean staged, boolean selectable,
                       boolean suggested, int differing) {
        public Card {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("key must not be blank");
            }
            if (differing < 0) {
                throw new IllegalArgumentException("differing must not be negative: " + differing);
            }
        }
    }

    private PresetCardModel() {
    }

    public static List<Card> cards(Map<String, Map<SettingId, Object>> profiles,
                                   Map<SettingId, Object> current, String customKey) {
        return cards(profiles, current, current, customKey, null);
    }

    public static List<Card> cards(Map<String, Map<SettingId, Object>> profiles,
                                   Map<SettingId, Object> live, Map<SettingId, Object> staged,
                                   String customKey, String suggestedKey) {
        if (profiles == null || profiles.isEmpty()) {
            throw new IllegalArgumentException("there must be at least one profile");
        }
        if (live == null || staged == null) {
            throw new IllegalArgumentException("live and staged values must not be null");
        }
        if (customKey == null || customKey.isBlank()) {
            throw new IllegalArgumentException("customKey must not be blank");
        }
        if (profiles.containsKey(customKey)) {
            throw new IllegalArgumentException("the custom profile must not be selectable: " + customKey);
        }

        List<Card> cards = new ArrayList<>(profiles.size() + 1);
        boolean anyPlaying = false;
        boolean anyStaged = false;
        for (Map.Entry<String, Map<SettingId, Object>> profile : profiles.entrySet()) {
            int differing = differing(profile.getValue(), staged);
            boolean playing = differing(profile.getValue(), live) == 0;
            boolean stagedHere = differing == 0 && !playing;
            anyPlaying |= playing;
            anyStaged |= stagedHere;
            cards.add(new Card(profile.getKey(), playing, stagedHere, true,
                    profile.getKey().equals(suggestedKey) && !playing, differing));
        }
        if (!anyPlaying && !anyStaged) {
            cards.add(new Card(customKey, true, false, false, false, nearest(profiles, staged)));
        }
        return List.copyOf(cards);
    }

    public static int differing(Map<SettingId, Object> profile, Map<SettingId, Object> current) {
        if (profile == null) {
            throw new IllegalArgumentException("profile must not be null");
        }
        if (current == null) {
            throw new IllegalArgumentException("current must not be null");
        }
        int count = 0;
        for (Map.Entry<SettingId, Object> entry : profile.entrySet()) {
            if (!Objects.equals(entry.getValue(), current.get(entry.getKey()))) {
                count++;
            }
        }
        return count;
    }

    private static int nearest(Map<String, Map<SettingId, Object>> profiles, Map<SettingId, Object> current) {
        int best = Integer.MAX_VALUE;
        for (Map<SettingId, Object> profile : profiles.values()) {
            best = Math.min(best, differing(profile, current));
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    public static boolean customTail(java.util.List<Card> cards) {
        return cards != null && !cards.isEmpty()
                && cards.get(cards.size() - 1).key().endsWith(".custom");
    }
}
