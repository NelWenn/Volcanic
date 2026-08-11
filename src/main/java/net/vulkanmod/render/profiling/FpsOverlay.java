package net.vulkanmod.render.profiling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.vulkanmod.Initializer;
import net.vulkanmod.gui.debug.DebugOverlay;
import net.vulkanmod.gui.HUD;
import net.vulkanmod.gui.HudHandler;
import net.vulkanmod.vulkan.FrameTimer;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FpsOverlay extends HUD {
    private static final int MARGIN = 4;
    private static final int LINE = 10;
    private static final int GOOD = 0xFF8FBC76;
    private static final int SLOW = 0xFFE0A03A;
    private static final int TEXT = 0xFFE9DDD7;
    private static final int BACKDROP = 0xA00E0A09;
    private static final long REFRESH_MS = 500L;

    private List<String> cached = List.of();
    private long cachedAt;
    private int cachedFps;

    public FpsOverlay() {
        super("vulkanmod.keybind.toggle_fps_overlay", GLFW.GLFW_KEY_UNKNOWN, "Volcanic");
    }

    @Override
    public boolean shouldRender() {
        return super.shouldRender() && (counter() || coordinates())
                && !debugScreenOpen() && !debugPanelOpen();
    }

    @Override
    public void render(GuiGraphics graphics) {
        boolean counter = counter();
        Minecraft minecraft = Minecraft.getInstance();
        if (graphics == null || minecraft == null || minecraft.font == null) {
            return;
        }
        refresh(counter, coordinates());
        if (cached.isEmpty()) {
            return;
        }

        int width = 0;
        for (String line : cached) {
            width = Math.max(width, minecraft.font.width(line));
        }
        int height = cached.size() * LINE + 3;
        graphics.fill(MARGIN - 3, MARGIN - 3, MARGIN + width + 3, MARGIN + height - 3, BACKDROP);
        for (int index = 0; index < cached.size(); index++) {
            graphics.drawString(minecraft.font, cached.get(index), MARGIN, MARGIN + index * LINE,
                    counter && index == 0 ? tint(cachedFps) : TEXT, false);
        }
    }

    private void refresh(boolean counter, boolean coordinates) {
        long now = System.currentTimeMillis();
        if (now - cachedAt < REFRESH_MS && !cached.isEmpty()) {
            return;
        }
        cachedAt = now;

        List<String> lines = new ArrayList<>(2);
        if (counter) {
            double frameMs = FrameTimer.frameMs();
            cachedFps = frameMs > 0.0
                    ? (int) Math.round(1000.0 / frameMs) : Minecraft.getInstance().getFps();
            lines.add(cachedFps + " fps");
        }
        if (coordinates) {
            lines.add(position());
        }
        lines.removeIf(String::isBlank);
        cached = List.copyOf(lines);
    }

    private static boolean debugPanelOpen() {
        DebugOverlay overlay = HudHandler.getInstance().get(DebugOverlay.class);
        return overlay != null && overlay.isEnabled();
    }

    private static boolean debugScreenOpen() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.gui != null && minecraft.gui.getDebugOverlay().showDebugScreen();
        } catch (Throwable unavailable) {
            return false;
        }
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

    private static boolean counter() {
        try {
            return Initializer.CONFIG.showFpsCounter;
        } catch (Throwable unavailable) {
            return false;
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
}
