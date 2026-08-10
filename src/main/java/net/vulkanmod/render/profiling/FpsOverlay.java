package net.vulkanmod.render.profiling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.ui.core.FrameSamples;
import net.vulkanmod.vulkan.FrameTimer;
import net.vulkanmod.vulkan.SessionSamples;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FpsOverlay {
    public static final int OFF = 0;
    public static final int SIMPLE = 1;
    public static final int ADVANCED = 2;

    private static final int MARGIN = 4;
    private static final int LINE = 10;
    private static final int GOOD = 0xFF8FBC76;
    private static final int SLOW = 0xFFE0A03A;
    private static final int TEXT = 0xFFE9DDD7;
    private static final int FAINT = 0xFF8A7770;
    private static final int BACKDROP = 0xA00E0A09;
    private static final long REFRESH_MS = 500L;

    private static List<String> cached = List.of();
    private static long cachedAt;
    private static int cachedFps;

    private FpsOverlay() {
    }

    public static void render(GuiGraphics graphics) {
        int mode = mode();
        boolean coordinates = coordinates();
        if ((mode == OFF && !coordinates) || graphics == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.font == null) {
            return;
        }
        refresh(mode, coordinates);
        cached = cached.stream().filter(line -> !line.isBlank()).toList();
        if (cached.isEmpty()) {
            return;
        }

        int width = 0;
        for (String line : cached) {
            width = Math.max(width, minecraft.font.width(line));
        }
        int height = cached.size() * LINE + 3;
        graphics.fill(MARGIN - 3, MARGIN - 3, MARGIN + width + 3, MARGIN + height - 3, BACKDROP);
        boolean hasFps = mode != OFF;
        for (int index = 0; index < cached.size(); index++) {
            graphics.drawString(minecraft.font, cached.get(index), MARGIN, MARGIN + index * LINE,
                    hasFps && index == 0 ? tint(cachedFps) : index <= 1 ? TEXT : FAINT, false);
        }
    }

    private static void refresh(int mode, boolean coordinates) {
        long now = System.currentTimeMillis();
        if (now - cachedAt < REFRESH_MS && !cached.isEmpty()) {
            return;
        }
        cachedAt = now;

        List<String> lines = new ArrayList<>(4);
        double frameMs = FrameTimer.frameMs();
        cachedFps = frameMs > 0.0 ? (int) Math.round(1000.0 / frameMs) : Minecraft.getInstance().getFps();
        if (mode != OFF) {
            lines.add(cachedFps + " fps");
        }

        if (mode >= ADVANCED) {
            lines.add(String.format(Locale.ROOT, "%s frame  %s cpu  %s gpu",
                    millis(frameMs), millis(FrameTimer.cpuBusyMs()), millis(FrameTimer.gpuMs())));
            FrameSamples.Summary play = SessionSamples.samples().summary();
            if (play.count() >= FrameSamples.READY_AT) {
                lines.add(String.format(Locale.ROOT, "1%% low %.0f fps  ·  P95 %.1f ms  ·  %d stutters",
                        play.low1() > 0.0f ? 1000.0f / play.low1() : 0.0f, play.p95(), play.spikes()));
            }
        }
        if (coordinates) {
            lines.add(position());
        }
        cached = List.copyOf(lines);
    }

    private static String position() {
        try {
            net.minecraft.world.entity.Entity camera = Minecraft.getInstance().getCameraEntity();
            if (camera == null) {
                return "";
            }
            return String.format(Locale.ROOT, "%.1f  %.1f  %.1f   %s",
                    camera.getX(), camera.getY(), camera.getZ(),
                    camera.getDirection().getName());
        } catch (Throwable unavailable) {
            return "";
        }
    }

    private static boolean coordinates() {
        try {
            return Initializer.CONFIG.showCoordinates;
        } catch (Throwable unavailable) {
            return false;
        }
    }

    private static int tint(int fps) {
        return fps >= 60 ? GOOD : fps >= 30 ? TEXT : SLOW;
    }

    private static String millis(double value) {
        return value < 0.0 ? "-" : String.format(Locale.ROOT, "%.1f", value);
    }

    private static int mode() {
        try {
            return Math.max(OFF, Math.min(ADVANCED, Initializer.CONFIG.showFps));
        } catch (Throwable unavailable) {
            return OFF;
        }
    }
}
