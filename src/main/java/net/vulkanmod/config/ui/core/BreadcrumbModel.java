package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class BreadcrumbModel {
    public static final int SEPARATOR_ADVANCE = 14;
    public static final int HEIGHT = 9;

    private BreadcrumbModel() {
    }

    public static List<Rect> layout(int[] textWidths, int originX, int originY) {
        if (textWidths == null) {
            throw new IllegalArgumentException("textWidths must not be null");
        }
        List<Rect> segments = new ArrayList<>(textWidths.length);
        int x = originX;
        for (int textWidth : textWidths) {
            if (textWidth < 0) {
                throw new IllegalArgumentException("textWidth must not be negative");
            }
            segments.add(new Rect(x, originY, textWidth, HEIGHT));
            x += textWidth + SEPARATOR_ADVANCE;
        }
        return List.copyOf(segments);
    }

    public static int indexAt(List<Rect> segments, int mouseX, int mouseY) {
        if (segments == null) {
            throw new IllegalArgumentException("segments must not be null");
        }
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).contains(mouseX, mouseY)) {
                return i;
            }
        }
        return -1;
    }
}
