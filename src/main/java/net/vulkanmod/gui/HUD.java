package net.vulkanmod.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public abstract class HUD {

    public static final KeyMapping optionsKeyMapping = new KeyMapping(
            "vulkanmod.keybind.open_options",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "Volcanic");

    private int priority = 0;
    private boolean enabled = true;

    private final KeyMapping toggleKeyMapping;

    protected HUD(String translationKey, int defaultKey, String category) {
        this.toggleKeyMapping = new KeyMapping(translationKey, defaultKey, category);
    }

    public abstract void render(GuiGraphics guiGraphics);

    public boolean shouldRender() {
        return enabled && !Minecraft.getInstance().options.hideGui;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggle() {
        this.enabled = !this.enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public KeyMapping getToggleKeyMapping() {
        return toggleKeyMapping;
    }
}
