package net.vulkanmod.render;

import net.vulkanmod.Initializer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.shader.ComputePipeline;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageMemoryBarrier;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public final class HiZPyramid {
    private static final int WORKGROUP_DIM = 8;
    private static final int PUSH_SIZE = 16;

    private static ComputePipeline downsamplePipeline;
    private static boolean pipelineFailed;

    private static VulkanImage pyramid;
    private static int mipCount;
    private static boolean built;

    private static VulkanImage fallbackImage;

    private HiZPyramid() {
    }

    public static boolean isReady() {
        return pyramid != null && built;
    }

    public static VulkanImage getSampleImage() {
        return pyramid;
    }

    public static int getMipCount() {
        return pyramid != null ? mipCount : 0;
    }

    public static int getWidth() {
        return pyramid != null ? pyramid.width : 0;
    }

    public static int getHeight() {
        return pyramid != null ? pyramid.height : 0;
    }

    public static boolean build(VkCommandBuffer commandBuffer) {
        VulkanImage depthSource = resolveMainDepth();
        return depthSource != null && build(commandBuffer, depthSource);
    }

    public static boolean build(VkCommandBuffer commandBuffer, VulkanImage depthSource) {
        Renderer renderer = Renderer.getInstance();
        if (renderer == null || !Renderer.isRecording() || depthSource == null || !ensurePipeline()) {
            return false;
        }

        boolean suspended = renderer.getBoundRenderPass() != null;
        if (suspended) {
            renderer.endRenderPass();
        }

        try (MemoryStack stack = stackPush()) {
            ensurePyramid(depthSource.width, depthSource.height);

            int originalDepthLayout = depthSource.getCurrentLayout();
            if (originalDepthLayout != VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
                int srcStage;
                int srcAccess;
                if (originalDepthLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
                    srcStage = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT;
                    srcAccess = VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
                } else if (originalDepthLayout == VK_IMAGE_LAYOUT_UNDEFINED) {
                    srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                    srcAccess = 0;
                } else {
                    srcStage = VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
                    srcAccess = VK_ACCESS_MEMORY_WRITE_BIT;
                }
                VulkanImage.transitionLayout(stack, commandBuffer, depthSource, 0,
                        originalDepthLayout, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                        srcStage, srcAccess,
                        VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_ACCESS_SHADER_READ_BIT);
            }

            VulkanImage.transitionLayout(stack, commandBuffer, pyramid, 0,
                    pyramid.getCurrentLayout(), VK_IMAGE_LAYOUT_GENERAL,
                    VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, 0,
                    VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_ACCESS_SHADER_WRITE_BIT | VK_ACCESS_SHADER_READ_BIT);

            downsamplePipeline.bind(commandBuffer);

            for (int level = 0; level < mipCount; ++level) {
                int dstWidth = Math.max(1, pyramid.width >> level);
                int dstHeight = Math.max(1, pyramid.height >> level);

                if (level == 0) {
                    downsamplePipeline.bindResources(commandBuffer,
                            new ComputePipeline.SamplerResource(depthSource.getDepthSampleView(), depthSource.getSampler(),
                                    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL),
                            new ComputePipeline.StorageImageResource(pyramid.getLevelImageView(0)));
                } else {
                    mipBarrier(stack, commandBuffer, level - 1, 1);
                    downsamplePipeline.bindResources(commandBuffer,
                            new ComputePipeline.SamplerResource(pyramid.getImageView(), pyramid.getSampler(),
                                    VK_IMAGE_LAYOUT_GENERAL),
                            new ComputePipeline.StorageImageResource(pyramid.getLevelImageView(level)));
                }

                ByteBuffer push = stack.malloc(PUSH_SIZE);
                push.putInt(0, dstWidth);
                push.putInt(4, dstHeight);
                push.putInt(8, Math.max(0, level - 1));
                push.putInt(12, level == 0 ? 0 : 1);
                downsamplePipeline.pushConstants(commandBuffer, push);

                downsamplePipeline.dispatch(commandBuffer,
                        (dstWidth + WORKGROUP_DIM - 1) / WORKGROUP_DIM,
                        (dstHeight + WORKGROUP_DIM - 1) / WORKGROUP_DIM,
                        1);
            }

            mipBarrier(stack, commandBuffer, 0, mipCount);

            if (originalDepthLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
                VulkanImage.transitionLayout(stack, commandBuffer, depthSource, 0,
                        VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                        VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_ACCESS_SHADER_READ_BIT,
                        VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT,
                        VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
            }

            built = true;
            return true;
        } catch (Throwable t) {
            Initializer.LOGGER.error("[HiZ] pyramid build failed", t);
            built = false;
            return false;
        } finally {
            if (suspended) {
                renderer.getMainPass().rebindMainTarget();
            }
        }
    }

    public static VulkanImage getOrCreateFallback(VkCommandBuffer commandBuffer, MemoryStack stack) {
        if (fallbackImage == null) {
            fallbackImage = VulkanImage.builder(1, 1)
                    .setFormat(VK_FORMAT_R32_SFLOAT)
                    .setUsage(VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_STORAGE_BIT)
                    .setLinearFiltering(false)
                    .setClamp(true)
                    .createVulkanImage();
        }
        if (fallbackImage.getCurrentLayout() != VK_IMAGE_LAYOUT_GENERAL) {
            VulkanImage.transitionLayout(stack, commandBuffer, fallbackImage, 0,
                    fallbackImage.getCurrentLayout(), VK_IMAGE_LAYOUT_GENERAL,
                    VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, 0,
                    VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_ACCESS_SHADER_READ_BIT);
        }
        return fallbackImage;
    }

    public static void destroy() {
        if (pyramid != null) {
            pyramid.free();
            pyramid = null;
        }
        if (fallbackImage != null) {
            fallbackImage.free();
            fallbackImage = null;
        }
        built = false;
        mipCount = 0;
    }

    private static VulkanImage resolveMainDepth() {
        try {
            Renderer renderer = Renderer.getInstance();
            if (renderer == null) {
                return null;
            }
            int glId = renderer.getMainPass().getDepthAttachmentGlId();
            if (glId < 0) {
                return null;
            }
            GlTexture texture = GlTexture.getTexture(glId);
            return texture != null ? texture.getVulkanImage() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean ensurePipeline() {
        if (downsamplePipeline != null) {
            return true;
        }
        if (pipelineFailed) {
            return false;
        }
        try {
            downsamplePipeline = new ComputePipeline("basic/hiz_downsample/hiz",
                    new int[]{VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_DESCRIPTOR_TYPE_STORAGE_IMAGE},
                    PUSH_SIZE);
            return true;
        } catch (Throwable t) {
            pipelineFailed = true;
            Initializer.LOGGER.error("[HiZ] failed to create downsample pipeline", t);
            return false;
        }
    }

    private static void ensurePyramid(int width, int height) {
        if (pyramid != null && pyramid.width == width && pyramid.height == height) {
            return;
        }

        if (pyramid != null) {
            pyramid.free();
            pyramid = null;
        }

        mipCount = 32 - Integer.numberOfLeadingZeros(Math.max(width, height));
        pyramid = VulkanImage.builder(width, height)
                .setFormat(VK_FORMAT_R32_SFLOAT)
                .setUsage(VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_STORAGE_BIT)
                .setMipLevels(mipCount)
                .setLinearFiltering(false)
                .setClamp(true)
                .setLevelViews(true)
                .createVulkanImage();
        built = false;
    }

    private static void mipBarrier(MemoryStack stack, VkCommandBuffer commandBuffer, int baseMip, int levelCount) {
        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
        barrier.sType$Default();
        barrier.oldLayout(VK_IMAGE_LAYOUT_GENERAL);
        barrier.newLayout(VK_IMAGE_LAYOUT_GENERAL);
        barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.image(pyramid.getId());
        barrier.srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT);
        barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
        barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        barrier.subresourceRange().baseMipLevel(baseMip);
        barrier.subresourceRange().levelCount(levelCount);
        barrier.subresourceRange().baseArrayLayer(0);
        barrier.subresourceRange().layerCount(1);

        vkCmdPipelineBarrier(commandBuffer,
                VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                0, null, null, barrier);
    }
}
