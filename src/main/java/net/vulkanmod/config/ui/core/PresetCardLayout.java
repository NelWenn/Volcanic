package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class PresetCardLayout {
    public static final int PAD_X = 14;
    public static final int TOP = 20;
    public static final int OVERVIEW_MARGIN = 8;
    public static final int GAP = 6;
    public static final int CARD_HEIGHT = 150;
    public static final int CARD_HEIGHT_COMPACT = 122;
    public static final int CARD_PAD = 10;
    public static final int BANNER_HEIGHT = 40;
    public static final int LEGEND_H = 7;
    public static final int LEGEND_GAP = 10;
    public static final int SUGGEST_GAP = 10;
    public static final int MARGIN = 14;
    public static final int BANNER_TOP = 6;
    public static final int BOTTOM = 12;
    public static final int GLYPH = 18;
    public static final int STRIP_H = 13;
    public static final int SEG_H = 6;
    public static final int SEG_GAP = 2;
    public static final int SEGMENTS = PresetRating.LEVELS;
    public static final int ACCENT_WIDTH = 3;
    public static final int NAME_LINE = 9;
    public static final int SMALL_LINE = 7;
    public static final int BAR_ROW = 9;
    public static final int MAX_CARD = 108;
    public static final int THREE_WIDE_AT = 640;
    public static final int TWO_WIDE_AT = 380;
    private static final int MIN_CARD = 88;

    private PresetCardLayout() {
    }

    public static int[] rowPattern(int count, int usableWidth) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
        int perRow = perRow(usableWidth);
        if (count == 0) {
            return new int[0];
        }
        if (perRow == 3 && count == 5) {
            return new int[] {3, 2};
        }
        List<Integer> rows = new ArrayList<>();
        int left = count;
        while (left > 0) {
            int take = Math.min(perRow, left);
            rows.add(take);
            left -= take;
        }
        int[] pattern = new int[rows.size()];
        for (int i = 0; i < pattern.length; i++) {
            pattern[i] = rows.get(i);
        }
        return pattern;
    }

    public static int perRow(int usableWidth) {
        return Math.max(1, Math.min(5, (usableWidth + GAP) / (MIN_CARD + GAP)));
    }

    public static int cardHeight(Breakpoint breakpoint) {
        if (breakpoint == null) {
            throw new IllegalArgumentException("breakpoint must not be null");
        }
        return breakpoint == Breakpoint.COMPACT ? CARD_HEIGHT_COMPACT : CARD_HEIGHT;
    }

    public record Page(Rect legend, List<Rect> cards, Rect suggestion, int height, boolean centred) {
    }

    public static Page page(Rect content, int count, int scroll, Breakpoint breakpoint) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
        if (scroll < 0) {
            throw new IllegalArgumentException("scroll must not be negative: " + scroll);
        }
        int usable = content.width() - PAD_X * 2;
        if (content.isEmpty() || usable <= 0 || count == 0) {
            return new Page(Rect.EMPTY, List.of(), Rect.EMPTY, 0, false);
        }

        int grid = gridHeight(count, breakpoint, usable);
        int block = grid + SUGGEST_GAP + SMALL_LINE;
        int total = LEGEND_H + LEGEND_GAP + block;
        boolean centred = total + OVERVIEW_MARGIN * 2 <= content.height();

        int legendTop = centred
                ? content.y() + OVERVIEW_MARGIN
                : content.y() + OVERVIEW_MARGIN - scroll;
        Rect legend = new Rect(content.x() + PAD_X, legendTop, usable, LEGEND_H);

        int below = legend.bottom() + LEGEND_GAP;
        int room = content.bottom() - OVERVIEW_MARGIN - below;
        int gridTop = centred && room > block ? below + (room - block) / 2 : below;

        List<Rect> cards = grid(content, count, gridTop, breakpoint);
        Rect suggestion = new Rect(legend.x(), gridTop + grid + SUGGEST_GAP, usable, SMALL_LINE);
        return new Page(legend, cards, suggestion, total + OVERVIEW_MARGIN * 2, centred);
    }

    public static int gridHeight(int count, Breakpoint breakpoint, int usableWidth) {
        int rows = rowPattern(count, usableWidth).length;
        return rows == 0 ? 0 : rows * (cardHeight(breakpoint) + GAP) - GAP;
    }

    public static Rect banner(Rect content, int scroll) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        int usable = content.width() - PAD_X * 2;
        if (content.isEmpty() || usable <= 0) {
            return Rect.EMPTY;
        }
        return new Rect(content.x() + PAD_X, content.y() + BANNER_TOP - scroll, usable, BANNER_HEIGHT);
    }

    private static List<Rect> grid(Rect content, int count, int top, Breakpoint breakpoint) {
        int usable = content.width() - PAD_X * 2;
        int[] pattern = rowPattern(count, usable);
        int columns = perRow(usable);
        int width = Math.min(MAX_CARD, (usable - GAP * (columns - 1)) / columns);
        if (width < MIN_CARD && columns > 1) {
            columns = 1;
            width = Math.min(MAX_CARD, usable);
            pattern = rowPattern(count, 0);
        }
        int height = cardHeight(breakpoint);
        List<Rect> cards = new ArrayList<>(count);
        for (int row = 0; row < pattern.length; row++) {
            int inRow = pattern[row];
            int rowWidth = inRow * width + GAP * (inRow - 1);
            int left = content.x() + PAD_X + (usable - rowWidth) / 2;
            for (int column = 0; column < inRow; column++) {
                cards.add(new Rect(left + column * (width + GAP), top + row * (height + GAP), width, height));
            }
        }
        return List.copyOf(cards);
    }

    public static List<Rect> cards(Rect content, int count, int scroll, Breakpoint breakpoint) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
        if (scroll < 0) {
            throw new IllegalArgumentException("scroll must not be negative: " + scroll);
        }
        int usable = content.width() - PAD_X * 2;
        if (content.isEmpty() || usable <= 0) {
            return List.of();
        }

        int[] pattern = rowPattern(count, usable);
        int columns = perRow(usable);
        int width = Math.min(MAX_CARD, (usable - GAP * (columns - 1)) / columns);
        if (width < MIN_CARD && columns > 1) {
            columns = 1;
            width = Math.min(MAX_CARD, usable);
            pattern = rowPattern(count, 0);
        }

        int height = cardHeight(breakpoint);
        List<Rect> cards = new ArrayList<>(count);
        int top = content.y() + TOP - scroll;
        for (int row = 0; row < pattern.length; row++) {
            int inRow = pattern[row];
            int rowWidth = inRow * width + GAP * (inRow - 1);
            int left = content.x() + PAD_X + (usable - rowWidth) / 2;
            for (int column = 0; column < inRow; column++) {
                cards.add(new Rect(left + column * (width + GAP), top + row * (height + GAP), width, height));
            }
        }
        return List.copyOf(cards);
    }

    public static int contentHeight(int count, Breakpoint breakpoint, int usableWidth) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
        if (count == 0) {
            return TOP + BOTTOM;
        }
        int rows = rowPattern(count, usableWidth).length;
        return TOP + rows * (cardHeight(breakpoint) + GAP) - GAP + BOTTOM;
    }

    public record Slots(Rect accent, Rect glyph, Rect name, Rect tier, Rect rule, Rect blurb,
                        Rect framesLabel, Rect framesRow, Rect looksLabel, Rect looksRow, Rect strip) {
    }

    private static final Slots EMPTY_SLOTS = new Slots(Rect.EMPTY, Rect.EMPTY, Rect.EMPTY,
            Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY,
            Rect.EMPTY, Rect.EMPTY);

    public static Slots slots(Rect card, boolean roomy) {
        if (card == null) {
            throw new IllegalArgumentException("card must not be null");
        }
        int inner = card.width() - CARD_PAD * 2 - ACCENT_WIDTH;
        if (card.isEmpty() || inner <= 0) {
            return EMPTY_SLOTS;
        }
        int left = card.x() + ACCENT_WIDTH + CARD_PAD;
        Rect accent = new Rect(card.x(), card.y(), ACCENT_WIDTH, card.height());
        Rect strip = new Rect(card.x() + 1, card.bottom() - 1 - STRIP_H, card.width() - 2, STRIP_H);

        Rect looksRow = new Rect(left, strip.y() - 6 - SEG_H, inner, SEG_H);
        Rect looksLabel = new Rect(left, looksRow.y() - 1 - SMALL_LINE, inner, SMALL_LINE);
        Rect framesRow = new Rect(left, looksLabel.y() - 3 - SEG_H, inner, SEG_H);
        Rect framesLabel = new Rect(left, framesRow.y() - 1 - SMALL_LINE, inner, SMALL_LINE);

        Rect glyph = new Rect(left, card.y() + CARD_PAD, GLYPH, GLYPH);
        Rect name = new Rect(left, glyph.bottom() + 5, inner, NAME_LINE);
        Rect tier = new Rect(left, name.bottom() + 2, inner, SMALL_LINE);
        Rect rule = new Rect(left, tier.bottom() + 4, inner, 1);
        if (framesLabel.y() < rule.bottom() + 2) {
            return EMPTY_SLOTS;
        }

        int blurbTop = rule.bottom() + 5;
        int lines = Math.min(roomy ? 3 : 2, (framesLabel.y() - 3 - blurbTop) / SMALL_LINE);
        Rect blurb = lines > 0 ? new Rect(left, blurbTop, inner, lines * SMALL_LINE) : Rect.EMPTY;
        return new Slots(accent, glyph, name, tier, rule, blurb,
                framesLabel, framesRow, looksLabel, looksRow, strip);
    }

    public static Rect segment(Rect row, int index) {
        if (row == null) {
            throw new IllegalArgumentException("row must not be null");
        }
        if (index < 0 || index >= SEGMENTS) {
            throw new IllegalArgumentException("index must be within 0.." + (SEGMENTS - 1)
                    + ": " + index);
        }
        int width = (row.width() - SEG_GAP * (SEGMENTS - 1)) / SEGMENTS;
        if (row.isEmpty() || width <= 0) {
            return Rect.EMPTY;
        }
        return new Rect(row.x() + index * (width + SEG_GAP), row.y(), width, row.height());
    }
}
