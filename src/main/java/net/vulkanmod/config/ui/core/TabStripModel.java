package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class TabStripModel {
    public static final int HEIGHT = 17;
    public static final int ARROW_W = 11;
    public static final int ARROW_GAP = 4;

    private static final int PADDING = 9;
    private static final int GAP = 5;

    public record Strip(List<Rect> boxes, Rect prev, Rect next, Rect viewport, int offset) {
        public Strip {
            boxes = List.copyOf(boxes);
        }

        public boolean scrollable() {
            return !prev.isEmpty();
        }
    }

    private TabStripModel() {
    }

    public static Strip strip(int[] textWidths, Rect band, int offset, int revealIndex) {
        if (textWidths == null || band == null) {
            throw new IllegalArgumentException("textWidths and band must not be null");
        }
        if (band.isEmpty() || textWidths.length == 0) {
            return new Strip(List.of(), Rect.EMPTY, Rect.EMPTY, band, 0);
        }
        List<Rect> measured = layout(textWidths, 0, band.y());
        int span = measured.get(measured.size() - 1).right();
        if (span <= band.width()) {
            return new Strip(layout(textWidths, band.x(), band.y()), Rect.EMPTY, Rect.EMPTY, band, 0);
        }

        int inset = ARROW_W + ARROW_GAP;
        Rect viewport = new Rect(band.x() + inset, band.y(),
                Math.max(0, band.width() - inset * 2), HEIGHT);
        List<Rect> placed = layout(textWidths, viewport.x(), band.y());
        int clamped = clampOffset(offset, placed, viewport);
        clamped = scrollToReveal(placed, revealIndex, viewport.x(), viewport.right(), clamped);
        return new Strip(shifted(placed, clamped),
                new Rect(band.x(), band.y(), ARROW_W, HEIGHT),
                new Rect(band.right() - ARROW_W, band.y(), ARROW_W, HEIGHT),
                viewport, clamped);
    }

    public static int stepOffset(Strip strip, int direction) {
        if (strip == null) {
            throw new IllegalArgumentException("strip must not be null");
        }
        if (direction == 0 || !strip.scrollable()) {
            return strip.offset();
        }
        Rect viewport = strip.viewport();
        int ceiling = maxOffset(strip.boxes(), viewport) + strip.offset();
        int best = direction > 0 ? ceiling : 0;
        for (Rect box : strip.boxes()) {
            int candidate = strip.offset() + box.x() - viewport.x();
            if (direction > 0 && candidate > strip.offset() && candidate < best) {
                best = candidate;
            }
            if (direction < 0 && candidate < strip.offset() && candidate > best) {
                best = candidate;
            }
        }
        return Math.max(0, Math.min(best, ceiling));
    }

    public static boolean visible(Strip strip, Rect box) {
        return !strip.scrollable()
                || (box.right() > strip.viewport().x() && box.x() < strip.viewport().right());
    }

    public static int maxOffset(List<Rect> boxes, Rect viewport) {
        if (boxes == null || viewport == null) {
            throw new IllegalArgumentException("boxes and viewport must not be null");
        }
        return boxes.isEmpty() ? 0
                : Math.max(0, boxes.get(boxes.size() - 1).right() - viewport.right());
    }

    public static int clampOffset(int offset, List<Rect> boxes, Rect viewport) {
        return Math.max(0, Math.min(offset, maxOffset(boxes, viewport)));
    }

    public static int scrollToReveal(List<Rect> boxes, int index, int viewportLeft, int viewportRight,
                                     int offset) {
        if (boxes == null) {
            throw new IllegalArgumentException("boxes must not be null");
        }
        if (viewportRight < viewportLeft) {
            throw new IllegalArgumentException("viewportRight must not precede viewportLeft");
        }
        if (boxes.isEmpty() || index < 0 || index >= boxes.size()) {
            return offset;
        }
        int maximum = Math.max(0, boxes.get(boxes.size() - 1).right() - viewportRight);
        Rect target = boxes.get(index);
        int nudged = offset;
        if (target.right() - nudged > viewportRight) {
            nudged = target.right() - viewportRight;
        }
        if (target.x() - nudged < viewportLeft) {
            nudged = target.x() - viewportLeft;
        }
        return Math.max(0, Math.min(nudged, maximum));
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
