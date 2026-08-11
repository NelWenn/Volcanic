package net.vulkanmod.vulkan;

import net.minecraft.client.Minecraft;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.ui.core.FrameSamples;

import java.util.Objects;

public final class SessionSamples {
    private static final FrameSamples SAMPLES = new FrameSamples();
    private static final net.vulkanmod.config.ui.core.FrameHistory HISTORY =
            new net.vulkanmod.config.ui.core.FrameHistory();
    private static long lastGcMs;
    private static long lastNanos;

    private SessionSamples() {
    }

    public static FrameSamples samples() {
        return SAMPLES;
    }

    public static int contextFingerprint() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return 0;
        }
        return Objects.hash(
                minecraft.level == null ? null : minecraft.level.dimension().location(),
                Vulkan.getSwapChain() == null ? 0 : Vulkan.getSwapChain().getWidth(),
                Vulkan.getSwapChain() == null ? 0 : Vulkan.getSwapChain().getHeight(),
                Initializer.CONFIG == null ? null : Initializer.CONFIG.selectedShader,
                net.neoforged.fml.ModList.get().size());
    }

    public static boolean describesCurrent() {
        return SAMPLES.count() > 0 && SAMPLES.fingerprint() == fingerprint();
    }

    public static void onFrameEnd() {
        long now = System.nanoTime();
        long previous = lastNanos;
        lastNanos = now;

        if (previous == 0L || now <= previous) {
            return;
        }
        float frameMs = (float) ((now - previous) / 1.0e6);
        boolean playing = counts();
        net.vulkanmod.render.profiling.StackSampler.setGameplay(playing);
        net.vulkanmod.render.profiling.StackSampler.watch(Thread.currentThread().threadId());
        if (!playing && !charts()) {
            return;
        }
        if (playing) {
            SAMPLES.record(fingerprint(), frameMs);
        }
        long nowMs = System.currentTimeMillis();
        long gcMs = totalGcMillis();
        Runtime runtime = Runtime.getRuntime();
        HISTORY.record(nowMs, frameMs, gcMs - lastGcMs,
                net.vulkanmod.render.profiling.RenderCounters.uploadsLastFrame(),
                net.vulkanmod.render.profiling.RenderCounters.pipelineBuilds(),
                (runtime.totalMemory() - runtime.freeMemory()) / 1048576.0f);
        lastGcMs = gcMs;
        snapshotTerrain();
    }

    public static net.vulkanmod.config.ui.core.FrameHistory history() {
        return HISTORY;
    }

    private static long totalGcMillis() {
        long total = 0L;
        for (java.lang.management.GarbageCollectorMXBean bean
                : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            long collected = bean.getCollectionTime();
            if (collected > 0L) {
                total += collected;
            }
        }
        return total;
    }

    private static void snapshotTerrain() {
        try {
            net.vulkanmod.render.chunk.WorldRenderer renderer =
                    net.vulkanmod.render.chunk.WorldRenderer.getInstance();
            if (renderer == null) {
                return;
            }
            net.vulkanmod.render.chunk.build.TaskDispatcher dispatcher = renderer.getTaskDispatcher();
            net.vulkanmod.render.profiling.RenderCounters.snapshotTerrain(
                    dispatcher == null ? -1 : dispatcher.pendingTaskCount(),
                    dispatcher == null ? -1 : dispatcher.pendingUploadCount(),
                    dispatcher == null ? -1 : dispatcher.idleThreadCount(),
                    dispatcher == null ? -1 : dispatcher.threadCount(),
                    renderer.getVisibleSectionsCount());
        } catch (Throwable unavailable) {
            // the counters simply stay at their previous value
        }
    }

    private static boolean charts() {
        Config config = Initializer.CONFIG;
        if (config == null || !config.statsSampleInMenus) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.screen != null && minecraft.level != null
                && minecraft.isWindowActive();
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
