package net.vulkanmod.mixin.compatibility.gl;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GLFW.class)
public class GLFWCompatM {
    @Overwrite(remap = false)
    public static long glfwGetCurrentContext() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getWindow() != null) {
            long handle = mc.getWindow().getWindow();
            if (handle != 0L) {
                return handle;
            }
        }
        return 1L;
    }
}
