package net.vulkanmod.config.ui.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.vulkanmod.config.ui.core.Motion;
import net.vulkanmod.config.ui.core.Rect;
import net.vulkanmod.config.ui.core.TextScale;

import java.util.List;

public final class FillSurfacePainter implements SurfacePainter {
    private final PaintQueue queue = new PaintQueue();
    private final GuiGraphics graphics;
    private final Font font;
    private int offsetX;
    private int offsetY;
    private float alpha = 1.0f;

    public FillSurfacePainter(GuiGraphics graphics, Font font) {
        this.graphics = graphics;
        this.font = font;
    }

    @Override
    public void setOffset(int dx, int dy) {
        this.offsetX = dx;
        this.offsetY = dy;
    }

    @Override
    public void setAlpha(float alpha) {
        this.alpha = alpha < 0.0f ? 0.0f : Math.min(alpha, 1.0f);
    }

    @Override
    public void fill(Rect rect, int argb) {
        queue.record(PaintOp.Layer.SURFACE, new PaintOp.Fill(shift(rect), tint(argb)));
    }

    @Override
    public void gradient(Rect rect, int topArgb, int bottomArgb) {
        queue.record(PaintOp.Layer.SURFACE,
                new PaintOp.Gradient(shift(rect), tint(topArgb), tint(bottomArgb)));
    }

    @Override
    public void text(int x, int y, String value, int argb, boolean shadow) {
        queue.record(PaintOp.Layer.TEXT,
                new PaintOp.Text(x + offsetX, y + offsetY, value, tint(argb), shadow));
    }

    @Override
    public void smallText(int x, int y, String value, int argb) {
        queue.record(PaintOp.Layer.TEXT,
                new PaintOp.SmallText(x + offsetX, y + offsetY, value, tint(argb), smallScale()));
    }

    private Rect shift(Rect rect) {
        return offsetX == 0 && offsetY == 0 ? rect : rect.translated(offsetX, offsetY);
    }

    private int tint(int argb) {
        return alpha >= 1.0f ? argb : Motion.fade(argb, alpha);
    }

    @Override
    public float smallScale() {
        return TextScale.small(net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale());
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
            case PaintOp.SmallText(int x, int y, String value, int argb, float scale) ->
                    emitSmall(x, y, value, argb, scale);
        }
    }

    private void emitSmall(int x, int y, String value, int argb, float scale) {
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, value, Math.round(x / scale), Math.round(y / scale), argb, false);
        graphics.pose().popPose();
    }

    private void emitRect(Rect rect, int argb) {
        if (rect.isEmpty()) {
            return;
        }
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), argb);
    }
}
