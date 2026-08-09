package net.vulkanmod.config.ui.render;

import net.vulkanmod.config.ui.core.Rect;

public sealed interface PaintOp {

    enum Layer {
        SURFACE,
        TEXT
    }

    record Fill(Rect rect, int argb) implements PaintOp {
    }

    record Gradient(Rect rect, int topArgb, int bottomArgb) implements PaintOp {
    }

    record Text(int x, int y, String value, int argb, boolean shadow) implements PaintOp {
    }

    record SmallText(int x, int y, String value, int argb, float scale) implements PaintOp {
    }
}
