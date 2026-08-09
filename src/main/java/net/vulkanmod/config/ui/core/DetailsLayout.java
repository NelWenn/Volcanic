package net.vulkanmod.config.ui.core;

public final class DetailsLayout {
    public static final int PAD_X = 14;
    public static final int PAD_Y = 12;
    public static final int TEXT_HEIGHT = 9;
    public static final int BAR_HEIGHT = 4;

    private DetailsLayout() {
    }

    public static int textWidth(Rect panel) {
        require(panel, "panel");
        return Math.max(0, panel.width() - PAD_X * 2);
    }

    public static int lineCapacity(Rect panel) {
        require(panel, "panel");
        return lineCapacity(panel.height());
    }

    public static int lineCapacity(int height) {
        return Math.max(0, (height - PAD_Y * 2) / TEXT_HEIGHT);
    }

    public static int height(int itemCount) {
        if (itemCount < 0) {
            throw new IllegalArgumentException("itemCount must not be negative: " + itemCount);
        }
        return itemCount == 0 ? 0 : PAD_Y * 2 + itemCount * TEXT_HEIGHT;
    }

    public static Rect sheet(Rect content, int height, int margin) {
        require(content, "content");
        if (height <= 0) {
            return Rect.EMPTY;
        }
        int width = content.width() - margin * 2;
        int capped = Math.min(height, content.height() - margin * 2);
        if (width <= 0 || capped <= 0) {
            return Rect.EMPTY;
        }
        return new Rect(content.x() + margin, content.bottom() - margin - capped, width, capped);
    }

    public static int lineTop(Rect panel, int index) {
        require(panel, "panel");
        requireIndex(index);
        return panel.y() + PAD_Y + index * TEXT_HEIGHT;
    }

    public static Rect bar(Rect panel, int index) {
        require(panel, "panel");
        requireIndex(index);
        int width = textWidth(panel);
        if (width <= 0) {
            return Rect.EMPTY;
        }
        return new Rect(panel.x() + PAD_X, lineTop(panel, index) + (TEXT_HEIGHT - BAR_HEIGHT) / 2,
                width, BAR_HEIGHT);
    }

    public static Rect barFill(Rect track, ImpactLevel level) {
        require(track, "track");
        if (level == null) {
            throw new IllegalArgumentException("level must not be null");
        }
        int width = Math.min(track.width(), Math.round(track.width() * level.fill()));
        if (width <= 0 || track.isEmpty()) {
            return Rect.EMPTY;
        }
        return new Rect(track.x(), track.y(), width, track.height());
    }

    private static void requireIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative: " + index);
        }
    }

    private static void require(Rect rect, String name) {
        if (rect == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }
}
