package net.vulkanmod.config.ui.render;

import net.vulkanmod.config.ui.core.Rect;

public sealed interface PaintOp {

    enum Layer {
        SURFACE,
        BORDER,
        GLOW,
        TEXT
    }

    record Fill(Rect rect, int argb) implements PaintOp {
    }

    record RoundedSurface(Rect rect, int radius, int fillArgb, int borderArgb,
                          int glowArgb, int glowRadius) implements PaintOp {
    }

    record Text(int x, int y, String value, int argb, boolean shadow) implements PaintOp {
    }
}
