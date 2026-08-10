package net.vulkanmod.config.ui.core;

public final class PluginShowcase {
    public static final int PAD = 12;
    public static final int ICON = 40;
    public static final int ICON_GAP = 12;
    public static final int BUTTON_W = 68;
    public static final int BUTTON_H = 16;
    public static final int TAG_H = 11;
    public static final int MAX_H = 210;
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
        return Math.min(MAX_H, Math.round(width * 9 / 21.0f));
    }

    public static Crop cover(int frameW, int frameH, int texW, int texH) {
        return cover(frameW, frameH, texW, texH, 0.5f);
    }

    public static Crop cover(int frameW, int frameH, int texW, int texH, float anchorY) {
        if (frameW <= 0 || frameH <= 0 || texW <= 0 || texH <= 0) {
            return new Crop(0, 0, Math.max(0, texW), Math.max(0, texH));
        }
        float scale = Math.max(frameW / (float) texW, frameH / (float) texH);
        int usedW = Math.max(1, Math.round(frameW / scale));
        int usedH = Math.max(1, Math.round(frameH / scale));
        usedW = Math.min(usedW, texW);
        usedH = Math.min(usedH, texH);
        float clamped = Math.max(0.0f, Math.min(1.0f, anchorY));
        return new Crop((texW - usedW) / 2, Math.round((texH - usedH) * clamped), usedW, usedH);
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
        Rect tags = new Rect(frame.x() + PAD + 2, frame.bottom() - PAD - TAG_H,
                button.x() - 8 - frame.x() - PAD - 2, TAG_H);

        int stackTop = frame.y() + Math.max(PAD + 4, Math.round(frame.height() * 0.34f));
        Rect icon = new Rect(frame.x() + PAD + 2, stackTop, ICON, ICON);
        int left = icon.right() + ICON_GAP;
        int width = frame.right() - PAD - left;
        if (width <= 0) {
            return new Slots(icon, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, tags, button);
        }

        Rect title = new Rect(left, stackTop + 3, width, NAME_LINE);
        Rect byline = new Rect(left, title.bottom() + 4, width, SMALL_LINE);
        int descTop = byline.bottom() + 7;
        int descLines = Math.min(4, (tags.y() - 5 - descTop) / SMALL_LINE);
        if (descLines < 1) {
            int squeezeTop = frame.y() + PAD + 2;
            icon = new Rect(frame.x() + PAD + 2, squeezeTop, ICON, ICON);
            title = new Rect(left, squeezeTop + 3, width, NAME_LINE);
            byline = new Rect(left, title.bottom() + 2, width, SMALL_LINE);
            descTop = byline.bottom() + 4;
            descLines = Math.min(2, (tags.y() - 4 - descTop) / SMALL_LINE);
        }
        Rect desc = descLines >= 1 ? new Rect(left, descTop, width, descLines * SMALL_LINE)
                : Rect.EMPTY;
        return new Slots(icon, title, byline, desc, tags, button);
    }
}
