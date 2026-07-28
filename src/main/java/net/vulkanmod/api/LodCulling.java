package net.vulkanmod.api;

import net.vulkanmod.Initializer;
import net.vulkanmod.render.HiZPyramid;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.memory.Buffer;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.StorageBuffer;
import net.vulkanmod.vulkan.shader.ComputePipeline;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferMemoryBarrier;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public final class LodCulling {

    public static final int FLAG_OCCLUSION = 1;
    public static final int FLAG_COMPACT = 2;

    public static final int AABB_STRIDE_BYTES = 32;
    public static final int INDIRECT_COMMAND_BYTES = 20;

    private static final int WORKGROUP_SIZE = 64;
    private static final int PUSH_SIZE = 112;
    private static final int AABB_BUFFER_BYTES = 65_536;
    private static final float DEPTH_BIAS = 1.0e-4f;

    private static ComputePipeline cullPipeline;
    private static boolean pipelineFailed;

    private static StorageBuffer[] aabbBuffers;
    private static int lastAabbFrame = -1;

    private LodCulling() {
    }

    public static boolean isAvailable() {
        return ensurePipeline();
    }

    public static void writeAabb(ByteBuffer target, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        target.putFloat(minX).putFloat(minY).putFloat(minZ).putFloat(0.0f)
                .putFloat(maxX).putFloat(maxY).putFloat(maxZ).putFloat(0.0f);
    }

    public static StorageBuffer getAabbBuffer() {
        prepareAabbFrame();
        return aabbBuffers[Renderer.getCurrentFrame()];
    }

    public static long uploadAabbs(ByteBuffer aabbData) {
        prepareAabbFrame();
        StorageBuffer buffer = aabbBuffers[Renderer.getCurrentFrame()];
        buffer.recordCopyCmd(aabbData);
        return buffer.getOffset();
    }

    public static boolean prepare(VkCommandBuffer commandBuffer, Buffer aabbBuffer, long aabbOffset,
                                  IndirectBuffer indirectBuffer, long indirectOffset, int drawCount,
                                  Matrix4f viewProj, int flags) {
        if (drawCount <= 0 || commandBuffer == null || aabbBuffer == null || indirectBuffer == null
                || viewProj == null || !ensurePipeline()) {
            return false;
        }

        Renderer renderer = Renderer.getInstance();
        if (renderer == null || !Renderer.isRecording()) {
            return false;
        }

        boolean suspended = renderer.getBoundRenderPass() != null;
        if (suspended) {
            renderer.endRenderPass();
        }

        try (MemoryStack stack = stackPush()) {
            int effectiveFlags = flags & ~FLAG_COMPACT;

            VulkanImage pyramidImage = HiZPyramid.getSampleImage();
            if (pyramidImage == null) {
                pyramidImage = HiZPyramid.getOrCreateFallback(commandBuffer, stack);
                effectiveFlags &= ~FLAG_OCCLUSION;
            } else if (!HiZPyramid.isReady()) {
                effectiveFlags &= ~FLAG_OCCLUSION;
            }

            cullPipeline.bind(commandBuffer);
            cullPipeline.bindResources(commandBuffer,
                    new ComputePipeline.BufferResource(aabbBuffer.getId(), 0, VK_WHOLE_SIZE),
                    new ComputePipeline.BufferResource(indirectBuffer.getId(), 0, VK_WHOLE_SIZE),
                    new ComputePipeline.BufferResource(indirectBuffer.getId(), 0, VK_WHOLE_SIZE),
                    new ComputePipeline.BufferResource(indirectBuffer.getId(), 0, VK_WHOLE_SIZE),
                    new ComputePipeline.SamplerResource(pyramidImage.getImageView(), pyramidImage.getSampler(),
                            VK_IMAGE_LAYOUT_GENERAL));

            pushCullData(commandBuffer, stack, viewProj, drawCount, effectiveFlags,
                    (int) (indirectOffset >> 2), (int) (aabbOffset / AABB_STRIDE_BYTES), 0, 0);

            cullPipeline.dispatch(commandBuffer, (drawCount + WORKGROUP_SIZE - 1) / WORKGROUP_SIZE, 1, 1);

            indirectReadBarrier(commandBuffer, stack, indirectBuffer.getId(), indirectOffset,
                    (long) drawCount * INDIRECT_COMMAND_BYTES);
            return true;
        } catch (Throwable t) {
            Initializer.LOGGER.error("[LodCulling] prepare failed", t);
            return false;
        } finally {
            if (suspended) {
                renderer.getMainPass().rebindMainTarget();
            }
        }
    }

    public static boolean prepareCompact(VkCommandBuffer commandBuffer, Buffer aabbBuffer, long aabbOffset,
                                         Buffer srcCommands, long srcOffset, Buffer dstCommands, long dstOffset,
                                         Buffer countBuffer, long countOffset, int drawCount,
                                         Matrix4f viewProj, int flags) {
        if (drawCount <= 0 || commandBuffer == null || aabbBuffer == null || srcCommands == null
                || dstCommands == null || countBuffer == null || viewProj == null || !ensurePipeline()) {
            return false;
        }

        Renderer renderer = Renderer.getInstance();
        if (renderer == null || !Renderer.isRecording()) {
            return false;
        }

        boolean suspended = renderer.getBoundRenderPass() != null;
        if (suspended) {
            renderer.endRenderPass();
        }

        try (MemoryStack stack = stackPush()) {
            int effectiveFlags = flags | FLAG_COMPACT;

            VulkanImage pyramidImage = HiZPyramid.getSampleImage();
            if (pyramidImage == null) {
                pyramidImage = HiZPyramid.getOrCreateFallback(commandBuffer, stack);
                effectiveFlags &= ~FLAG_OCCLUSION;
            } else if (!HiZPyramid.isReady()) {
                effectiveFlags &= ~FLAG_OCCLUSION;
            }

            vkCmdFillBuffer(commandBuffer, countBuffer.getId(), countOffset, 4, 0);

            VkBufferMemoryBarrier.Buffer fillBarrier = VkBufferMemoryBarrier.calloc(1, stack);
            fillBarrier.sType$Default();
            fillBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            fillBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            fillBarrier.buffer(countBuffer.getId());
            fillBarrier.offset(countOffset);
            fillBarrier.size(4);
            fillBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            fillBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT);
            vkCmdPipelineBarrier(commandBuffer,
                    VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                    0, null, fillBarrier, null);

            cullPipeline.bind(commandBuffer);
            cullPipeline.bindResources(commandBuffer,
                    new ComputePipeline.BufferResource(aabbBuffer.getId(), 0, VK_WHOLE_SIZE),
                    new ComputePipeline.BufferResource(srcCommands.getId(), 0, VK_WHOLE_SIZE),
                    new ComputePipeline.BufferResource(dstCommands.getId(), 0, VK_WHOLE_SIZE),
                    new ComputePipeline.BufferResource(countBuffer.getId(), 0, VK_WHOLE_SIZE),
                    new ComputePipeline.SamplerResource(pyramidImage.getImageView(), pyramidImage.getSampler(),
                            VK_IMAGE_LAYOUT_GENERAL));

            pushCullData(commandBuffer, stack, viewProj, drawCount, effectiveFlags,
                    (int) (srcOffset >> 2), (int) (aabbOffset / AABB_STRIDE_BYTES),
                    (int) (dstOffset >> 2), (int) (countOffset >> 2));

            cullPipeline.dispatch(commandBuffer, (drawCount + WORKGROUP_SIZE - 1) / WORKGROUP_SIZE, 1, 1);

            VkBufferMemoryBarrier.Buffer drawBarriers = VkBufferMemoryBarrier.calloc(2, stack);
            fillBufferBarrier(drawBarriers.get(0), dstCommands.getId(), dstOffset,
                    (long) drawCount * INDIRECT_COMMAND_BYTES);
            fillBufferBarrier(drawBarriers.get(1), countBuffer.getId(), countOffset, 4);
            vkCmdPipelineBarrier(commandBuffer,
                    VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT,
                    0, null, drawBarriers, null);
            return true;
        } catch (Throwable t) {
            Initializer.LOGGER.error("[LodCulling] prepareCompact failed", t);
            return false;
        } finally {
            if (suspended) {
                renderer.getMainPass().rebindMainTarget();
            }
        }
    }

    private static void pushCullData(VkCommandBuffer commandBuffer, MemoryStack stack, Matrix4f viewProj,
                                     int drawCount, int flags, int cmdBaseUint, int aabbBaseRecord,
                                     int outCmdBaseUint, int countIndexUint) {
        ByteBuffer push = stack.malloc(PUSH_SIZE);
        viewProj.get(0, push);
        push.putInt(64, drawCount);
        push.putInt(68, flags);
        push.putInt(72, HiZPyramid.getMipCount());
        push.putInt(76, cmdBaseUint);
        push.putInt(80, aabbBaseRecord);
        push.putInt(84, outCmdBaseUint);
        push.putInt(88, countIndexUint);
        push.putInt(92, 0);
        push.putFloat(96, HiZPyramid.getWidth());
        push.putFloat(100, HiZPyramid.getHeight());
        push.putFloat(104, DEPTH_BIAS);
        push.putFloat(108, 0.0f);
        cullPipeline.pushConstants(commandBuffer, push);
    }

    private static void indirectReadBarrier(VkCommandBuffer commandBuffer, MemoryStack stack,
                                            long buffer, long offset, long size) {
        VkBufferMemoryBarrier.Buffer barrier = VkBufferMemoryBarrier.calloc(1, stack);
        fillBufferBarrier(barrier.get(0), buffer, offset, size);
        vkCmdPipelineBarrier(commandBuffer,
                VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT,
                0, null, barrier, null);
    }

    private static void fillBufferBarrier(VkBufferMemoryBarrier barrier, long buffer, long offset, long size) {
        barrier.sType$Default();
        barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        barrier.buffer(buffer);
        barrier.offset(offset);
        barrier.size(size);
        barrier.srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT);
        barrier.dstAccessMask(VK_ACCESS_INDIRECT_COMMAND_READ_BIT);
    }

    private static void prepareAabbFrame() {
        int framesNum = Renderer.getFramesNum();
        if (aabbBuffers == null || aabbBuffers.length != framesNum) {
            if (aabbBuffers != null) {
                for (StorageBuffer buffer : aabbBuffers) {
                    buffer.freeBuffer();
                }
            }
            aabbBuffers = new StorageBuffer[framesNum];
            for (int i = 0; i < framesNum; i++) {
                aabbBuffers[i] = new StorageBuffer(AABB_BUFFER_BYTES, MemoryTypes.HOST_MEM);
            }
            lastAabbFrame = -1;
        }

        int frame = Renderer.getCurrentFrame();
        if (frame != lastAabbFrame) {
            aabbBuffers[frame].reset();
            lastAabbFrame = frame;
        }
    }

    private static boolean ensurePipeline() {
        if (cullPipeline != null) {
            return true;
        }
        if (pipelineFailed) {
            return false;
        }
        try {
            cullPipeline = new ComputePipeline("basic/lod_cull/lod_cull",
                    new int[]{
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
                            VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
                            VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER
                    },
                    PUSH_SIZE);
            return true;
        } catch (Throwable t) {
            pipelineFailed = true;
            Initializer.LOGGER.error("[LodCulling] failed to create culling pipeline", t);
            return false;
        }
    }
}
