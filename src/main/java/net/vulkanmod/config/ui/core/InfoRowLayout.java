package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class InfoRowLayout {
    public static final int PAD_X = 14;
    public static final int TOP = 4;
    public static final int BOTTOM = 12;
    public static final int ROW_H = 15;
    public static final int SECTION_H = 18;
    public static final int ACCENT_W = 3;
    public static final int TEXT_X = ACCENT_W + SettingRowLayout.CARD_PAD_X;
    public static final int SCROLLBAR = 6;
    public static final int LABEL_SHARE = 45;
    public static final int LABEL_MIN = 40;

    private static final int LINE = TextScale.LINE_HEIGHT;
    private static final int SMALL_LINE = PresetCardLayout.SMALL_LINE;
    private static final int LABEL_GAP = 10;

    private InfoRowLayout() {
    }

    public static List<Rect> rows(Rect content, boolean[] sections, int scroll) {
        if (content == null || sections == null) {
            throw new IllegalArgumentException("content and sections must not be null");
        }
        int usable = content.width() - PAD_X * 2;
        if (content.isEmpty() || usable <= 0) {
            return List.of();
        }
        List<Rect> rows = new ArrayList<>(sections.length);
        int cursor = TOP;
        for (boolean section : sections) {
            int height = section ? SECTION_H : ROW_H;
            rows.add(new Rect(content.x() + PAD_X, content.y() + cursor - scroll, usable, height));
            cursor += height;
        }
        return List.copyOf(rows);
    }

    public static int contentHeight(boolean[] sections) {
        if (sections == null) {
            throw new IllegalArgumentException("sections must not be null");
        }
        int height = TOP;
        for (boolean section : sections) {
            height += section ? SECTION_H : ROW_H;
        }
        return height + BOTTOM;
    }

    public static int maxScroll(Rect content, boolean[] sections) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        return Math.max(0, contentHeight(sections) - content.height());
    }

    public static Rect sectionLabel(Rect row) {
        int width = row.width() - TEXT_X - SCROLLBAR;
        return row.isEmpty() || width <= 0 ? Rect.EMPTY
                : new Rect(row.x() + TEXT_X, row.bottom() - SMALL_LINE - 4, width, SMALL_LINE);
    }

    public static Rect sectionAccent(Rect row) {
        return row.isEmpty() ? Rect.EMPTY
                : new Rect(row.x(), row.bottom() - SMALL_LINE - 4,
                        Math.min(ACCENT_W, row.width()), SMALL_LINE);
    }

    public static Rect sectionRule(Rect row) {
        return row.isEmpty() ? Rect.EMPTY
                : new Rect(row.x(), row.bottom() - 1, Math.max(0, row.width() - SCROLLBAR), 1);
    }

    public static Rect label(Rect row) {
        int room = columnRoom(row);
        int width = room * LABEL_SHARE / 100;
        return room <= 0 || width < LABEL_MIN ? Rect.EMPTY
                : new Rect(row.x() + TEXT_X, row.y() + (row.height() - LINE) / 2, width, LINE);
    }

    public static Rect value(Rect row) {
        int room = columnRoom(row);
        if (room <= 0) {
            return Rect.EMPTY;
        }
        int width = label(row).isEmpty() ? room : room - room * LABEL_SHARE / 100 - LABEL_GAP;
        if (width <= 0) {
            width = room;
        }
        return new Rect(row.right() - SCROLLBAR - width, row.y() + (row.height() - LINE) / 2,
                width, LINE);
    }

    private static int columnRoom(Rect row) {
        return row.isEmpty() ? 0 : row.width() - TEXT_X - SCROLLBAR;
    }
}
