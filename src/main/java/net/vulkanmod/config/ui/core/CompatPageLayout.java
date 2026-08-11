package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class CompatPageLayout {
    public static final int ROW_H = 28;
    public static final int ROW_GAP = 2;

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
            return new Page(List.of(), PluginPageLayout.emptyCard(content, x, usable, base), height);
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
            blocks.add(PluginPageLayout.block(x, base + offset, 0, usable, count, ROW_H, ROW_GAP, false));
            offset += PluginPageLayout.blockHeight(count, ROW_H, ROW_GAP);
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
            total += PluginPageLayout.blockHeight(count, ROW_H, ROW_GAP);
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
