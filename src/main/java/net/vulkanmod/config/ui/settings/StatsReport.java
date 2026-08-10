package net.vulkanmod.config.ui.settings;

import net.minecraft.client.Minecraft;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.build.TaskDispatcher;
import net.vulkanmod.render.profiling.RenderCounters;
import net.vulkanmod.render.profiling.Telemetry;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.memory.MemoryManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StatsReport {
    public record Cell(String labelKey, String value, String note, boolean alert) {
    }

    private static final long MB = 1024L * 1024L;

    private StatsReport() {
    }

    public static List<Cell> fingerprint() {
        List<Cell> cells = new ArrayList<>();
        String gpu = gpuName();
        String chip = chipName();
        add(cells, "vulkanmod.overview.gpu", gpu,
                chip == null || chip.equals(gpu) ? "rendering device" : chip, false);
        add(cells, "vulkanmod.ui.developer.info.vk_instance", vkVersion(), "instance API", false);
        add(cells, "vulkanmod.ui.stats.pack", shaderPack(), shaderNote(), false);
        add(cells, "vulkanmod.ui.stats.render_size", renderSize(), "internal resolution", false);
        add(cells, "vulkanmod.ui.stats.distance", distance(), "simulation follows it", false);
        add(cells, "vulkanmod.ui.developer.info.present_mode", presentMode(),
                uncapped() ? "uncapped - frame time is honest" : "vsync caps the frame time",
                uncapped());
        return List.copyOf(cells);
    }

    public static List<Cell> scene() {
        List<Cell> cells = new ArrayList<>();
        int draws = RenderCounters.terrainDraws();
        int visible = RenderCounters.visibleSections();
        add(cells, "vulkanmod.ui.stats.sections", count(visible), loadedSections(), false);
        add(cells, "vulkanmod.ui.stats.terrain_draws", count(draws),
                draws > 0 && visible > 0
                        ? String.format(Locale.ROOT, "%.1f sections per call", visible / (float) draws)
                        : null, false);
        add(cells, "vulkanmod.ui.stats.pipelines", count(RenderCounters.boundPipelines()),
                "state changes cost on MoltenVK", false);
        add(cells, "vulkanmod.ui.stats.entities", entities(), "in the loaded world", false);
        add(cells, "vulkanmod.ui.stats.particles", particles(), "alive", false);
        return List.copyOf(cells);
    }

    public static List<Cell> terrain() {
        List<Cell> cells = new ArrayList<>();
        int pendingUploads = RenderCounters.uploadQueue();
        int idle = RenderCounters.idleBuilders();
        int total = RenderCounters.builders();
        add(cells, "vulkanmod.ui.stats.mesh_queue", count(RenderCounters.meshQueue()),
                "last played frame", false);
        add(cells, "vulkanmod.ui.stats.upload_queue",
                pendingUploads < 0 ? null : pendingUploads + " / 128",
                pendingUploads >= 120 ? "saturated - builders parked" : "last played frame",
                pendingUploads >= 120);
        add(cells, "vulkanmod.ui.stats.uploads_frame", count(RenderCounters.uploadsLastFrame()),
                "budget " + Initializer.CONFIG.chunkUploadsPerFrame, false);
        add(cells, total < 0 ? "vulkanmod.ui.stats.builders" : "vulkanmod.ui.stats.builders",
                total < 0 ? null : idle + " / " + total,
                total > 0 && idle == 0 ? "all busy" : "idle / total",
                total > 0 && idle == 0);
        return List.copyOf(cells);
    }

    public static List<Cell> memory(float allocationMbPerSecond) {
        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / MB;
        long max = runtime.maxMemory() / MB;
        List<Cell> cells = new ArrayList<>();
        add(cells, "vulkanmod.ui.developer.info.heap", used + " / " + max + " MB",
                max > 0 ? used * 100 / max + "% of the limit" : null,
                max > 0 && used * 100 / max > 85);
        add(cells, "vulkanmod.ui.stats.vram", vram(), "tracked by Volcanic", false);
        add(cells, "vulkanmod.ui.stats.host_memory", hostMemory(), "staging and uniforms", false);
        add(cells, "vulkanmod.ui.stats.gc", gcTotal(), "since launch", false);
        add(cells, "vulkanmod.ui.stats.alloc",
                allocationMbPerSecond < 0.0f ? "-"
                        : String.format(Locale.ROOT, "%.0f MB/s", allocationMbPerSecond),
                allocationMbPerSecond < 0.0f ? "needs a few seconds of play"
                        : "what schedules the collections",
                allocationMbPerSecond >= 400.0f);
        return List.copyOf(cells);
    }

    public static List<Cell> machine() {
        Telemetry.Snapshot snapshot = Telemetry.latest();
        List<Cell> cells = new ArrayList<>();
        if (snapshot.dieTemp() > 0.0) {
            add(cells, snapshot.unifiedDie() ? "vulkanmod.ui.stats.die" : "vulkanmod.ui.stats.cpu_temp",
                    String.format(Locale.ROOT, "%.0f °C", snapshot.dieTemp()),
                    snapshot.unifiedDie() ? chipName() : null, snapshot.dieTemp() > 95.0);
        }
        if (!snapshot.unifiedDie() && snapshot.gpuTemp() > 0.0) {
            add(cells, "vulkanmod.ui.stats.gpu_temp",
                    String.format(Locale.ROOT, "%.0f °C", snapshot.gpuTemp()), null,
                    snapshot.gpuTemp() > 90.0);
        }
        if (snapshot.fanRpm() > 0) {
            add(cells, "vulkanmod.ui.stats.fans", snapshot.fanRpm() + " rpm",
                    "reacts before the die does", false);
        }
        if (snapshot.systemLoad() >= 0.0) {
            add(cells, "vulkanmod.ui.stats.cpu_load",
                    Math.round(snapshot.systemLoad() * 100) + "%",
                    snapshot.processCores() >= 0.0
                            ? String.format(Locale.ROOT, "%.1f cores", snapshot.processCores()) : null,
                    false);
        }
        if (snapshot.totalMemoryBytes() > 0L) {
            add(cells, "vulkanmod.ui.stats.system_memory",
                    snapshot.freeMemoryBytes() / (1024L * MB) + " / "
                            + snapshot.totalMemoryBytes() / (1024L * MB) + " GB",
                    snapshot.swapBytes() > 0L ? "swap " + snapshot.swapBytes() / MB + " MB" : null,
                    snapshot.swapBytes() > 0L);
        }
        if (snapshot.threads() > 0) {
            add(cells, "vulkanmod.ui.stats.threads",
                    snapshot.threads() + " / " + snapshot.peakThreads(), "live / peak", false);
        }
        if (snapshot.uptimeMs() > 0L) {
            add(cells, "vulkanmod.ui.stats.uptime", snapshot.uptimeMs() / 60_000L + " min",
                    "readings are not comparable across sessions", false);
        }
        return List.copyOf(cells);
    }

    public static boolean unifiedDie() {
        try {
            return DeviceManager.deviceProperties == null
                    || DeviceManager.deviceProperties.deviceType() != 2;
        } catch (Throwable unavailable) {
            return true;
        }
    }

    private static void add(List<Cell> cells, String labelKey, String value, String note, boolean alert) {
        if (value != null && !value.isBlank()) {
            cells.add(new Cell(labelKey, value, note, alert));
        }
    }

    private static String shaderNote() {
        try {
            if (!Initializer.CONFIG.shadersEnabled || Initializer.CONFIG.selectedShader == null) {
                return "vanilla rendering";
            }
            return Initializer.CONFIG.shadowsEnabled
                    ? "shadows " + Initializer.CONFIG.shadowDistance + " blocks" : "no shadows";
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static String chipName() {
        try {
            return net.vulkanmod.vulkan.SystemInfo.cpuInfo;
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static String gpuName() {
        return DeviceManager.device == null ? null : DeviceManager.device.deviceName;
    }

    private static String vkVersion() {
        return DeviceManager.device == null ? null : DeviceManager.device.vkVersion;
    }

    private static String shaderPack() {
        try {
            return Initializer.CONFIG.shadersEnabled && Initializer.CONFIG.selectedShader != null
                    ? Initializer.CONFIG.selectedShader : "none";
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static String renderSize() {
        SwapChain swapChain = swapChain();
        return swapChain == null || swapChain.getWidth() <= 0 ? null
                : swapChain.getWidth() + " x " + swapChain.getHeight();
    }

    private static String distance() {
        try {
            return Minecraft.getInstance().options.renderDistance().get() + " chunks";
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static boolean uncapped() {
        SwapChain swapChain = swapChain();
        return swapChain != null && !swapChain.isVsync();
    }

    private static String presentMode() {
        SwapChain swapChain = swapChain();
        return swapChain == null ? null : swapChain.isVsync() ? "FIFO" : "Immediate";
    }

    private static int visibleSections() {
        try {
            WorldRenderer renderer = WorldRenderer.getInstance();
            return renderer == null ? -1 : renderer.getVisibleSectionsCount();
        } catch (Throwable unavailable) {
            return -1;
        }
    }

    private static String loadedSections() {
        try {
            WorldRenderer renderer = WorldRenderer.getInstance();
            return renderer == null ? null
                    : "of " + renderer.getSectionGrid().getSectionCount() + " loaded";
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static String entities() {
        try {
            return Integer.toString(Minecraft.getInstance().level.getEntityCount());
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static String particles() {
        try {
            return Minecraft.getInstance().particleEngine.countParticles();
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static TaskDispatcher dispatcher() {
        try {
            WorldRenderer renderer = WorldRenderer.getInstance();
            return renderer == null ? null : renderer.getTaskDispatcher();
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static String vram() {
        try {
            return MemoryManager.getInstance().getAllocatedDeviceMemoryMB() + " MB";
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static String hostMemory() {
        try {
            return MemoryManager.getInstance().getNativeMemoryMB() + " MB";
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static String gcTotal() {
        long total = 0L;
        long count = 0L;
        for (java.lang.management.GarbageCollectorMXBean bean
                : java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean.getCollectionTime() > 0L) {
                total += bean.getCollectionTime();
            }
            if (bean.getCollectionCount() > 0L) {
                count += bean.getCollectionCount();
            }
        }
        return count + " · " + total + " ms";
    }

    private static String count(int value) {
        return value < 0 ? null : Integer.toString(value);
    }

    private static SwapChain swapChain() {
        try {
            return Vulkan.getSwapChain();
        } catch (Throwable unavailable) {
            return null;
        }
    }
}
