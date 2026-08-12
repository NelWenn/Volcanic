package net.vulkanmod.vulkan.pass;

import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.texture.VulkanImage;

public interface DepthCaptureProvider extends PipelineFeature {

    void onDepthClear(Framebuffer framebuffer);

    boolean suppressDepthClear(Framebuffer framebuffer);

    void captureOpaqueDepth();

    VulkanImage getCapturedOpaqueDepth();

    VulkanImage getWorldDepth();

    VulkanImage getForegroundDepth();

    boolean isLiveDepthForeground();
}
