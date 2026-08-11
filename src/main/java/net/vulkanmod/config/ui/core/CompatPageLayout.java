package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class CompatPageLayout {
    public static final int ROW_H = 28;
    public static final int ROW_GAP = 2;

    private static final int SMALL_LINE = PresetCardLayout.SMALL_LINE;

    public record Page(List<PluginPageLayout.Block> blocks, Rect empty, int height) {
        public Page {
            blocks = List.copyOf(blocks);
        }
    }

    private static final Page NO_PAGE = new Page(List.of(), Rect.EMPTY, 0);
    private static final PluginPageLayout.Block NO_BLOCK =
            new PluginPageLayout.Block(Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, List.of(), false);

    private CompatPageLayout() {
    }

    public static Page page(Rect content, List<Integer> counts, int scroll) {
        if (content == null || counts == null) {
            throw new IllegalArgumentException("content and counts must not be null");
        }
        if (scroll < 0) {
            throw new IllegalArgumentException("scroll must not be negative: " + scroll);
        }
        int usable = content.width() - PluginPageLayout.PAD_X * 2;
        if (content.isEmpty() || usable <= 0) {
            return NO_PAGE;
        }

        int x = content.x() + PluginPageLayout.PAD_X;
        int base = content.y() - scroll;
        int height = contentHeight(counts);

        if (total(counts) == 0) {
            int cardWidth = Math.min(PluginPageLayout.EMPTY_W, usable);
            int free = content.height() - PluginPageLayout.BOTTOM - PluginPageLayout.EMPTY_H;
            Rect empty = new Rect(x + Math.max(0, (usable - cardWidth) / 2),
                    base + Math.max(0, free / 2), cardWidth, PluginPageLayout.EMPTY_H);
            return new Page(List.of(), empty, height);
        }

        List<PluginPageLayout.Block> blocks = new ArrayList<>(counts.size());
        int offset = 0;
        boolean first = true;
        for (int index = 0; index < counts.size(); index++) {
            int count = count(counts, index);
            if (count == 0) {
                blocks.add(NO_BLOCK);
                continue;
            }
            if (!first) {
                offset += PluginPageLayout.BLOCK_GAP;
            }
            first = false;
            blocks.add(block(x, base + offset, usable, count));
            offset += blockHeight(count);
        }
        return new Page(blocks, Rect.EMPTY, height);
    }

    public static int contentHeight(List<Integer> counts) {
        if (counts == null) {
            throw new IllegalArgumentException("counts must not be null");
        }
        int total = 0;
        boolean first = true;
        for (int index = 0; index < counts.size(); index++) {
            int count = count(counts, index);
            if (count == 0) {
                continue;
            }
            if (!first) {
                total += PluginPageLayout.BLOCK_GAP;
            }
            first = false;
            total += blockHeight(count);
        }
        return total == 0
                ? PluginPageLayout.EMPTY_H + PluginPageLayout.BOTTOM
                : total + PluginPageLayout.BOTTOM;
    }

    public static int maxScroll(Rect content, List<Integer> counts) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        return Math.max(0, contentHeight(counts) - content.height());
    }

    private static PluginPageLayout.Block block(int x, int top, int usable, int count) {
        Rect heading = new Rect(x, top + 1,
                Math.max(0, usable - PluginPageLayout.COUNT_W - 6), SMALL_LINE);
        Rect tally = new Rect(x + usable - PluginPageLayout.COUNT_W, top + 1,
                PluginPageLayout.COUNT_W, SMALL_LINE);
        Rect rule = new Rect(x, top + PluginPageLayout.HEADING_H - 1, usable, 1);

        int rowsTop = top + PluginPageLayout.HEADING_H + PluginPageLayout.HEADING_GAP;
        List<Rect> rows = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            rows.add(new Rect(x, rowsTop + index * (ROW_H + ROW_GAP), usable, ROW_H));
        }
        return new PluginPageLayout.Block(heading, tally, rule, rows, false);
    }

    private static int blockHeight(int count) {
        return PluginPageLayout.HEADING_H + PluginPageLayout.HEADING_GAP
                + count * (ROW_H + ROW_GAP) - ROW_GAP;
    }

    private static int total(List<Integer> counts) {
        int total = 0;
        for (int index = 0; index < counts.size(); index++) {
            total += count(counts, index);
        }
        return total;
    }

    private static int count(List<Integer> counts, int index) {
        Integer count = counts.get(index);
        if (count == null || count < 0) {
            throw new IllegalArgumentException("count " + index + " must be zero or more: " + count);
        }
        return count;
    }
}
