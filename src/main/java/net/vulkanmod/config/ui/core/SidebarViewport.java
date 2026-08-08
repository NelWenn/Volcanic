package net.vulkanmod.config.ui.core;

public record SidebarViewport(Rect bounds, int scroll) {
    public SidebarViewport {
        if (bounds == null) {
            throw new IllegalArgumentException("bounds must not be null");
        }
        if (scroll < 0) {
            throw new IllegalArgumentException("scroll must not be negative: " + scroll);
        }
    }

    public boolean contains(int screenX, int screenY) {
        return bounds.contains(screenX, screenY);
    }

    public int contentY(int screenY) {
        return screenY - bounds.y() + scroll;
    }

    public int screenTop(int contentOffset) {
        return bounds.y() + contentOffset - scroll;
    }
}
