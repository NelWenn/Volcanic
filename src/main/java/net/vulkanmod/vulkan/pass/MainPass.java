package net.vulkanmod.vulkan.pass;

import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

public interface MainPass {

    void begin(VkCommandBuffer commandBuffer, MemoryStack stack);

    void end(VkCommandBuffer commandBuffer);

    /** Called just before a depth clear, so a depth post shader can snapshot depth before it's wiped. */
    default void onDepthClear(Framebuffer framebuffer) {}

    /** Called right after {@link #onDepthClear} for the same clear: return true to skip the depth aspect
     *  of that clear (color is still cleared). */
    default boolean suppressDepthClear(Framebuffer framebuffer) {
        return false;
    }

    default void mainTargetBindWrite() {}

    default void mainTargetUnbindWrite() {}

    default void rebindMainTarget() {}

    default void bindAsTexture() {}

    default void resolveRenderScaleForGui() {}

    default int getColorAttachmentGlId() {
        return -1;
    }

    default int getDepthAttachmentGlId() {
        return -1;
    }
}
