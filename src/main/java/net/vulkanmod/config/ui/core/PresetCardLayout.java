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
    public static final int BAR_HEIGHT = 3;
    public static final int BAR_LABEL_WIDTH = 34;
    public static final int BAR_VALUE_WIDTH = 42;
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

    public record Slots(Rect accent, Rect name, Rect badge, Rect blurb, Rect changes,
                        Rect framesBar, Rect looksBar, Rect measured) {
    }

    public static Slots slots(Rect card, boolean roomy) {
        if (card == null) {
            throw new IllegalArgumentException("card must not be null");
        }
        int inner = card.width() - CARD_PAD * 2 - ACCENT_WIDTH;
        if (card.isEmpty() || inner <= 0) {
            return new Slots(Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY,
                    Rect.EMPTY, Rect.EMPTY, Rect.EMPTY);
        }
        int left = card.x() + ACCENT_WIDTH + CARD_PAD;
        Rect accent = new Rect(card.x(), card.y(), ACCENT_WIDTH, card.height());

        Rect measured = new Rect(left, card.bottom() - CARD_PAD - SMALL_LINE, inner, SMALL_LINE);
        int barsTop = measured.y() - GAP - BAR_ROW * 2;
        int trackWidth = Math.max(0, inner - BAR_LABEL_WIDTH - BAR_VALUE_WIDTH);
        Rect frames = new Rect(left + BAR_LABEL_WIDTH, barsTop + 3, trackWidth, BAR_HEIGHT);
        Rect looks = new Rect(frames.x(), barsTop + BAR_ROW + 3, trackWidth, BAR_HEIGHT);

        int top = card.y() + CARD_PAD;
        Rect name = new Rect(left, top, inner, NAME_LINE);
        Rect badge = new Rect(left, top + 1, inner, SMALL_LINE);

        int textTop = top + NAME_LINE + 3;
        int available = barsTop - textTop;
        int blurbLines = available >= SMALL_LINE * 3 + 2 ? 2 : 1;
        Rect blurb = new Rect(left, textTop, inner, SMALL_LINE * blurbLines);
        Rect changes = roomy && blurb.bottom() + 2 + SMALL_LINE <= barsTop
                ? new Rect(left, blurb.bottom() + 2, inner, SMALL_LINE)
                : Rect.EMPTY;
        if (blurb.bottom() > barsTop) {
            blurb = Rect.EMPTY;
        }
        return new Slots(accent, name, badge, blurb, changes, frames, looks, measured);
    }

    public static Rect bar(Rect card, int index, int fromBottom) {
        if (card == null) {
            throw new IllegalArgumentException("card must not be null");
        }
        if (index < 0 || fromBottom < 0) {
            throw new IllegalArgumentException("index and fromBottom must not be negative");
        }
        int width = card.width() - CARD_PAD * 2 - BAR_LABEL_WIDTH - BAR_VALUE_WIDTH;
        if (card.isEmpty() || width <= 0) {
            return Rect.EMPTY;
        }
        int baseline = card.bottom() - CARD_PAD - fromBottom;
        return new Rect(card.x() + CARD_PAD + BAR_LABEL_WIDTH, baseline + index * 9, width, BAR_HEIGHT);
    }

    public static Rect barFill(Rect track, int level, int levels) {
        if (track == null) {
            throw new IllegalArgumentException("track must not be null");
        }
        if (levels <= 0) {
            throw new IllegalArgumentException("levels must be positive: " + levels);
        }
        int clamped = Math.max(0, Math.min(levels, level));
        int width = Math.round(track.width() * (float) clamped / levels);
        if (width <= 0 || track.isEmpty()) {
            return Rect.EMPTY;
        }
        return new Rect(track.x(), track.y(), width, track.height());
    }
}
