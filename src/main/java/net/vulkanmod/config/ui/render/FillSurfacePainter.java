package net.vulkanmod.config.ui.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.vulkanmod.config.ui.core.Rect;

import java.util.List;

public final class FillSurfacePainter implements SurfacePainter {
    private final PaintQueue queue = new PaintQueue();
    private final GuiGraphics graphics;
    private final Font font;

    public FillSurfacePainter(GuiGraphics graphics, Font font) {
        this.graphics = graphics;
        this.font = font;
    }

    @Override
    public void fill(Rect rect, int argb) {
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(rect, argb));
    }

    @Override
    public void surface(Rect rect, int radius, int fillArgb, int borderArgb, int glowArgb, int glowRadius) {
        queue.record(PaintOp.Layer.SURFACE,
                new PaintOp.RoundedSurface(rect, radius, fillArgb, borderArgb, glowArgb, glowRadius));
    }

    @Override
    public void text(int x, int y, String value, int argb, boolean shadow) {
        queue.record(PaintOp.Layer.TEXT, new PaintOp.Text(x, y, value, argb, shadow));
    }

    @Override
    public void flush() {
        List<PaintOp> ops = queue.drain();
        for (PaintOp op : ops) {
            emit(op);
        }
    }

    private void emit(PaintOp op) {
        if (op instanceof PaintOp.Fill fill) {
            emitRect(fill.rect(), fill.argb());
        } else if (op instanceof PaintOp.RoundedSurface surface) {
            emitRoundedSurface(surface);
        } else if (op instanceof PaintOp.Text text) {
            graphics.drawString(font, text.value(), text.x(), text.y(), text.argb(), text.shadow());
        }
    }

    private void emitRoundedSurface(PaintOp.RoundedSurface surface) {
        Rect rect = surface.rect();
        int radius = Math.max(0, Math.min(surface.radius(), Math.min(rect.width(), rect.height()) / 2));
        List<Rect> fillRects = decomposeRoundedFill(rect, surface.radius());

        for (Rect fillRect : fillRects) {
            emitRect(fillRect, surface.fillArgb());
        }

        if (surface.borderArgb() == 0) {
            return;
        }

        if (fillRects.size() == 3) {
            emitRect(new Rect(rect.x() + radius, rect.y(), rect.width() - radius * 2, 1), surface.borderArgb());
            emitRect(new Rect(rect.x() + radius, rect.bottom() - 1, rect.width() - radius * 2, 1),
                    surface.borderArgb());
            emitRect(new Rect(rect.x(), rect.y() + radius, 1, rect.height() - radius * 2), surface.borderArgb());
            emitRect(new Rect(rect.right() - 1, rect.y() + radius, 1, rect.height() - radius * 2),
                    surface.borderArgb());
        } else {
            emitRect(new Rect(rect.x(), rect.y(), rect.width(), 1), surface.borderArgb());
            emitRect(new Rect(rect.x(), rect.bottom() - 1, rect.width(), 1), surface.borderArgb());
            emitRect(new Rect(rect.x(), rect.y(), 1, rect.height()), surface.borderArgb());
            emitRect(new Rect(rect.right() - 1, rect.y(), 1, rect.height()), surface.borderArgb());
        }
    }

    static List<Rect> decomposeRoundedFill(Rect rect, int radius) {
        if (rect.isEmpty()) {
            return List.of();
        }

        int clampedRadius = Math.max(0, Math.min(radius, Math.min(rect.width(), rect.height()) / 2));
        if (clampedRadius == 0) {
            return List.of(rect);
        }
        if (rect.width() < 3 * clampedRadius || rect.height() < 3 * clampedRadius) {
            return List.of(rect);
        }

        return List.of(
                new Rect(rect.x() + clampedRadius, rect.y(), rect.width() - clampedRadius * 2, rect.height()),
                new Rect(rect.x(), rect.y() + clampedRadius, clampedRadius, rect.height() - clampedRadius * 2),
                new Rect(rect.right() - clampedRadius, rect.y() + clampedRadius, clampedRadius,
                        rect.height() - clampedRadius * 2)
        );
    }

    private void emitRect(Rect rect, int argb) {
        if (rect.isEmpty()) {
            return;
        }
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), argb);
    }
}
