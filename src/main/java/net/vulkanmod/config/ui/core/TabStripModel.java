package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class TabStripModel {
    private static final int PADDING = 9;
    private static final int GAP = 5;
    private static final int HEIGHT = 17;

    private TabStripModel() {
    }

    public static List<Rect> layout(int[] textWidths, int originX, int originY) {
        if (textWidths == null) {
            throw new IllegalArgumentException("textWidths must not be null");
        }
        List<Rect> boxes = new ArrayList<>(textWidths.length);
        int x = originX;
        for (int textWidth : textWidths) {
            if (textWidth < 0) {
                throw new IllegalArgumentException("textWidth must not be negative");
            }
            int width = textWidth + PADDING * 2;
            boxes.add(new Rect(x, originY, width, HEIGHT));
            x += width + GAP;
        }
        return List.copyOf(boxes);
    }

    public static int scrollToReveal(List<Rect> boxes, int index, int viewportLeft, int viewportRight) {
        if (boxes == null) {
            throw new IllegalArgumentException("boxes must not be null");
        }
        if (viewportRight < viewportLeft) {
            throw new IllegalArgumentException("viewportRight must not precede viewportLeft");
        }
        if (boxes.isEmpty() || index < 0 || index >= boxes.size()) {
            return 0;
        }

        int total = boxes.get(boxes.size() - 1).right() - viewportLeft;
        int maximum = Math.max(0, total - (viewportRight - viewportLeft));
        Rect target = boxes.get(index);
        int offset = 0;
        if (target.right() > viewportRight) {
            offset = target.right() - viewportRight;
        }
        if (target.x() - offset < viewportLeft) {
            offset = target.x() - viewportLeft;
        }
        return Math.max(0, Math.min(offset, maximum));
    }

    public static List<Rect> shifted(List<Rect> boxes, int offsetX) {
        if (boxes == null) {
            throw new IllegalArgumentException("boxes must not be null");
        }
        if (offsetX == 0) {
            return boxes;
        }
        List<Rect> shifted = new ArrayList<>(boxes.size());
        for (Rect box : boxes) {
            shifted.add(new Rect(box.x() - offsetX, box.y(), box.width(), box.height()));
        }
        return List.copyOf(shifted);
    }

    public static int indexAt(List<Rect> boxes, int mouseX, int mouseY) {
        if (boxes == null) {
            throw new IllegalArgumentException("boxes must not be null");
        }
        for (int i = 0; i < boxes.size(); i++) {
            if (boxes.get(i).contains(mouseX, mouseY)) {
                return i;
            }
        }
        return -1;
    }
}
