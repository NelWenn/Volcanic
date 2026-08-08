package net.vulkanmod.config.ui.core;

public record ScrollIndicator(Rect track, Rect thumb) {
    public static final ScrollIndicator NONE = new ScrollIndicator(Rect.EMPTY, Rect.EMPTY);

    private static final int WIDTH = 3;
    private static final int MARGIN = 1;
    private static final int MIN_THUMB = 8;
    private static final int MAX_THUMB_DIVISOR = 3;

    public ScrollIndicator {
        if (track == null) {
            throw new IllegalArgumentException("track must not be null");
        }
        if (thumb == null) {
            throw new IllegalArgumentException("thumb must not be null");
        }
    }

    public static ScrollIndicator of(Rect viewport, int contentHeight, int scroll) {
        if (viewport == null) {
            throw new IllegalArgumentException("viewport must not be null");
        }
        if (contentHeight < 0) {
            throw new IllegalArgumentException("contentHeight must not be negative: " + contentHeight);
        }
        if (scroll < 0) {
            throw new IllegalArgumentException("scroll must not be negative: " + scroll);
        }

        int height = viewport.height();
        if (viewport.isEmpty() || viewport.width() < WIDTH + MARGIN || contentHeight <= height) {
            return NONE;
        }

        Rect track = new Rect(viewport.right() - MARGIN - WIDTH, viewport.y(), WIDTH, height);
        int proportional = Math.round((float) height * height / contentHeight);
        int thumbHeight = Math.min(Math.max(MIN_THUMB, Math.min(proportional, height / MAX_THUMB_DIVISOR)),
                height);
        int travel = height - thumbHeight;
        int offset = travel == 0
                ? 0
                : Math.round((float) travel * Math.min(scroll, contentHeight - height) / (contentHeight - height));
        return new ScrollIndicator(track, new Rect(track.x(), track.y() + offset, WIDTH, thumbHeight));
    }

    public boolean visible() {
        return !thumb.isEmpty();
    }
}
