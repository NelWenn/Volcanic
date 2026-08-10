package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class SettingRowLayout {
    public static final int CARD_RADIUS = 8;
    public static final int CARD_PAD_X = 12;
    public static final int RESET_SIZE = 15;
    public static final int RESET_RADIUS = 4;
    public static final int RESET_GAP = 5;

    public static final int ARROW_SIZE = 15;
    public static final int ARROW_RADIUS = 4;
    private static final int CYCLER_MIN_VALUE = 40;
    private static final int CYCLER_TITLE_RESERVE = 104;

    public static final int BADGE_SIZE = 7;
    public static final int BADGE_GAP = 5;
    public static final int BADGE_ADVANCE = BADGE_SIZE + BADGE_GAP;

    private static final int STAR_SIZE = 15;
    private static final int STAR_GAP = 4;
    private static final int GUTTER = RESET_GAP + STAR_SIZE + STAR_GAP + RESET_SIZE;

    private static final int ROW_HEIGHT = 27;
    private static final int ROW_HEIGHT_COMPACT = 22;
    private static final int ROW_GAP = 5;
    private static final int ROW_GAP_COMPACT = 3;
    private static final int PAD_X = 14;
    private static final int BOTTOM = 12;

    private SettingRowLayout() {
    }

    public static int rowHeight(Breakpoint breakpoint) {
        return requireBreakpoint(breakpoint) == Breakpoint.COMPACT ? ROW_HEIGHT_COMPACT : ROW_HEIGHT;
    }

    public static int rowGap(Breakpoint breakpoint) {
        return requireBreakpoint(breakpoint) == Breakpoint.COMPACT ? ROW_GAP_COMPACT : ROW_GAP;
    }

    public static List<Rect> rows(Rect content, int count, int scroll, Breakpoint breakpoint) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
        if (scroll < 0) {
            throw new IllegalArgumentException("scroll must not be negative: " + scroll);
        }
        requireBreakpoint(breakpoint);

        int width = content.width() - PAD_X * 2;
        if (content.isEmpty() || width <= 0) {
            return List.of();
        }

        int height = rowHeight(breakpoint);
        int pitch = height + rowGap(breakpoint);
        List<Rect> rows = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            rows.add(new Rect(content.x() + PAD_X,
                    content.y() + index * pitch - scroll, width, height));
        }
        return List.copyOf(rows);
    }

    public static final int GROUP_CHEVRON = 7;
    public static final int GROUP_COUNT_W = 24;

    public static Rect groupChevron(Rect row) {
        return row.isEmpty() ? Rect.EMPTY
                : new Rect(row.x(), row.y() + (row.height() - GROUP_CHEVRON) / 2,
                        GROUP_CHEVRON, GROUP_CHEVRON);
    }

    public static Rect groupLabel(Rect row) {
        int left = row.x() + GROUP_CHEVRON + 5;
        int width = row.right() - GROUP_COUNT_W - 6 - left;
        return row.isEmpty() || width <= 0 ? Rect.EMPTY
                : new Rect(left, row.y() + (row.height() - 9) / 2, width, 9);
    }

    public static Rect groupCount(Rect row) {
        return row.isEmpty() ? Rect.EMPTY
                : new Rect(row.right() - GROUP_COUNT_W, row.y() + (row.height() - 7) / 2,
                        GROUP_COUNT_W, 7);
    }

    public static Rect groupRule(Rect row) {
        return row.isEmpty() ? Rect.EMPTY : new Rect(row.x(), row.bottom() - 1, row.width(), 1);
    }

    public static Rect cardBox(Rect row) {
        if (row == null) {
            throw new IllegalArgumentException("row must not be null");
        }
        int width = row.width() - GUTTER;
        if (row.isEmpty() || width <= 0) {
            return Rect.EMPTY;
        }
        return new Rect(row.x(), row.y(), width, row.height());
    }

    public static Rect resetBox(Rect row) {
        if (row == null) {
            throw new IllegalArgumentException("row must not be null");
        }
        if (cardBox(row).isEmpty() || row.height() < RESET_SIZE) {
            return Rect.EMPTY;
        }
        return new Rect(row.right() - RESET_SIZE, row.y() + (row.height() - RESET_SIZE) / 2,
                RESET_SIZE, RESET_SIZE);
    }

    public static Rect starBox(Rect row) {
        if (row == null) {
            throw new IllegalArgumentException("row must not be null");
        }
        if (cardBox(row).isEmpty() || row.height() < STAR_SIZE) {
            return Rect.EMPTY;
        }
        return new Rect(row.right() - RESET_SIZE - STAR_GAP - STAR_SIZE,
                row.y() + (row.height() - STAR_SIZE) / 2, STAR_SIZE, STAR_SIZE);
    }

    public static Rect cyclerPrevBox(Rect row) {
        return cyclerPart(row, 0);
    }

    public static Rect cyclerValueBox(Rect row) {
        return cyclerPart(row, 1);
    }

    public static Rect cyclerNextBox(Rect row) {
        return cyclerPart(row, 2);
    }

    public static Rect cyclerBox(Rect row) {
        if (row == null) {
            throw new IllegalArgumentException("row must not be null");
        }
        Rect card = cardBox(row);
        int inner = card.width() - CARD_PAD_X * 2;
        if (card.isEmpty() || row.height() < ARROW_SIZE || inner <= 0) {
            return Rect.EMPTY;
        }
        int width = Math.min(Math.max(inner / 2, ARROW_SIZE * 2 + CYCLER_MIN_VALUE),
                inner - CYCLER_TITLE_RESERVE);
        if (width < ARROW_SIZE * 2 + CYCLER_MIN_VALUE) {
            return Rect.EMPTY;
        }
        return new Rect(card.right() - CARD_PAD_X - width, row.y(), width, row.height());
    }

    private static Rect cyclerPart(Rect row, int part) {
        Rect region = cyclerBox(row);
        if (region.isEmpty()) {
            return Rect.EMPTY;
        }
        int top = region.y() + (region.height() - ARROW_SIZE) / 2;
        return switch (part) {
            case 0 -> new Rect(region.x(), top, ARROW_SIZE, ARROW_SIZE);
            case 1 -> new Rect(region.x() + ARROW_SIZE, region.y(),
                    region.width() - ARROW_SIZE * 2, region.height());
            default -> new Rect(region.right() - ARROW_SIZE, top, ARROW_SIZE, ARROW_SIZE);
        };
    }

    public static Rect badgeBox(Rect row, int textWidth) {
        if (row == null) {
            throw new IllegalArgumentException("row must not be null");
        }
        if (textWidth < 0) {
            throw new IllegalArgumentException("textWidth must not be negative: " + textWidth);
        }
        Rect card = cardBox(row);
        if (card.isEmpty() || card.height() < BADGE_SIZE) {
            return Rect.EMPTY;
        }
        int x = card.x() + CARD_PAD_X + textWidth + BADGE_GAP;
        if (x + BADGE_SIZE > card.right() - CARD_PAD_X) {
            return Rect.EMPTY;
        }
        return new Rect(x, row.y() + (row.height() - BADGE_SIZE) / 2, BADGE_SIZE, BADGE_SIZE);
    }

    public static int contentHeight(int count, Breakpoint breakpoint) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
        requireBreakpoint(breakpoint);

        if (count == 0) {
            return 0;
        }
        return count * (rowHeight(breakpoint) + rowGap(breakpoint)) - rowGap(breakpoint) + BOTTOM;
    }

    public static int maxScroll(Rect content, int count, Breakpoint breakpoint) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        int needed = contentHeight(count, breakpoint);
        return content.isEmpty() ? 0 : Math.max(0, needed - content.height());
    }

    public static int clampScroll(int scroll, Rect content, int count, Breakpoint breakpoint) {
        return Math.min(Math.max(0, scroll), maxScroll(content, count, breakpoint));
    }

    public static int scrollToReveal(Rect content, int count, int index, int scroll, Breakpoint breakpoint) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
        if (scroll < 0) {
            throw new IllegalArgumentException("scroll must not be negative: " + scroll);
        }
        requireBreakpoint(breakpoint);

        if (index < 0 || index >= count) {
            return clampScroll(scroll, content, count, breakpoint);
        }

        int top = index * (rowHeight(breakpoint) + rowGap(breakpoint));
        int bottom = top + rowHeight(breakpoint);
        int offset = scroll;
        if (bottom > offset + content.height() - BOTTOM) {
            offset = bottom - content.height() + BOTTOM;
        }
        if (top < offset) {
            offset = top;
        }
        return clampScroll(Math.max(0, offset), content, count, breakpoint);
    }

    public static int trackFill(int trackWidth, int value, int min, int max) {
        if (trackWidth < 0) {
            throw new IllegalArgumentException("trackWidth must not be negative: " + trackWidth);
        }
        if (max < min) {
            throw new IllegalArgumentException("max " + max + " is below min " + min);
        }
        if (max == min) {
            return 0;
        }
        int clamped = Math.max(min, Math.min(max, value));
        return Math.round((float) trackWidth * (clamped - min) / (max - min));
    }

    private static Breakpoint requireBreakpoint(Breakpoint breakpoint) {
        if (breakpoint == null) {
            throw new IllegalArgumentException("breakpoint must not be null");
        }
        return breakpoint;
    }
}
