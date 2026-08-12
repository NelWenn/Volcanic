package net.vulkanmod.vulkan.pass;

import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.function.IntConsumer;

public interface ShadowProvider extends PipelineFeature {

    void renderShadowMap(VkCommandBuffer commandBuffer, MemoryStack stack);

    void renderEntityShadows(Runnable casters, IntConsumer tintCascade);

    void applyColoredShadow();

    boolean isReady();

    VulkanImage getCascadeDepthImage(int cascade);

    VulkanImage getTintImage(int cascade);
}
