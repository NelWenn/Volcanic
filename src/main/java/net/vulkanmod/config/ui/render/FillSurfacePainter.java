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
    public void gradient(Rect rect, int topArgb, int bottomArgb) {
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Gradient(rect, topArgb, bottomArgb));
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
        switch (op) {
            case PaintOp.Fill(Rect rect, int argb) -> emitRect(rect, argb);
            case PaintOp.Gradient(Rect rect, int topArgb, int bottomArgb) ->
                    graphics.fillGradient(rect.x(), rect.y(), rect.right(), rect.bottom(), topArgb, bottomArgb);
            case PaintOp.Text(int x, int y, String value, int argb, boolean shadow) ->
                    graphics.drawString(font, value, x, y, argb, shadow);
        }
    }

    private void emitRect(Rect rect, int argb) {
        if (rect.isEmpty()) {
            return;
        }
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), argb);
    }
}
