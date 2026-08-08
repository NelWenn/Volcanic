package net.vulkanmod.render.vtu;

import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import org.joml.Matrix4f;

public final class VtuJitter {

    private static int phase;
    private static int phaseCount = 8;
    private static float pixelX, pixelY;
    private static float prevPixelX, prevPixelY;
    private static int targetWidth, targetHeight;

    private VtuJitter() {
    }

    public static void applyToWorldProjection(Matrix4f worldProjection) {
        prevPixelX = pixelX;
        prevPixelY = pixelY;

        int width = 0;
        int height = 0;

        Renderer renderer = Renderer.getInstance();
        if (renderer != null && renderer.getMainPass() != null) {
            width = renderer.getMainPass().renderTargetWidth();
            height = renderer.getMainPass().renderTargetHeight();
        }

        targetWidth = width;
        targetHeight = height;

        boolean wanted = net.vulkanmod.render.vsr.Vsr.clampBackend(Initializer.CONFIG.vsrBackend) == net.vulkanmod.render.vsr.Vsr.VTU;

        if (!wanted || width <= 0 || height <= 0) {
            phase = 0;
            pixelX = 0.0f;
            pixelY = 0.0f;
            return;
        }

        phaseCount = resolvePhaseCount(width);
        phase = (phase + 1) % phaseCount;

        pixelX = halton(phase + 1, 2) - 0.5f;
        pixelY = halton(phase + 1, 3) - 0.5f;

        worldProjection.translateLocal(2.0f * pixelX / width, -2.0f * pixelY / height, 0.0f);
    }

    private static float halton(int index, int base) {
        float f = 1.0f;
        float r = 0.0f;

        for (int i = index; i > 0; i /= base) {
            f /= base;
            r += f * (i % base);
        }

        return r;
    }

    private static int resolvePhaseCount(int renderWidth) {
        int outputWidth = 0;

        try {
            outputWidth = Vulkan.getSwapChain().getWidth();
        } catch (Throwable ignored) {
        }

        if (renderWidth <= 0 || outputWidth <= 0) {
            return 8;
        }

        float ratio = (float) outputWidth / renderWidth;
        return Math.max(8, Math.min(64, (int) Math.ceil(8.0f * ratio * ratio)));
    }

    public static int phase() {
        return phase;
    }

    public static int phaseCount() {
        return phaseCount;
    }

    public static float pixelX() {
        return pixelX;
    }

    public static float pixelY() {
        return pixelY;
    }

    public static float prevPixelX() {
        return prevPixelX;
    }

    public static float prevPixelY() {
        return prevPixelY;
    }

    public static int targetWidth() {
        return targetWidth;
    }

    public static int targetHeight() {
        return targetHeight;
    }
}
