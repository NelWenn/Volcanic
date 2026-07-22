package net.vulkanmod.mixin.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.Device;
import net.vulkanmod.vulkan.memory.MemoryManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.vulkanmod.Initializer.getVersion;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayM {

    @Shadow
    private static long bytesToMegabytes(long bytes) {
        return 0;
    }

    @Unique
    private static String volca$cpuInfoCache;

    @Redirect(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;"))
    private ArrayList<String> redirectList(Object[] elements) {
        ArrayList<String> strings = new ArrayList<>();

        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;

        Device device = Vulkan.getDevice();

        strings.add(String.format("Java: %s", System.getProperty("java.version")));
        strings.add(String.format("Mem: % 2d%% %03d/%03dMB", usedMemory * 100L / maxMemory, bytesToMegabytes(usedMemory), bytesToMegabytes(maxMemory)));
        strings.add(String.format("Allocated: % 2d%% %03dMB", totalMemory * 100L / maxMemory, bytesToMegabytes(totalMemory)));
        strings.add(String.format("Off-heap: " + volca$getOffHeapMemory() + "MB"));
        strings.add("NativeMemory: %dMB".formatted(MemoryManager.getInstance().getNativeMemoryMB()));
        strings.add("DeviceMemory: %dMB".formatted(MemoryManager.getInstance().getAllocatedDeviceMemoryMB()));
        strings.add("");
        strings.add("Volcanic " + getVersion());
        strings.add("CPU: " + volca$getCpuInfo());
        strings.add("GPU: " + device.deviceName);
        strings.add("Driver: " + device.driverVersion);
        strings.add("Vulkan: " + device.vkVersion);
        strings.add("");
        strings.add("");

        Collections.addAll(strings, WorldRenderer.getInstance().getChunkAreaManager().getStats());

        return strings;
    }

    @Unique
    private long volca$getOffHeapMemory() {
        return bytesToMegabytes(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed());
    }

    @Unique
    private static String volca$getCpuInfo() {
        if (volca$cpuInfoCache != null)
            return volca$cpuInfoCache;

        String cpu = "Unknown CPU";

        try {
            String os = System.getProperty("os.name", "").toLowerCase();

            if (os.contains("win")) {
                Process process = Runtime.getRuntime().exec(
                        new String[]{"wmic", "cpu", "get", "name"}
                );

                try (var reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    cpu = reader.lines()
                            .map(String::trim)
                            .filter(trim -> !trim.equalsIgnoreCase("Name"))
                            .filter(line -> !line.isEmpty())
                            .findFirst()
                            .orElse(cpu);
                }
            } else if (os.contains("linux")) {
                try (var reader = new BufferedReader(
                        new FileReader("/proc/cpuinfo"))) {
                    cpu = reader.lines()
                            .filter(line -> line.startsWith("model name"))
                            .map(line -> line.split(":", 2)[1].trim())
                            .findFirst()
                            .orElse(cpu);
                }
            } else if (os.contains("mac")) {
                Process process = Runtime.getRuntime().exec(
                        new String[]{"sysctl", "-n", "machdep.cpu.brand_string"}
                );

                try (var reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    cpu = reader.readLine();
                }
            }
        } catch (Exception ignored) {
        }

        volca$cpuInfoCache = "%s x %d".formatted(
                cpu,
                Runtime.getRuntime().availableProcessors()
        );

        return volca$cpuInfoCache;
    }
}
