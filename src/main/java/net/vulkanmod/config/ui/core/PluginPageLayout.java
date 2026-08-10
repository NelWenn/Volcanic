package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class PluginPageLayout {
    public static final int PAD_X = 14;
    public static final int HEADING_H = 12;
    public static final int HEADING_GAP = 6;
    public static final int BLOCK_GAP = 14;
    public static final int MOD_ROW_H = 20;
    public static final int MOD_ROW_GAP = 2;
    public static final int BOTTOM = 12;
    public static final int RULE_LEAD = 24;
    public static final int COUNT_W = 24;
    public static final int ACCENT_W = 3;
    public static final int TEXT_X = ACCENT_W + SettingRowLayout.CARD_PAD_X;
    public static final int TOGGLE_W = 22;
    public static final int TOGGLE_H = 12;
    public static final int STATE_W = 44;
    public static final int TWO_LINE_MIN = 27;
    public static final int EMPTY_W = 240;
    public static final int EMPTY_H = 52;
    public static final int EMPTY_PAD = 14;

    private static final int NAME_LINE = PresetCardLayout.NAME_LINE;
    private static final int SMALL_LINE = PresetCardLayout.SMALL_LINE;
    private static final int NAME_GAP = 8;
    private static final int NAME_FLOOR = 24;
    private static final int TOGGLE_ROOM = 24;

    public record Block(Rect heading, Rect count, Rect rule, List<Rect> rows, boolean placeholder) {
        public Block {
            rows = List.copyOf(rows);
        }
    }

    public record Page(Block plugins, Block mods, Rect empty, Rect showcase, int height) {
    }

    public record Slots(Rect accent, Rect name, Rect note, Rect state, Rect toggle) {
    }

    private static final Block NO_BLOCK = new Block(Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, List.of(), false);
    private static final Page NO_PAGE = new Page(NO_BLOCK, NO_BLOCK, Rect.EMPTY, Rect.EMPTY, 0);
    private static final Slots NO_SLOTS =
            new Slots(Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY);

    private PluginPageLayout() {
    }

    public static Page page(Rect content, int pluginCount, int modCount, int scroll, Breakpoint breakpoint) {
        return page(content, pluginCount, modCount, scroll, breakpoint, -1);
    }

    public static Page page(Rect content, int pluginCount, int modCount, int scroll, Breakpoint breakpoint,
                            int expandedIndex) {
        if (content == null || breakpoint == null) {
            throw new IllegalArgumentException("content and breakpoint must not be null");
        }
        if (pluginCount < 0 || modCount < 0) {
            throw new IllegalArgumentException("counts must not be negative");
        }
        int usable = content.width() - PAD_X * 2;
        if (content.isEmpty() || usable <= 0) {
            return NO_PAGE;
        }

        int x = content.x() + PAD_X;
        int base = content.y() - scroll;
        int height = contentHeight(pluginCount, modCount, breakpoint);

        if (pluginCount == 0 && modCount == 0) {
            int cardWidth = Math.min(EMPTY_W, usable);
            int free = content.height() - BOTTOM - EMPTY_H;
            Rect empty = new Rect(x + Math.max(0, (usable - cardWidth) / 2),
                    base + Math.max(0, free / 2), cardWidth, EMPTY_H);
            return new Page(NO_BLOCK, NO_BLOCK, empty, Rect.EMPTY, height);
        }

        int offset = 0;
        boolean placeholder = pluginCount == 0;
        int shown = placeholder ? 1 : pluginCount;
        int rowHeight = placeholder ? MOD_ROW_H : SettingRowLayout.rowHeight(breakpoint);
        int rowGap = placeholder ? MOD_ROW_GAP : SettingRowLayout.rowGap(breakpoint);
        Block plugins = block(x, base, offset, usable, shown, rowHeight, rowGap, placeholder);
        offset += blockHeight(shown, rowHeight, rowGap);

        Rect showcase = Rect.EMPTY;
        if (!placeholder && expandedIndex >= 0 && expandedIndex < shown) {
            int showHeight = PluginShowcase.height(usable);
            int drop = showHeight + rowGap;
            Rect anchor = plugins.rows().get(expandedIndex);
            showcase = new Rect(x, anchor.bottom() + rowGap, usable, showHeight);
            List<Rect> moved = new ArrayList<>(plugins.rows());
            for (int index = expandedIndex + 1; index < moved.size(); index++) {
                moved.set(index, moved.get(index).translated(0, drop));
            }
            plugins = new Block(plugins.heading(), plugins.count(), plugins.rule(), moved, false);
            offset += drop;
            height += drop;
        }

        Block mods = NO_BLOCK;
        if (modCount > 0) {
            offset += BLOCK_GAP;
            mods = block(x, base, offset, usable, modCount, MOD_ROW_H, MOD_ROW_GAP, false);
        }
        return new Page(plugins, mods, Rect.EMPTY, showcase, height);
    }

    public static Slots slots(Rect row, boolean toggleable) {
        if (row == null) {
            throw new IllegalArgumentException("row must not be null");
        }
        if (row.isEmpty()) {
            return NO_SLOTS;
        }
        Rect accent = new Rect(row.x(), row.y(), Math.min(ACCENT_W, row.width()), row.height());
        int left = row.x() + TEXT_X;

        Rect toggle = toggleable
                && row.width() >= TEXT_X + SettingRowLayout.CARD_PAD_X + TOGGLE_W + TOGGLE_ROOM
                ? new Rect(row.right() - SettingRowLayout.CARD_PAD_X - TOGGLE_W,
                        row.y() + (row.height() - TOGGLE_H) / 2, TOGGLE_W, TOGGLE_H)
                : Rect.EMPTY;
        Rect state = toggleable || row.width() < TEXT_X + SettingRowLayout.CARD_PAD_X + STATE_W
                ? Rect.EMPTY
                : new Rect(row.right() - SettingRowLayout.CARD_PAD_X - STATE_W,
                        row.y() + (row.height() - SMALL_LINE) / 2, STATE_W, SMALL_LINE);

        int nameRight = !toggle.isEmpty() ? toggle.x() - NAME_GAP
                : !state.isEmpty() ? state.x() - NAME_GAP
                : row.right() - SettingRowLayout.CARD_PAD_X;
        int nameWidth = nameRight - left;
        if (nameWidth <= NAME_FLOOR && !state.isEmpty()) {
            state = Rect.EMPTY;
            nameWidth = row.right() - SettingRowLayout.CARD_PAD_X - left;
        }
        if (nameWidth <= 0) {
            return new Slots(accent, Rect.EMPTY, Rect.EMPTY, state, toggle);
        }

        boolean roomy = row.height() >= TWO_LINE_MIN;
        Rect name = new Rect(left,
                roomy ? row.y() + 5 : row.y() + (row.height() - NAME_LINE) / 2, nameWidth, NAME_LINE);
        Rect note = roomy ? new Rect(left, row.y() + 15, nameWidth, SMALL_LINE) : Rect.EMPTY;
        return new Slots(accent, name, note, state, toggle);
    }

    public static Rect ruleLead(Rect rule) {
        return rule.isEmpty() ? Rect.EMPTY
                : new Rect(rule.x(), rule.y(), Math.min(RULE_LEAD, rule.width()), rule.height());
    }

    public static Rect modLabel(Rect row) {
        int width = row.width() - TEXT_X - SettingRowLayout.CARD_PAD_X;
        return row.isEmpty() || width <= 0 ? Rect.EMPTY
                : new Rect(row.x() + TEXT_X, row.y() + (row.height() - NAME_LINE) / 2, width, NAME_LINE);
    }

    public static Rect emptyTitle(Rect card) {
        int width = card.width() - EMPTY_PAD * 2;
        return card.isEmpty() || width <= 0 ? Rect.EMPTY
                : new Rect(card.x() + EMPTY_PAD, card.y() + 12, width, NAME_LINE);
    }

    public static Rect emptyBody(Rect card) {
        int width = card.width() - EMPTY_PAD * 2;
        return card.isEmpty() || width <= 0 ? Rect.EMPTY
                : new Rect(card.x() + EMPTY_PAD, card.y() + 26, width, SMALL_LINE * 2);
    }

    public static int contentHeight(int pluginCount, int modCount, Breakpoint breakpoint) {
        if (breakpoint == null) {
            throw new IllegalArgumentException("breakpoint must not be null");
        }
        if (pluginCount < 0 || modCount < 0) {
            throw new IllegalArgumentException("counts must not be negative");
        }
        if (pluginCount == 0 && modCount == 0) {
            return EMPTY_H + BOTTOM;
        }
        boolean placeholder = pluginCount == 0;
        int shown = placeholder ? 1 : pluginCount;
        int rowHeight = placeholder ? MOD_ROW_H : SettingRowLayout.rowHeight(breakpoint);
        int rowGap = placeholder ? MOD_ROW_GAP : SettingRowLayout.rowGap(breakpoint);

        int total = blockHeight(shown, rowHeight, rowGap);
        if (modCount > 0) {
            total += BLOCK_GAP + blockHeight(modCount, MOD_ROW_H, MOD_ROW_GAP);
        }
        return total + BOTTOM;
    }

    public static int maxScroll(Rect content, int pluginCount, int modCount, Breakpoint breakpoint) {
        return maxScroll(content, pluginCount, modCount, breakpoint, false);
    }

    public static int maxScroll(Rect content, int pluginCount, int modCount, Breakpoint breakpoint,
                                boolean expanded) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        int height = contentHeight(pluginCount, modCount, breakpoint);
        if (expanded && pluginCount > 0) {
            int usable = content.width() - PAD_X * 2;
            height += PluginShowcase.height(Math.max(0, usable))
                    + SettingRowLayout.rowGap(breakpoint);
        }
        return Math.max(0, height - content.height());
    }

    public static int clampScroll(int scroll, Rect content, int pluginCount, int modCount,
                                  Breakpoint breakpoint) {
        return Math.max(0, Math.min(scroll, maxScroll(content, pluginCount, modCount, breakpoint)));
    }

    private static Block block(int x, int base, int offset, int usable, int count,
                               int rowHeight, int rowGap, boolean placeholder) {
        Rect heading = new Rect(x, base + offset + 1,
                Math.max(0, usable - COUNT_W - 6), SMALL_LINE);
        Rect tally = new Rect(x + usable - COUNT_W, base + offset + 1, COUNT_W, SMALL_LINE);
        Rect rule = new Rect(x, base + offset + HEADING_H - 1, usable, 1);

        int top = offset + HEADING_H + HEADING_GAP;
        List<Rect> rows = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            rows.add(new Rect(x, base + top + index * (rowHeight + rowGap), usable, rowHeight));
        }
        return new Block(heading, tally, rule, rows, placeholder);
    }

    private static int blockHeight(int count, int rowHeight, int rowGap) {
        return HEADING_H + HEADING_GAP + count * (rowHeight + rowGap) - rowGap;
    }
}
