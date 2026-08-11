package net.vulkanmod.config.ui.core;

public final class TooltipLayout {
    public static final int GAP = 4;
    public static final int MARGIN = 4;

    private TooltipLayout() {
    }

    public static Rect placeBox(Rect anchor, int boxWidth, int boxHeight, Rect screen) {
        require(anchor, "anchor");
        require(screen, "screen");
        int width = Math.min(boxWidth, screen.width() - MARGIN * 2);
        int height = Math.min(boxHeight, screen.height() - MARGIN * 2);
        if (width <= 0 || height <= 0) {
            return Rect.EMPTY;
        }

        int below = anchor.bottom() + GAP;
        int top = below + height <= screen.bottom() - MARGIN ? below : anchor.y() - GAP - height;
        return new Rect(clamp(anchor.x(), screen.x() + MARGIN, screen.right() - MARGIN - width),
                clamp(top, screen.y() + MARGIN, screen.bottom() - MARGIN - height), width, height);
    }

    public static int availableHeight(Rect anchor, Rect screen) {
        require(anchor, "anchor");
        require(screen, "screen");
        int below = screen.bottom() - MARGIN - (anchor.bottom() + GAP);
        int above = anchor.y() - GAP - (screen.y() + MARGIN);
        return Math.max(0, Math.max(below, above));
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
