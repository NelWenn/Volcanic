package net.vulkanmod.vulkan;

import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.device.DeviceManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkQueryPoolCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * In-engine GPU/CPU frame timer for perf diagnostics. GPU time is measured with two Vulkan
 * timestamp queries per frame-in-flight (start/end of the command buffer). Enabled only when
 * {@link MoltenVKConfig#perfDiagEnabled()} is true.
 */
public final class FrameTimer {

    private static FrameTimer INSTANCE;

    public static FrameTimer instance() { return INSTANCE; }

    /** Create the timer (no-op unless perf diagnostics are enabled). Call once, after the device exists. */
    public static double gpuMs() {
        return INSTANCE != null ? INSTANCE.smoothGpuMs : -1.0;
    }

    public static double frameMs() {
        return INSTANCE != null ? INSTANCE.smoothWallMs : -1.0;
    }

    public static void init(int framesNum) {
        if (INSTANCE != null) return;
        try {
            INSTANCE = new FrameTimer(framesNum);
            INSTANCE.logging = MoltenVKConfig.perfDiagEnabled();
            Initializer.LOGGER.info("FrameTimer: {} ({} frames, timestampPeriod={} ns, logging={})",
                    GPU_TIMESTAMPS ? "wall + GPU timestamps" : "wall only, GPU timestamps off",
                    framesNum, INSTANCE.timestampPeriod, INSTANCE.logging);
        } catch (Throwable t) {
            Initializer.LOGGER.warn("FrameTimer: disabled ({})", t.toString());
            INSTANCE = null;
        }
    }

    public static void destroy() {
        if (INSTANCE != null) {
            INSTANCE.cleanup();
            INSTANCE = null;
        }
    }

    private static final long REPORT_INTERVAL_NS = 2_000_000_000L;
    private static final boolean GPU_TIMESTAMPS =
            !"false".equalsIgnoreCase(System.getProperty("vulkanmod.perf.timestamps", "true"));

    private final int framesNum;
    private final long queryPool;
    private final float timestampPeriod;      // ns per timestamp tick
    private final boolean[] written;          // timestamps recorded into this slot last cycle?

    private long frameStartNanos = 0;
    private long lastFrameStartNanos = 0;

    // render-thread CPU time during the span (busy vs blocked, vs wall time)
    private static final java.lang.management.ThreadMXBean THREAD_MX = java.lang.management.ManagementFactory.getThreadMXBean();
    private long threadCpuStartNanos = -1;
    private double accCpuBusyMs;

    // accumulators for the periodic report
    private double accWallMs, accCpuMs, accGpuMs;
    private double accUploadMs, accTerrainMs, accSetupMs;
    private int samples;
    private long lastReportNanos = 0;
    private boolean anyGpuSample = false;
    private boolean logging = false;
    private double smoothGpuMs = -1.0;
    private double smoothWallMs = -1.0;
    private long lastGcMillis = -1; // cumulative JVM GC time, delta'd per window

    private FrameTimer(int framesNum) {
        this.framesNum = framesNum;
        this.written = new boolean[framesNum];
        this.timestampPeriod = DeviceManager.deviceProperties.limits().timestampPeriod();

        if (!GPU_TIMESTAMPS) {
            this.queryPool = VK_NULL_HANDLE;
            return;
        }

        try (MemoryStack stack = stackPush()) {
            VkQueryPoolCreateInfo info = VkQueryPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_QUERY_POOL_CREATE_INFO)
                    .queryType(VK_QUERY_TYPE_TIMESTAMP)
                    .queryCount(framesNum * 2);
            java.nio.LongBuffer pPool = stack.mallocLong(1);
            Vulkan.checkResult(vkCreateQueryPool(DeviceManager.vkDevice, info, null, pPool),
                    "Failed to create timestamp query pool");
            this.queryPool = pPool.get(0);
        }
    }

    /** Called at frame start (after the fence signalled). Reads back this slot's previous GPU time. */
    public void onBeginFrame(int frame) {
        long now = System.nanoTime();

        double wallMs = lastFrameStartNanos == 0 ? 0 : (now - lastFrameStartNanos) / 1.0e6;
        lastFrameStartNanos = now;
        frameStartNanos = now;

        threadCpuStartNanos = THREAD_MX.isCurrentThreadCpuTimeSupported() ? THREAD_MX.getCurrentThreadCpuTime() : -1;

        double gpuMs = -1;
        if (written[frame]) {
            gpuMs = readGpuMs(frame);
            // consume it so a skipped frame isn't re-read stale next cycle
            written[frame] = false;
        }

        if (wallMs > 0) {
            accWallMs += wallMs;
            smoothWallMs = smoothWallMs < 0 ? wallMs : smoothWallMs * 0.9 + wallMs * 0.1;
            if (gpuMs >= 0) {
                accGpuMs += gpuMs;
                anyGpuSample = true;
                smoothGpuMs = smoothGpuMs < 0 ? gpuMs : smoothGpuMs * 0.9 + gpuMs * 0.1;
            }
            // cpuMs is added at endFrame, paired with this wall sample
            pendingWall = true;
        }

        maybeReport(now);
    }

    private boolean pendingWall = false;

    /** Record the CPU render span for the frame that is ending, completing the current sample. */
    public void onEndFrameCpu() {
        if (!pendingWall) return;
        double cpuMs = (System.nanoTime() - frameStartNanos) / 1.0e6;
        accCpuMs += cpuMs;
        if (threadCpuStartNanos >= 0) {
            accCpuBusyMs += (THREAD_MX.getCurrentThreadCpuTime() - threadCpuStartNanos) / 1.0e6;
        }
        samples++;
        pendingWall = false;
    }

    /** Chunk upload phase time (runs in preInitFrame). */
    public void addUploadNanos(long nanos) { accUploadMs += nanos / 1.0e6; }

    /** Terrain draw-recording phase time. */
    public void addTerrainNanos(long nanos) { accTerrainMs += nanos / 1.0e6; }

    /** Visibility/culling build time (WorldRenderer.setupRenderer). */
    public void addSetupNanos(long nanos) { accSetupMs += nanos / 1.0e6; }

    /** Reset this slot's queries and write the frame-start timestamp. Must NOT be inside a render pass. */
    public void cmdBeginTimestamp(org.lwjgl.vulkan.VkCommandBuffer cmd, int frame) {
        if (queryPool == VK_NULL_HANDLE) {
            return;
        }
        int base = frame * 2;
        vkCmdResetQueryPool(cmd, queryPool, base, 2);
        vkCmdWriteTimestamp(cmd, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, queryPool, base);
    }

    /** Write the frame-end timestamp. Safe to call inside a render pass. */
    public void cmdEndTimestamp(org.lwjgl.vulkan.VkCommandBuffer cmd, int frame) {
        if (queryPool == VK_NULL_HANDLE) {
            return;
        }
        vkCmdWriteTimestamp(cmd, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, queryPool, frame * 2 + 1);
        written[frame] = true;
    }

    private double readGpuMs(int frame) {
        if (queryPool == VK_NULL_HANDLE) {
            return -1;
        }
        try (MemoryStack stack = stackPush()) {
            LongBuffer results = stack.mallocLong(2);
            int r = vkGetQueryPoolResults(DeviceManager.vkDevice, queryPool, frame * 2, 2,
                    results, 8L, VK_QUERY_RESULT_64_BIT);
            if (r != VK_SUCCESS) return -1;
            long ticks = results.get(1) - results.get(0);
            if (ticks <= 0) return -1;
            return (ticks * (double) timestampPeriod) / 1.0e6;
        }
    }

    private void maybeReport(long now) {
        if (lastReportNanos == 0) { lastReportNanos = now; return; }
        if (now - lastReportNanos < REPORT_INTERVAL_NS || samples < 1) return;

        if (!this.logging) {
            net.vulkanmod.vulkan.shader.GraphicsPipeline.consumeBuilds();
            net.vulkanmod.vulkan.shader.GraphicsPipeline.consumeBuildMs();
            net.vulkanmod.vulkan.framebuffer.RenderPass.consumeCreations();
            net.vulkanmod.vulkan.pass.DefaultMainPass.consumeTargetSwitches();
            net.vulkanmod.render.vsr.Vsr.consumePasses();

            accWallMs = accCpuMs = accGpuMs = accCpuBusyMs = 0;
            accUploadMs = accTerrainMs = accSetupMs = 0;
            samples = 0;
            anyGpuSample = false;
            lastReportNanos = now;
            return;
        }

        double wall = accWallMs / samples;
        double cpu = accCpuMs / samples;
        double gpu = anyGpuSample ? accGpuMs / samples : -1;
        double fps = wall > 0 ? 1000.0 / wall : 0;

        double cpuBusy = accCpuBusyMs / samples;

        // compare busy CPU work and GPU time, not the wall span; mostly-idle thread => pipeline/GPU wait
        double idle = Math.max(0, wall - cpuBusy);
        String bound;
        if (gpu >= 0 && gpu >= cpuBusy && gpu >= 0.60 * wall)
            bound = "GPU / command overhead (does NOT scale with resolution)";
        else if (cpuBusy >= 0.70 * wall)
            bound = "CPU compute (render thread)";
        else
            bound = String.format("pipeline/GPU wait (thread idle %.1f of %.1fms)", idle, wall);
        Initializer.LOGGER.info(String.format(
                "Perf: FPS=%.0f  frame=%.2fms  CPU-render(wall)=%.2fms  CPU-busy=%.2fms  GPU=%s  -> bound=%s",
                fps, wall, cpu, cpuBusy, gpu >= 0 ? String.format("%.2fms", gpu) : "n/a", bound));

        // render-thread breakdown: terrain vs the rest of the span, plus chunk upload
        double upload = accUploadMs / samples;
        double terrain = accTerrainMs / samples;
        double setup = accSetupMs / samples;
        double renderOther = Math.max(0, cpu - terrain - setup);

        // GC time over this window
        long gcNow = totalGcMillis();
        double gcMsPerFrame = (lastGcMillis < 0) ? 0 : (double) (gcNow - lastGcMillis) / samples;
        lastGcMillis = gcNow;

        Initializer.LOGGER.info(String.format(
                "  breakdown: upload=%.2fms  setup/cull=%.2fms  terrain=%.2fms  entities/BE/GUI=%.2fms  |  GC=%.2fms/frame",
                upload, setup, terrain, renderOther, gcMsPerFrame));

        int pipelineBuilds = net.vulkanmod.vulkan.shader.GraphicsPipeline.consumeBuilds();
        double pipelineBuildMs = net.vulkanmod.vulkan.shader.GraphicsPipeline.consumeBuildMs();
        int renderPassCreations = net.vulkanmod.vulkan.framebuffer.RenderPass.consumeCreations();

        Initializer.LOGGER.info(String.format(
                "  pipelines: builds=%.1f/frame  buildTime=%.2fms/frame  liveVariants=%d  renderPassCreations=%.1f/frame",
                pipelineBuilds / (double) samples, pipelineBuildMs / samples,
                net.vulkanmod.vulkan.shader.GraphicsPipeline.totalVariants(),
                renderPassCreations / (double) samples));

        int targetSwitches = net.vulkanmod.vulkan.pass.DefaultMainPass.consumeTargetSwitches();

        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            com.mojang.blaze3d.platform.Window win = mc.getWindow();
            Initializer.LOGGER.info(String.format(
                    "  renderScale: config=%d%%  %s  targetSwitches=%.1f/frame  mcFramebuffer=%dx%d  fpsLimit=%d  vsrBackend=%d  vsrPasses=%.1f/frame",
                    net.vulkanmod.config.RenderScale.clamp(Initializer.CONFIG.renderScale),
                    Renderer.getInstance().getMainPass().renderScaleStatus(),
                    targetSwitches / (double) samples,
                    win.getWidth(), win.getHeight(),
                    mc.options.framerateLimit().get(),
                    net.vulkanmod.render.vsr.Vsr.getLastBackend(),
                    net.vulkanmod.render.vsr.Vsr.consumePasses() / (double) samples));
        } catch (Throwable ignored) {}

        // post-shader settings snapshot, so windows with different settings aren't compared blind
        try {
            net.vulkanmod.config.Config cfg = Initializer.CONFIG;
            if (cfg.shadersEnabled) {
                String shadows = cfg.shadowsEnabled
                        ? String.format("on(%d,r%d)",
                                net.vulkanmod.vulkan.pass.ShadowMap.currentResolution(),
                                cfg.shadowDistance)
                        : "off";
                Initializer.LOGGER.info(String.format(
                        "  post: shader=%s shadows=%s scale=%d",
                        cfg.selectedShader, shadows, cfg.renderScale));
            }
        } catch (Throwable ignored) {}

        accWallMs = accCpuMs = accGpuMs = accCpuBusyMs = 0;
        accUploadMs = accTerrainMs = accSetupMs = 0;
        samples = 0;
        anyGpuSample = false;
        lastReportNanos = now;
    }

    private static long totalGcMillis() {
        long t = 0;
        for (java.lang.management.GarbageCollectorMXBean b : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            long ct = b.getCollectionTime();
            if (ct > 0) t += ct;
        }
        return t;
    }

    private void cleanup() {
        if (queryPool != VK_NULL_HANDLE) {
            vkDestroyQueryPool(DeviceManager.vkDevice, queryPool, null);
        }
    }
}
