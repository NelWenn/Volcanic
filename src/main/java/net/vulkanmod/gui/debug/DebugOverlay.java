package net.vulkanmod.gui.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.MinecraftServer;
import net.vulkanmod.Initializer;
import net.vulkanmod.gui.HUD;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.Device;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DebugOverlay extends HUD {

    public static Font font;

    private record HardwareInfos(
            String processor,
            String gpu,
            int threadCount
    ) {}

    private record SoftwareInfos(
            String VKVersion,
            String VolcanicBuild
    ) {}

    private HardwareInfos hardwareCache;
    private SoftwareInfos softwareCache;

    private List<String> cachedHwLines;
    private List<String> cachedSwLines;

    private static final int MARGIN = 8;
    private static final int PADDING = 6;
    private static final int PANEL_GAP = 5;
    private static final int LINE_HEIGHT = 11;

    private static final int BG_COLOR = 0x80101010;
    private static final int BORDER_COLOR = 0x30FFFFFF;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private static final int FRAME_BUFFER_SIZE = 1000;
    private final long[] frameTimeBuffer = new long[FRAME_BUFFER_SIZE];
    private int frameIndex = 0;
    private int validFrames = 0;
    private long lastFrameTimestamp = System.nanoTime();
    private int cached1PercentLow = 0;
    private long lastLowUpdate = 0;
    private int HUDState = 0;

    public DebugOverlay() {
        super("vulkanmod.keybind.toggle_debug_overlay", GLFW.GLFW_KEY_END, "Volcanic");
        this.setEnabled(false);
    }

    @Override
    public void toggle() {
        HUDState = (HUDState + 1) % 4;
    }

    @Override
    public boolean shouldRender() {
        return HUDState > 0;
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        if (!shouldRender()) return;

        Minecraft mc = Minecraft.getInstance();
        if (font == null) font = mc.font;

        if (hardwareCache == null) {
            hardwareCache = getHardwareInfos();
            cachedHwLines = List.of(
                    "§7CPU:§r " + hardwareCache.processor(),
                    "§7GPU:§r " + hardwareCache.gpu(),
                    "§7Threads:§r " + hardwareCache.threadCount()
            );
        }
        if (softwareCache == null) {
            softwareCache = getSoftwareInfos();
            cachedSwLines = List.of(
                    "§7Vulkan:§r " + softwareCache.VKVersion(),
                    "§7Build:§r " + softwareCache.VolcanicBuild()
            );
        }

        long now = System.nanoTime();
        long frameTime = now - lastFrameTimestamp;
        lastFrameTimestamp = now;
        updateFrameTimeBuffer(frameTime);
        int currentFps = mc.getFps();

        int currentY = MARGIN;

        String perfLine = buildPerformanceLine(mc, currentFps);
        int perfWidth = font.width(perfLine) + PADDING * 2;
        int perfHeight = LINE_HEIGHT + PADDING * 2;

        drawPanel(guiGraphics, MARGIN, currentY, perfWidth, perfHeight);
        guiGraphics.drawString(font, perfLine, MARGIN + PADDING, currentY + PADDING, TEXT_COLOR, false);

        if (HUDState > 1) {
            currentY += perfHeight + PANEL_GAP;

            List<String> debugLines = buildDebug();

            int debugWidth = 250 + PADDING * 2;
            int debugHeight = LINE_HEIGHT * debugLines.size() + PADDING * 2;

            drawPanel(guiGraphics, MARGIN, currentY, debugWidth, debugHeight);
            renderTextLines(guiGraphics, debugLines, MARGIN + PADDING, currentY + PADDING);
        }

        if (HUDState > 2) {
            int hwTextWidth = getMaxWidth(cachedHwLines);
            int swTextWidth = getMaxWidth(cachedSwLines);

            int hwWidth = hwTextWidth + PADDING * 2;
            int swWidth = swTextWidth + PADDING * 2;

            int maxLines = Math.max(cachedHwLines.size(), cachedSwLines.size());
            int bottomPanelsHeight = (maxLines * LINE_HEIGHT) + PADDING * 2;

            int hwX = MARGIN;
            int bottomY = guiGraphics.guiHeight() - bottomPanelsHeight - 10;
            drawPanel(guiGraphics, hwX, bottomY, hwWidth, bottomPanelsHeight);
            renderTextLines(guiGraphics, cachedHwLines, hwX + PADDING, bottomY + PADDING);

            int swX = hwX + hwWidth + PANEL_GAP;
            drawPanel(guiGraphics, swX, bottomY, swWidth, bottomPanelsHeight);
            renderTextLines(guiGraphics, cachedSwLines, swX + PADDING, bottomY + PADDING);
        }
    }

    private void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, BG_COLOR);
        guiGraphics.renderOutline(x, y, width, height, BORDER_COLOR);
    }

    private void renderTextLines(GuiGraphics guiGraphics, List<String> lines, int x, int y) {
        int textY = y;
        for (String line : lines) {
            guiGraphics.drawString(font, line, x, textY, TEXT_COLOR, false);
            textY += LINE_HEIGHT;
        }
    }

    private int getMaxWidth(List<String> lines) {
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }
        return maxWidth;
    }

    private String buildPerformanceLine(Minecraft mc, int fps) {
        String fpsColor = getColorCode(fps, 60, 30);
        String lowColor = getColorCode(cached1PercentLow, 60, 30);

        StringBuilder sb = new StringBuilder();
        sb.append("FPS: ").append(fpsColor).append(fps).append("§r");
        sb.append(" | 1% Low: ").append(lowColor).append(cached1PercentLow).append(" FPS§r");

        MinecraftServer server = mc.getSingleplayerServer();
        if (server != null) {
            float msPerTick = server.getAverageTickTimeNanos() / 1_000_000f;
            float tps = Math.min(20.0f, 1000.0f / Math.max(1.0f, msPerTick));
            String tpsColor = getColorCode((int) tps, 19, 15);
            sb.append(" | TPS: ").append(tpsColor).append(String.format("%.1f", tps)).append("§r");
        }

        return sb.toString();
    }

    private List<String> buildDebug() {
        List<String> sb = new ArrayList<>();

        sb.addAll(WorldRenderer.getInstance().getChunkStatisticsAsList());
        sb.addAll(WorldRenderer.getInstance().getChunkAreaManager().getStatsAsList());

        return sb;
    }

    private void updateFrameTimeBuffer(long frameTimeNanos) {
        if (frameTimeNanos <= 0 || frameTimeNanos > 2_000_000_000L) return;

        frameTimeBuffer[frameIndex] = frameTimeNanos;
        frameIndex = (frameIndex + 1) % FRAME_BUFFER_SIZE;
        if (validFrames < FRAME_BUFFER_SIZE) validFrames++;

        long now = System.currentTimeMillis();
        if (now - lastLowUpdate > 1000 && validFrames > 0) {
            long[] tempBuffer = new long[validFrames];
            System.arraycopy(frameTimeBuffer, 0, tempBuffer, 0, validFrames);
            Arrays.sort(tempBuffer);

            int onePercentCount = Math.max(1, Math.round(validFrames * 0.01f));
            long sum = 0;

            for (int i = validFrames - onePercentCount; i < validFrames; i++) {
                sum += tempBuffer[i];
            }

            double avgWorstFrameTimeNanos = (double) sum / onePercentCount;
            cached1PercentLow = (int) Math.round(1_000_000_000.0 / avgWorstFrameTimeNanos);
            lastLowUpdate = now;
        }
    }

    private String getColorCode(int value, int goodThreshold, int okThreshold) {
        if (value >= goodThreshold) return "§a";
        if (value >= okThreshold) return "§e";
        return "§c";
    }

    private static HardwareInfos getHardwareInfos() {
        String cpu = "Unknown CPU";
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                Process process = Runtime.getRuntime().exec(new String[]{"wmic", "cpu", "get", "name"});
                try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    cpu = reader.lines()
                            .map(String::trim)
                            .filter(trim -> !trim.equalsIgnoreCase("Name"))
                            .filter(line -> !line.isEmpty())
                            .findFirst()
                            .orElse(cpu);
                }
            } else if (os.contains("linux")) {
                try (var reader = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
                    cpu = reader.lines()
                            .filter(line -> line.startsWith("model name"))
                            .map(line -> line.split(":", 2)[1].trim())
                            .findFirst()
                            .orElse(cpu);
                }
            } else if (os.contains("mac")) {
                Process process = Runtime.getRuntime().exec(new String[]{"sysctl", "-n", "machdep.cpu.brand_string"});
                try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    cpu = reader.readLine();
                }
            }
        } catch (Exception ignored) {}

        cpu = cpu.replace("(R)", "").replace("(TM)", "").replace(" CPU", "").trim();

        Device device = Vulkan.getDevice();
        String gpu = device != null ? device.deviceName : "Unknown GPU";
        int threads = Runtime.getRuntime().availableProcessors();

        return new HardwareInfos(cpu, gpu, threads);
    }

    private static SoftwareInfos getSoftwareInfos() {
        Device device = Vulkan.getDevice();
        String vkVersion = device != null ? device.vkVersion : "Unknown";
        String modBuild = Initializer.getVersion();

        return new SoftwareInfos(vkVersion, modBuild);
    }
}