package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class PresetCardLayout {
    public static final int PAD_X = 14;
    public static final int TOP = 70;
    public static final int GAP = 6;
    public static final int CARD_HEIGHT = 62;
    public static final int CARD_HEIGHT_COMPACT = 50;
    public static final int TWO_COLUMN_AT = 560;
    public static final int LINE_PITCH = 12;
    public static final int CARD_PAD_X = 11;
    public static final int TAG_HEIGHT = 11;

    private PresetCardLayout() {
    }

    public static int columns(int usableWidth) {
        return usableWidth >= TWO_COLUMN_AT ? 2 : 1;
    }

    public static int cardHeight(Breakpoint breakpoint) {
        if (breakpoint == null) {
            throw new IllegalArgumentException("breakpoint must not be null");
        }
        return breakpoint == Breakpoint.COMPACT ? CARD_HEIGHT_COMPACT : CARD_HEIGHT;
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

        int columns = columns(usable);
        int height = cardHeight(breakpoint);
        int width = (usable - GAP * (columns - 1)) / columns;
        List<Rect> cards = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int column = index % columns;
            int row = index / columns;
            cards.add(new Rect(content.x() + PAD_X + column * (width + GAP),
                    content.y() + TOP + row * (height + GAP) - scroll, width, height));
        }
        return List.copyOf(cards);
    }

    public static Rect line(Rect card, int index) {
        if (card == null) {
            throw new IllegalArgumentException("card must not be null");
        }
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative: " + index);
        }
        int width = card.width() - CARD_PAD_X * 2;
        if (card.isEmpty() || width <= 0) {
            return Rect.EMPTY;
        }
        return new Rect(card.x() + CARD_PAD_X, card.y() + 8 + index * LINE_PITCH, width, 9);
    }

    public static int lineCapacity(Rect card) {
        if (card == null) {
            throw new IllegalArgumentException("card must not be null");
        }
        return Math.max(0, (card.height() - 8) / LINE_PITCH);
    }

    public static Rect tag(Rect card, int tagWidth) {
        if (card == null) {
            throw new IllegalArgumentException("card must not be null");
        }
        if (tagWidth < 0) {
            throw new IllegalArgumentException("tagWidth must not be negative: " + tagWidth);
        }
        Rect first = line(card, 0);
        if (first.isEmpty() || tagWidth == 0 || tagWidth > first.width()) {
            return Rect.EMPTY;
        }
        return new Rect(first.right() - tagWidth, first.y() - 1, tagWidth, TAG_HEIGHT);
    }

    public static int contentHeight(int count, Breakpoint breakpoint, int usableWidth) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
        if (count == 0) {
            return 0;
        }
        int columns = columns(usableWidth);
        int rows = (count + columns - 1) / columns;
        return TOP + rows * (cardHeight(breakpoint) + GAP) - GAP;
    }
}
