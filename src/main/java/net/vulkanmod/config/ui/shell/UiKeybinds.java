package net.vulkanmod.config.ui.shell;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class UiKeybinds {
    public static final String CATEGORY = "Volcanic";

    public static final KeyMapping OPEN_OPTIONS = new KeyMapping(
            "vulkanmod.keybind.open_options",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY);

    private UiKeybinds() {
    }
}
