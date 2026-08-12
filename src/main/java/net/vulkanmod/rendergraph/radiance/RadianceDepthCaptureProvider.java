package net.vulkanmod.rendergraph.radiance;

import net.vulkanmod.Initializer;
import net.vulkanmod.api.CalderaBridge;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.pass.DepthCaptureProvider;
import net.vulkanmod.vulkan.pass.EngineContext;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageCopy;

import static org.lwjgl.vulkan.VK10.*;

public final class RadianceDepthCaptureProvider implements DepthCaptureProvider {
    private EngineContext context;
    private Runnable rebindTarget;

    private VulkanImage capturedWorldDepth;
    private VulkanImage capturedOpaqueDepth;
    private VulkanImage capturedForegroundDepth;
    private VulkanImage coloredShadowDepth;
    private int scaledDepthClears;
    private boolean liveDepthIsForeground;

    public void setRebindTarget(Runnable rebindTarget) {
        this.rebindTarget = rebindTarget;
    }

    @Override
    public void initialize(EngineContext context) {
        this.context = context;
    }

    public void beginFrame() {
        this.scaledDepthClears = 0;
        this.liveDepthIsForeground = false;
    }

    @Override
    public void onDepthClear(Framebuffer framebuffer) {
        if (!context.postShaderActive() || framebuffer != context.mainFramebuffer() || !context.isScaledFramebuffer())
            return;

        this.scaledDepthClears++;

        if (this.scaledDepthClears == 2) {
            VRenderSystem.captureWorldReconstruction();
            this.capturedWorldDepth = snapshotDepth(this.capturedWorldDepth, context.mainFramebuffer());
        } else if (this.scaledDepthClears == 3) {
            this.liveDepthIsForeground = true;
        }
    }

    @Override
    public boolean suppressDepthClear(Framebuffer framebuffer) {
        return context.postShaderActive()
                && context.isScaledFramebuffer()
                && framebuffer == context.mainFramebuffer()
                && this.scaledDepthClears >= 3;
    }

    @Override
    public void captureOpaqueDepth() {
        if (context.postShaderActive() && context.isScaledFramebuffer()) {
            this.capturedOpaqueDepth = snapshotDepth(this.capturedOpaqueDepth, context.mainFramebuffer());
            return;
        }

        if (!Initializer.CONFIG.lodDepthSnapshot || context.mainFramebuffer() == null) return;
        if (!CalderaBridge.isOcclusionRefreshFrame()) return;

        this.capturedOpaqueDepth = snapshotDepth(this.capturedOpaqueDepth, context.mainFramebuffer());
    }

    @Override
    public VulkanImage getCapturedOpaqueDepth() {
        if (context.postShaderActive() && context.isScaledFramebuffer())
            return this.capturedOpaqueDepth;

        if (!Initializer.CONFIG.lodDepthSnapshot)
            return null;

        return this.capturedOpaqueDepth;
    }

    @Override
    public VulkanImage getWorldDepth() {
        return this.capturedWorldDepth;
    }

    @Override
    public VulkanImage getForegroundDepth() {
        if (this.capturedWorldDepth == null)
            return null;

        if (this.liveDepthIsForeground) {
            Framebuffer fb = context.mainFramebuffer();
            return fb != null ? fb.getDepthAttachment() : null;
        }

        return this.capturedForegroundDepth != null ? this.capturedForegroundDepth : this.capturedWorldDepth;
    }

    @Override
    public boolean isLiveDepthForeground() {
        return this.liveDepthIsForeground;
    }

    public VulkanImage getColoredShadowDepth() {
        return this.coloredShadowDepth;
    }

    public VulkanImage ensureColoredShadowDepth() {
        if (this.capturedOpaqueDepth != null) return this.capturedOpaqueDepth;
        this.coloredShadowDepth = snapshotDepth(this.coloredShadowDepth, context.mainFramebuffer());
        return this.coloredShadowDepth;
    }

    private VulkanImage snapshotDepth(VulkanImage target, Framebuffer source) {
        VulkanImage src = source.getDepthAttachment();
        int w = src.width;
        int h = src.height;

        if (target == null || target.width != w || target.height != h || target.format != src.format) {
            if (target != null) target.free();

            target = VulkanImage.createDepthImage(
                    src.format, w, h,
                    VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    false, true);
        }

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        Renderer.getInstance().endRenderPass(commandBuffer);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            src.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
            target.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            VkImageCopy.Buffer region = VkImageCopy.calloc(1, stack);
            region.srcSubresource().set(VK_IMAGE_ASPECT_DEPTH_BIT, 0, 0, 1);
            region.srcOffset().set(0, 0, 0);
            region.dstSubresource().set(VK_IMAGE_ASPECT_DEPTH_BIT, 0, 0, 1);
            region.dstOffset().set(0, 0, 0);
            region.extent().set(w, h, 1);

            vkCmdCopyImage(commandBuffer,
                    src.getId(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    target.getId(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    region);

            target.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            src.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
        }

        if (rebindTarget != null)
            rebindTarget.run();

        return target;
    }

    @Override
    public void cleanup() {
        if (this.capturedWorldDepth != null) { this.capturedWorldDepth.free(); this.capturedWorldDepth = null; }
        if (this.capturedOpaqueDepth != null) { this.capturedOpaqueDepth.free(); this.capturedOpaqueDepth = null; }
        if (this.capturedForegroundDepth != null) { this.capturedForegroundDepth.free(); this.capturedForegroundDepth = null; }
        if (this.coloredShadowDepth != null) { this.coloredShadowDepth.free(); this.coloredShadowDepth = null; }
    }
}
