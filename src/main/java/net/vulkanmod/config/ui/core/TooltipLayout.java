package net.vulkanmod.config.ui.core;

public final class TooltipLayout {
    public static final int PAD_X = 6;
    public static final int PAD_Y = 4;
    public static final int TEXT_HEIGHT = 9;
    public static final int LINE_GAP = 1;
    public static final int GAP = 4;
    public static final int MARGIN = 4;

    private TooltipLayout() {
    }

    public static int maxTextWidth(Rect screen) {
        require(screen, "screen");
        return Math.max(0, screen.width() - (MARGIN + PAD_X) * 2);
    }

    public static Rect place(Rect anchor, int textWidth, int lineCount, Rect screen) {
        require(anchor, "anchor");
        require(screen, "screen");
        if (textWidth < 0) {
            throw new IllegalArgumentException("textWidth must not be negative: " + textWidth);
        }
        if (lineCount < 1) {
            throw new IllegalArgumentException("lineCount must be at least one: " + lineCount);
        }

        int width = Math.min(textWidth + PAD_X * 2, screen.width() - MARGIN * 2);
        int height = Math.min(lineCount * TEXT_HEIGHT + (lineCount - 1) * LINE_GAP + PAD_Y * 2,
                screen.height() - MARGIN * 2);
        if (width <= 0 || height <= 0) {
            return Rect.EMPTY;
        }

        int below = anchor.bottom() + GAP;
        int top = below + height <= screen.bottom() - MARGIN ? below : anchor.y() - GAP - height;
        return new Rect(clamp(anchor.x(), screen.x() + MARGIN, screen.right() - MARGIN - width),
                clamp(top, screen.y() + MARGIN, screen.bottom() - MARGIN - height), width, height);
    }

    public static int lineCapacity(Rect box) {
        require(box, "box");
        return Math.max(0, (box.height() - PAD_Y * 2 + LINE_GAP) / (TEXT_HEIGHT + LINE_GAP));
    }

    public static int lineTop(Rect box, int index) {
        require(box, "box");
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative: " + index);
        }
        return box.y() + PAD_Y + index * (TEXT_HEIGHT + LINE_GAP);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void require(Rect rect, String name) {
        if (rect == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }
}
