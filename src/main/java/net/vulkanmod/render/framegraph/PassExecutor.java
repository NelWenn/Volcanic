package net.vulkanmod.render.framegraph;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

@Deprecated(since = "Volcanic 0.1.4 alpha")
public interface PassExecutor {
    void execute(VkCommandBuffer commandBuffer, MemoryStack stack);
}
