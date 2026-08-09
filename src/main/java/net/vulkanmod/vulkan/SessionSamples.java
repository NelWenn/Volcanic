package net.vulkanmod.vulkan;

import net.minecraft.client.Minecraft;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.ui.core.FrameSamples;

import java.util.Objects;

public final class SessionSamples {
    private static final FrameSamples SAMPLES = new FrameSamples();
    private static long lastNanos;

    private SessionSamples() {
    }

    public static FrameSamples samples() {
        return SAMPLES;
    }

    public static void onFrameEnd() {
        long now = System.nanoTime();
        long previous = lastNanos;
        lastNanos = now;

        if (!counts()) {
            return;
        }
        if (previous == 0L || now <= previous) {
            return;
        }
        SAMPLES.record(fingerprint(), (float) ((now - previous) / 1.0e6));
    }

    private static boolean counts() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.screen == null && minecraft.level != null
                && minecraft.isWindowActive() && !minecraft.isPaused();
    }

    private static int fingerprint() {
        Minecraft minecraft = Minecraft.getInstance();
        Config config = Initializer.CONFIG;
        return Objects.hash(
                config == null ? 0 : config.renderScale,
                config == null ? 0 : config.vsrPreset,
                config == null ? 0 : config.vsrBackend,
                config == null ? 0 : config.advCulling,
                config == null ? 0 : config.indirectDraw,
                config == null ? 0 : config.uniqueOpaqueLayer,
                config == null ? 0 : config.ambientOcclusion,
                config == null ? 0 : config.shadersEnabled,
                config == null ? null : config.selectedShader,
                minecraft.options.renderDistance().get(),
                minecraft.options.graphicsMode().get(),
                minecraft.level.dimension().location(),
                Vulkan.getSwapChain() == null ? 0 : Vulkan.getSwapChain().getWidth(),
                Vulkan.getSwapChain() == null ? 0 : Vulkan.getSwapChain().getHeight());
    }
}
