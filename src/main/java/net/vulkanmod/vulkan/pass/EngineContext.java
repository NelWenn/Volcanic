package net.vulkanmod.vulkan.pass;

import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.SwapChain;

/**
 * Read only view of the current Rendering state, implemented in Main passes
 */
public interface EngineContext {

    Framebuffer mainFramebuffer();

    SwapChain swapChain();

    boolean postShaderActive();

    boolean isScaledFramebuffer();

    int renderWidth();

    int renderHeight();
}
