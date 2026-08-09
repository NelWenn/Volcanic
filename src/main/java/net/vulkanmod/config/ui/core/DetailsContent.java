package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class DetailsContent {
    public static final String KEY_EMPTY = "vulkanmod.details.empty";
    public static final String KEY_PERFORMANCE = "vulkanmod.details.performance";
    public static final String KEY_VISUAL = "vulkanmod.details.visual";
    public static final String KEY_RECOMMENDED = "vulkanmod.details.recommended";
    public static final String KEY_RESTART = "vulkanmod.details.restart";
    public static final String KEY_EXPERIMENTAL = "vulkanmod.details.experimental";
    private static final String ELLIPSIS = "…";

    public interface Text {
        List<String> lines(String key, boolean uppercase);
    }

    private DetailsContent() {
    }

    public static List<DetailsItem> items(SettingMeta meta, String reasonKey, Text text,
                                          boolean spacers, int maxDescriptionLines, boolean bars) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (maxDescriptionLines < 0) {
            throw new IllegalArgumentException("maxDescriptionLines must not be negative: " + maxDescriptionLines);
        }
        List<DetailsItem> items = new ArrayList<>();
        if (meta == null) {
            append(items, text.lines(KEY_EMPTY, false), ColorToken.TEXT_FAINT);
            return List.copyOf(items);
        }

        append(items, text.lines(meta.titleKey(), true), ColorToken.TEXT_PRIMARY);
        block(items, truncate(text.lines(meta.descriptionKey(), false), maxDescriptionLines),
                ColorToken.TEXT_MUTED, spacers);
        block(items, text.lines(reasonKey, false), ColorToken.WARNING, spacers);
        impact(items, text, KEY_PERFORMANCE, meta.performance(), true, spacers, bars);
        impact(items, text, KEY_VISUAL, meta.visual(), false, spacers, bars);
        flags(items, meta, text, spacers);
        return List.copyOf(items);
    }

    public static List<DetailsItem> fit(SettingMeta meta, String reasonKey, Text text, int capacity) {
        if (capacity <= 0) {
            return List.of();
        }
        List<DetailsItem> full = items(meta, reasonKey, text, true, Integer.MAX_VALUE, true);
        if (full.size() <= capacity) {
            return full;
        }
        List<DetailsItem> tight = items(meta, reasonKey, text, false, Integer.MAX_VALUE, true);
        if (tight.size() <= capacity) {
            return tight;
        }
        int described = meta == null ? 0 : text.lines(meta.descriptionKey(), false).size();
        for (int max = described - 1; max >= 1; max--) {
            List<DetailsItem> shrunk = items(meta, reasonKey, text, false, max, true);
            if (shrunk.size() <= capacity) {
                return shrunk;
            }
        }
        List<DetailsItem> bare = items(meta, reasonKey, text, false, 0, true);
        if (bare.size() <= capacity) {
            return bare;
        }
        List<DetailsItem> flat = items(meta, reasonKey, text, false, 0, false);
        if (flat.size() <= capacity) {
            return flat;
        }
        return List.copyOf(flat.subList(0, capacity));
    }

    private static void impact(List<DetailsItem> items, Text text, String labelKey, ImpactLevel level,
                               boolean accent, boolean spacers, boolean bars) {
        if (level == null) {
            return;
        }
        spacer(items, spacers);
        append(items, text.lines(labelKey, false), ColorToken.TEXT_FAINT);
        boolean rated = level != ImpactLevel.NONE;
        if (rated && bars) {
            items.add(DetailsItem.bar(level, accent));
        }
        append(items, text.lines(level.labelKey(), true),
                accent && rated ? ColorToken.ACCENT : ColorToken.TEXT_SECONDARY);
    }

    private static void flags(List<DetailsItem> items, SettingMeta meta, Text text, boolean spacers) {
        boolean restart = meta.scope() == ApplyScope.RESTART;
        if (!meta.recommended() && !restart && !meta.experimental()) {
            return;
        }
        spacer(items, spacers);
        if (meta.recommended()) {
            append(items, text.lines(KEY_RECOMMENDED, false), ColorToken.SUCCESS, DetailsItem.Glyph.CHECK);
        }
        if (restart) {
            append(items, text.lines(KEY_RESTART, false), ColorToken.WARNING, DetailsItem.Glyph.NONE);
        }
        if (meta.experimental()) {
            append(items, text.lines(KEY_EXPERIMENTAL, false), ColorToken.WARNING, DetailsItem.Glyph.FLASK);
        }
    }

    private static void block(List<DetailsItem> items, List<String> lines, ColorToken token, boolean spacers) {
        if (lines.isEmpty()) {
            return;
        }
        spacer(items, spacers);
        append(items, lines, token);
    }

    private static void append(List<DetailsItem> items, List<String> lines, ColorToken token) {
        append(items, lines, token, DetailsItem.Glyph.NONE);
    }

    private static void append(List<DetailsItem> items, List<String> lines, ColorToken token,
                               DetailsItem.Glyph glyph) {
        for (int index = 0; index < lines.size(); index++) {
            items.add(DetailsItem.line(lines.get(index), token, index == 0 ? glyph : DetailsItem.Glyph.NONE));
        }
    }

    private static void spacer(List<DetailsItem> items, boolean spacers) {
        if (spacers && !items.isEmpty()) {
            items.add(DetailsItem.blank());
        }
    }

    private static List<String> truncate(List<String> lines, int max) {
        if (lines.size() <= max) {
            return lines;
        }
        if (max == 0) {
            return List.of();
        }
        List<String> kept = new ArrayList<>(lines.subList(0, max));
        kept.set(max - 1, kept.get(max - 1) + ELLIPSIS);
        return kept;
    }
}
