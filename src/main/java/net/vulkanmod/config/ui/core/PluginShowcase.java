package net.vulkanmod.config.ui.core;

public final class PluginShowcase {
    public static final int PAD = 12;
    public static final int ICON = 32;
    public static final int ICON_GAP = 10;
    public static final int BUTTON_W = 68;
    public static final int BUTTON_H = 16;
    public static final int TAG_H = 11;
    public static final int MAX_H = 300;
    public static final int NAME_LINE = 9;
    public static final int SMALL_LINE = 7;

    public record Crop(int u, int v, int uw, int vh) {
    }

    public record Slots(Rect icon, Rect title, Rect byline, Rect desc, Rect tags, Rect button) {
    }

    private static final Slots NO_SLOTS = new Slots(Rect.EMPTY, Rect.EMPTY, Rect.EMPTY,
            Rect.EMPTY, Rect.EMPTY, Rect.EMPTY);

    private PluginShowcase() {
    }

    public static int height(int width) {
        if (width <= 0) {
            return 0;
        }
        return Math.min(MAX_H, Math.round(width * 9 / 16.0f));
    }

    public static Crop cover(int frameW, int frameH, int texW, int texH) {
        if (frameW <= 0 || frameH <= 0 || texW <= 0 || texH <= 0) {
            return new Crop(0, 0, Math.max(0, texW), Math.max(0, texH));
        }
        float scale = Math.max(frameW / (float) texW, frameH / (float) texH);
        int usedW = Math.max(1, Math.round(frameW / scale));
        int usedH = Math.max(1, Math.round(frameH / scale));
        usedW = Math.min(usedW, texW);
        usedH = Math.min(usedH, texH);
        return new Crop((texW - usedW) / 2, (texH - usedH) / 2, usedW, usedH);
    }

    public static Slots slots(Rect frame) {
        if (frame == null) {
            throw new IllegalArgumentException("frame must not be null");
        }
        if (frame.isEmpty() || frame.width() < PAD * 2 + ICON + ICON_GAP + BUTTON_W
                || frame.height() < PAD * 2 + ICON) {
            return NO_SLOTS;
        }
        Rect button = new Rect(frame.right() - PAD - BUTTON_W, frame.bottom() - PAD - BUTTON_H,
                BUTTON_W, BUTTON_H);
        Rect icon = new Rect(frame.x() + PAD, frame.bottom() - PAD - ICON, ICON, ICON);

        int left = icon.right() + ICON_GAP;
        int width = button.x() - ICON_GAP - left;
        if (width <= 0) {
            return new Slots(icon, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, button);
        }

        Rect tags = new Rect(left, frame.bottom() - PAD - TAG_H, width, TAG_H);
        int descLines = frame.height() >= 150 ? 3 : frame.height() >= 115 ? 2 : 1;
        Rect desc = new Rect(left, tags.y() - 3 - descLines * SMALL_LINE, width,
                descLines * SMALL_LINE);
        Rect byline = new Rect(left, desc.y() - 2 - SMALL_LINE, width, SMALL_LINE);
        Rect title = new Rect(left, byline.y() - 2 - NAME_LINE, width, NAME_LINE);
        if (title.y() < frame.y() + PAD) {
            return new Slots(icon, new Rect(left, frame.y() + PAD, width, NAME_LINE),
                    Rect.EMPTY, Rect.EMPTY, tags, button);
        }
        return new Slots(icon, title, byline, desc, tags, button);
    }
}
