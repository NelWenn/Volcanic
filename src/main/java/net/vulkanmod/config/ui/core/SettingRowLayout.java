package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class SettingRowLayout {
    public static final int ROW_HEIGHT = 34;
    public static final int ROW_GAP = 6;
    public static final int RESET_SIZE = 9;
    private static final int RESET_INSET = 12;
    private static final int PAD_X = 14;
    private static final int TOP = 70;

    private SettingRowLayout() {
    }

    public static List<Rect> rows(Rect content, int count, int scroll) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative: " + count);
        }
        if (scroll < 0) {
            throw new IllegalArgumentException("scroll must not be negative: " + scroll);
        }
        int width = content.width() - PAD_X * 2;
        if (content.isEmpty() || width <= 0) {
            return List.of();
        }

        List<Rect> rows = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            rows.add(new Rect(content.x() + PAD_X,
                    content.y() + TOP + index * (ROW_HEIGHT + ROW_GAP) - scroll, width, ROW_HEIGHT));
        }
        return List.copyOf(rows);
    }

    public static Rect resetBox(Rect row) {
        if (row == null) {
            throw new IllegalArgumentException("row must not be null");
        }
        if (row.width() < RESET_INSET * 2 + RESET_SIZE || row.height() < RESET_SIZE) {
            return Rect.EMPTY;
        }
        return new Rect(row.right() - RESET_INSET - RESET_SIZE,
                row.y() + (row.height() - RESET_SIZE) / 2, RESET_SIZE, RESET_SIZE);
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
}
