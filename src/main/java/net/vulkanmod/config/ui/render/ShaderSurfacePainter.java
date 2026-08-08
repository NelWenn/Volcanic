package net.vulkanmod.config.ui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.vulkanmod.config.ui.core.Rect;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class ShaderSurfacePainter implements SurfacePainter {
    private static final int EDGE_PAD = 2;
    private static final int MAX_GLOW_RADIUS = 255;

    private final PaintQueue queue = new PaintQueue();
    private final List<PaintOp.RoundedSurface> surfaces = new ArrayList<>();
    private final List<PaintOp.Text> texts = new ArrayList<>();
    private final GuiGraphics graphics;
    private final Font font;

    public ShaderSurfacePainter(GuiGraphics graphics, Font font) {
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
        surfaces.clear();
        texts.clear();

        for (PaintOp op : queue.drain()) {
            if (op instanceof PaintOp.Fill fill) {
                graphics.fill(fill.rect().x(), fill.rect().y(), fill.rect().right(), fill.rect().bottom(), fill.argb());
            } else if (op instanceof PaintOp.RoundedSurface surface) {
                surfaces.add(surface);
            } else if (op instanceof PaintOp.Text text) {
                texts.add(text);
            }
        }

        drawSurfaces();

        for (PaintOp.Text text : texts) {
            graphics.drawString(font, text.value(), text.x(), text.y(), text.argb(), text.shadow());
        }

        surfaces.clear();
        texts.clear();
    }

    private void drawSurfaces() {
        if (surfaces.isEmpty()) {
            return;
        }

        if (!GuiSurfacePipeline.isAvailable()) {
            drawSurfacesAsFills();
            return;
        }

        try {
            graphics.flush();
            emitBatch();
        } catch (Throwable throwable) {
            recoverFromDrawFailure(throwable);
        }
    }

    private void emitBatch() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GuiSurfacePipeline::shader);

        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, GuiSurfacePipeline.format());

        for (PaintOp.RoundedSurface surface : surfaces) {
            emitSurface(builder, pose, surface);
        }

        MeshData mesh = builder.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private void recoverFromDrawFailure(Throwable throwable) {
        try {
            Tesselator.getInstance().clear();
        } catch (Throwable ignored) {
        }

        GuiSurfacePipeline.markUnavailable("a rounded surface batch failed to draw", throwable);
        drawSurfacesAsFills();
    }

    private void drawSurfacesAsFills() {
        SurfacePainter fallback = new FillSurfacePainter(graphics, font);
        for (PaintOp.RoundedSurface surface : surfaces) {
            fallback.surface(surface.rect(), surface.radius(), surface.fillArgb(), surface.borderArgb(),
                    surface.glowArgb(), surface.glowRadius());
        }
        fallback.flush();
    }

    private static void emitSurface(BufferBuilder builder, Matrix4f pose, PaintOp.RoundedSurface surface) {
        Rect rect = surface.rect();
        int width = rect.width();
        int height = rect.height();
        int radius = clamp(surface.radius(), 0, Math.min(width, height) / 2);
        int glowRadius = clamp(surface.glowRadius(), 0, MAX_GLOW_RADIUS);

        float centerX = rect.x() + width * 0.5f;
        float centerY = rect.y() + height * 0.5f;

        if (glowRadius > 0 && alpha(surface.glowArgb()) != 0) {
            emitLayer(builder, pose, rect, glowRadius, centerX, centerY, width, height, radius, glowRadius,
                    surface.glowArgb(), 1.0f, 0.0f, 0.0f);
        }

        if (alpha(surface.fillArgb()) != 0) {
            emitLayer(builder, pose, rect, EDGE_PAD, centerX, centerY, width, height, radius, glowRadius,
                    surface.fillArgb(), 0.0f, 1.0f, 0.0f);
        }

        if (surface.borderArgb() != 0) {
            emitLayer(builder, pose, rect, EDGE_PAD, centerX, centerY, width, height, radius, glowRadius,
                    surface.borderArgb(), 0.0f, 0.0f, 1.0f);
        }
    }

    private static void emitLayer(BufferBuilder builder, Matrix4f pose, Rect rect, int pad,
                                  float centerX, float centerY, int width, int height,
                                  int radius, int glowRadius, int argb,
                                  float modeGlow, float modeFill, float modeBorder) {
        float left = rect.x() - pad;
        float top = rect.y() - pad;
        float right = rect.right() + pad;
        float bottom = rect.bottom() + pad;

        emitVertex(builder, pose, left, top, centerX, centerY, width, height, radius, glowRadius, argb,
                modeGlow, modeFill, modeBorder);
        emitVertex(builder, pose, left, bottom, centerX, centerY, width, height, radius, glowRadius, argb,
                modeGlow, modeFill, modeBorder);
        emitVertex(builder, pose, right, bottom, centerX, centerY, width, height, radius, glowRadius, argb,
                modeGlow, modeFill, modeBorder);
        emitVertex(builder, pose, right, top, centerX, centerY, width, height, radius, glowRadius, argb,
                modeGlow, modeFill, modeBorder);
    }

    private static void emitVertex(BufferBuilder builder, Matrix4f pose, float x, float y,
                                   float centerX, float centerY, int width, int height,
                                   int radius, int glowRadius, int argb,
                                   float modeGlow, float modeFill, float modeBorder) {
        builder.addVertex(pose, x, y, 0.0f)
                .setColor(argb)
                .setUv(x - centerX, y - centerY)
                .setUv1(width, height)
                .setUv2(radius, glowRadius)
                .setNormal(modeGlow, modeFill, modeBorder);
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
