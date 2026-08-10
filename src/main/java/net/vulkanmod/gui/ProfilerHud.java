package net.vulkanmod.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.vulkanmod.compat.RuntimeOptions;
import net.vulkanmod.render.profiling.ProfilerOverlay;
import org.lwjgl.glfw.GLFW;

public class ProfilerHud extends HUD {

    public ProfilerHud() {
        super("vulkanmod.keybind.toggle_profiler_overlay", GLFW.GLFW_KEY_UNKNOWN, "Volcanic");
    }

    @Override
    public boolean shouldRender() {
        return super.shouldRender() && ProfilerOverlay.shouldRender
                && !RuntimeOptions.profilingMixinsEnabled();
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        if (ProfilerOverlay.INSTANCE == null) {
            ProfilerOverlay.createInstance(Minecraft.getInstance());
        }
        ProfilerOverlay.INSTANCE.render(guiGraphics);
    }
}
