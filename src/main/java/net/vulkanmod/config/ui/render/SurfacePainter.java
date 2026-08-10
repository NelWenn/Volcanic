package net.vulkanmod.config.ui.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.vulkanmod.config.ui.core.Rect;

public interface SurfacePainter {

    void fill(Rect rect, int argb);

    void gradient(Rect rect, int topArgb, int bottomArgb);

    void text(int x, int y, String value, int argb, boolean shadow);

    void smallText(int x, int y, String value, int argb);

    float smallScale();

    void setOffset(int dx, int dy);

    void setAlpha(float alpha);

    void flush();

    static SurfacePainter create(GuiGraphics graphics, Font font) {
        return new FillSurfacePainter(graphics, font);
    }
}
