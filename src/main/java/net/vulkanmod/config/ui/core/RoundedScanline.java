package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class RoundedScanline {
    private RoundedScanline() {
    }

    public static int[] insets(int width, int height, int radius) {
        if (width < 0) {
            throw new IllegalArgumentException("width must not be negative: " + width);
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must not be negative: " + height);
        }
        if (radius < 0) {
            throw new IllegalArgumentException("radius must not be negative: " + radius);
        }

        int corner = Math.min(radius, Math.min(width, height) / 2);
        int[] insets = new int[height];
        for (int row = 0; row < height; row++) {
            float centre = row + 0.5f;
            float distance = 0.0f;
            if (centre < corner) {
                distance = corner - centre;
            } else if (centre > height - corner) {
                distance = centre - (height - corner);
            }
            insets[row] = distance <= 0.0f
                    ? 0
                    : Math.round(corner - (float) Math.sqrt(Math.max(0.0f, corner * corner - distance * distance)));
        }
        return insets;
    }

    public static List<Rect> fillSpans(Rect rect, int radius) {
        if (rect == null) {
            throw new IllegalArgumentException("rect must not be null");
        }
        if (rect.isEmpty()) {
            return List.of();
        }

        int width = rect.width();
        int[] insets = insets(width, rect.height(), radius);
        List<Rect> spans = new ArrayList<>(insets.length);
        for (int row = 0; row < insets.length; row++) {
            int inset = insets[row];
            int span = width - inset * 2;
            if (span > 0) {
                spans.add(new Rect(rect.x() + inset, rect.y() + row, span, 1));
            }
        }
        return spans;
    }

    public static List<Rect> outlineSpans(Rect rect, int radius) {
        if (rect == null) {
            throw new IllegalArgumentException("rect must not be null");
        }
        if (rect.isEmpty()) {
            return List.of();
        }

        int width = rect.width();
        int height = rect.height();
        int[] insets = insets(width, height, radius);
        List<Rect> spans = new ArrayList<>(insets.length * 2);
        for (int row = 0; row < height; row++) {
            int inset = insets[row];
            int end = width - inset;
            if (end <= inset) {
                continue;
            }

            int above = row == 0 ? width : insets[row - 1];
            int below = row == height - 1 ? width : insets[row + 1];
            int thickness = Math.max(inset + 1, Math.max(above, below));

            int leftEnd = Math.min(thickness, end);
            spans.add(new Rect(rect.x() + inset, rect.y() + row, leftEnd - inset, 1));

            int rightStart = Math.max(leftEnd, width - thickness);
            if (rightStart < end) {
                spans.add(new Rect(rect.x() + rightStart, rect.y() + row, end - rightStart, 1));
            }
        }
        return spans;
    }
}
