package net.vulkanmod.config.ui.settings;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.ui.core.FrameSamples;
import net.vulkanmod.vulkan.FrameTimer;
import net.vulkanmod.vulkan.SessionSamples;
import net.vulkanmod.vulkan.SystemInfo;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.framebuffer.SwapChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SystemReport {
    public record Row(String labelKey, String value, boolean section) {
    }

    private static final long MB = 1024L * 1024L;
    private static String neoforge;

    private SystemReport() {
    }

    public static List<Row> rows() {
        List<Row> rows = new ArrayList<>();

        section(rows, "vulkanmod.ui.developer.info.section.machine");
        add(rows, "vulkanmod.overview.gpu", deviceName());
        add(rows, "vulkanmod.ui.developer.info.driver", driver());
        add(rows, "vulkanmod.ui.developer.info.cpu", trimmed(SystemInfo.cpuInfo));
        add(rows, "vulkanmod.ui.developer.info.cores",
                Integer.toString(Runtime.getRuntime().availableProcessors()));
        add(rows, "vulkanmod.ui.developer.info.heap", heap());
        add(rows, "vulkanmod.ui.developer.info.os", System.getProperty("os.name")
                + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")");
        add(rows, "vulkanmod.ui.developer.info.java", System.getProperty("java.version"));

        section(rows, "vulkanmod.ui.developer.info.section.vulkan");
        add(rows, "vulkanmod.ui.developer.info.vk_instance", vkVersion());
        add(rows, "vulkanmod.ui.developer.info.present_mode", presentMode());
        add(rows, "vulkanmod.ui.developer.info.color_format", colorFormat());
        add(rows, "vulkanmod.ui.developer.info.swapchain_images", swapchainImages());
        add(rows, "vulkanmod.ui.developer.info.indirect_draw", indirectDraw());

        section(rows, "vulkanmod.ui.developer.info.section.volcanic");
        add(rows, "vulkanmod.ui.developer.info.mod_version", Initializer.getVersion());
        add(rows, "vulkanmod.ui.developer.info.minecraft", minecraftVersion());
        add(rows, "vulkanmod.ui.developer.info.neoforge", neoforgeVersion());
        add(rows, "vulkanmod.ui.developer.info.mod_count", modCount());
        add(rows, "vulkanmod.ui.developer.info.render_size", renderSize());
        add(rows, "vulkanmod.ui.developer.info.window", windowSize());

        section(rows, "vulkanmod.ui.developer.info.section.frames");
        add(rows, "vulkanmod.ui.developer.info.frame_ms", millis(FrameTimer.frameMs()));
        add(rows, "vulkanmod.ui.developer.info.cpu_ms", millis(FrameTimer.cpuBusyMs()));
        add(rows, "vulkanmod.ui.developer.info.gpu_ms", millis(FrameTimer.gpuMs()));
        add(rows, "vulkanmod.ui.developer.info.gameplay", gameplay());
        return List.copyOf(rows);
    }

    private static void section(List<Row> rows, String labelKey) {
        rows.add(new Row(labelKey, "", true));
    }

    private static void add(List<Row> rows, String labelKey, String value) {
        if (value != null && !value.isBlank()) {
            rows.add(new Row(labelKey, value, false));
        }
    }

    private static String deviceName() {
        return DeviceManager.device == null ? null : trimmed(DeviceManager.device.deviceName);
    }

    private static String driver() {
        return DeviceManager.device == null ? null : trimmed(DeviceManager.device.driverVersion);
    }

    private static String vkVersion() {
        return DeviceManager.device == null ? null : trimmed(DeviceManager.device.vkVersion);
    }

    private static String heap() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / MB + " / " + runtime.maxMemory() / MB + " MB";
    }

    private static String presentMode() {
        SwapChain swapChain = swapChain();
        return swapChain == null ? null
                : swapChain.isVsync() ? "FIFO (vsync)" : "Immediate";
    }

    private static String colorFormat() {
        SwapChain swapChain = swapChain();
        return swapChain == null ? null : swapChain.isBGRAformat ? "B8G8R8A8_UNORM" : "R8G8B8A8_UNORM";
    }

    private static String swapchainImages() {
        SwapChain swapChain = swapChain();
        return swapChain == null || swapChain.getImagesNum() <= 0
                ? null : Integer.toString(swapChain.getImagesNum());
    }

    private static String indirectDraw() {
        try {
            return DeviceManager.supportsFastIndirectDraw() ? "Supported" : "Unsupported";
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static String renderSize() {
        SwapChain swapChain = swapChain();
        if (swapChain == null || swapChain.getWidth() <= 0 || swapChain.getHeight() <= 0) {
            return null;
        }
        return swapChain.getWidth() + " x " + swapChain.getHeight();
    }

    private static String windowSize() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return null;
        }
        return minecraft.getWindow().getWidth() + " x " + minecraft.getWindow().getHeight()
                + "  ·  scale " + String.format(Locale.ROOT, "%.1f", minecraft.getWindow().getGuiScale());
    }

    private static String minecraftVersion() {
        try {
            return SharedConstants.getCurrentVersion().getName();
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static String neoforgeVersion() {
        if (neoforge == null) {
            try {
                neoforge = ModList.get().getModContainerById("neoforge")
                        .map(container -> container.getModInfo().getVersion().toString()).orElse("");
            } catch (Throwable unavailable) {
                neoforge = "";
            }
        }
        return neoforge;
    }

    private static String modCount() {
        try {
            return Integer.toString(ModList.get().size());
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static String gameplay() {
        FrameSamples.Summary summary = SessionSamples.samples().summary();
        if (summary.count() == 0) {
            return null;
        }
        if (summary.count() < FrameSamples.READY_AT) {
            return summary.count() + " / " + FrameSamples.READY_AT + " frames";
        }
        return String.format(Locale.ROOT, "%.1f ms  ·  %.0f fps",
                summary.median(), 1000.0f / summary.median());
    }

    private static String millis(double value) {
        return value < 0.0 ? null : String.format(Locale.ROOT, "%.2f ms", value);
    }

    private static SwapChain swapChain() {
        try {
            return Vulkan.getSwapChain();
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() || "undef".equals(value) ? null : value.trim();
    }
}
