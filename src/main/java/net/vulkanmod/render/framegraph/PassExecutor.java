package net.vulkanmod.render.framegraph;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

public interface PassExecutor {
    void execute(VkCommandBuffer commandBuffer, MemoryStack stack);
}
