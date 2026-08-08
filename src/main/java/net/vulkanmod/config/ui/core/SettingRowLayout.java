package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class SettingRowLayout {
    public static final int CARD_RADIUS = 8;
    public static final int RESET_SIZE = 15;
    public static final int RESET_RADIUS = 4;
    public static final int RESET_GAP = 5;

    private static final int ROW_HEIGHT = 27;
    private static final int ROW_HEIGHT_COMPACT = 22;
    private static final int ROW_GAP = 5;
    private static final int ROW_GAP_COMPACT = 3;
    private static final int PAD_X = 14;
    private static final int TOP = 70;
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
                    content.y() + TOP + index * pitch - scroll, width, height));
        }
        return List.copyOf(rows);
    }

    public static Rect cardBox(Rect row) {
        if (row == null) {
            throw new IllegalArgumentException("row must not be null");
        }
        int width = row.width() - RESET_SIZE - RESET_GAP;
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

    public static int contentHeight(int count, Breakpoint breakpoint) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
        requireBreakpoint(breakpoint);

        if (count == 0) {
            return 0;
        }
        return TOP + count * (rowHeight(breakpoint) + rowGap(breakpoint)) - rowGap(breakpoint) + BOTTOM;
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

        int top = TOP + index * (rowHeight(breakpoint) + rowGap(breakpoint));
        int bottom = top + rowHeight(breakpoint);
        int offset = scroll;
        if (bottom > offset + content.height() - BOTTOM) {
            offset = bottom - content.height() + BOTTOM;
        }
        if (top < offset + TOP) {
            offset = top - TOP;
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
