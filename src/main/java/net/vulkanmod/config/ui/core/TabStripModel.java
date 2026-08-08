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
