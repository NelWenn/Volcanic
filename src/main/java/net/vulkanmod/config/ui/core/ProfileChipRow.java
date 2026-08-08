package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ProfileChipRow {

    public record Chip(String key, boolean active, boolean selectable) {
        public Chip {
            requireKey(key, "key");
        }
    }

    private ProfileChipRow() {
    }

    public static List<Chip> chips(List<String> profiles, String customKey, Optional<String> match) {
        if (profiles == null) {
            throw new IllegalArgumentException("profiles must not be null");
        }
        if (profiles.isEmpty()) {
            throw new IllegalArgumentException("there must be at least one selectable profile");
        }
        requireKey(customKey, "customKey");
        if (match == null) {
            throw new IllegalArgumentException("match must not be null");
        }
        if (profiles.contains(customKey)) {
            throw new IllegalArgumentException("the custom profile must not be selectable: " + customKey);
        }
        if (match.isPresent() && !profiles.contains(match.get())) {
            throw new IllegalArgumentException("matched profile is not on the row: " + match.get());
        }

        List<Chip> chips = new ArrayList<>(profiles.size() + 1);
        for (String profile : profiles) {
            requireKey(profile, "profile");
            chips.add(new Chip(profile, match.isPresent() && match.get().equals(profile), true));
        }
        chips.add(new Chip(customKey, match.isEmpty(), false));
        return List.copyOf(chips);
    }

    public static List<Rect> boxes(Rect row, int[] textWidths) {
        if (row == null) {
            throw new IllegalArgumentException("row must not be null");
        }
        if (textWidths == null) {
            throw new IllegalArgumentException("textWidths must not be null");
        }
        if (row.isEmpty()) {
            return List.of();
        }
        int top = row.y() + Math.max(0, (row.height() - TabStripModel.HEIGHT) / 2);
        return TabStripModel.layout(textWidths, row.x(), top);
    }

    private static void requireKey(String key, String name) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
