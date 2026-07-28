package net.vulkanmod.render;

import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.ReadbackBuffer;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkBufferMemoryBarrier;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageMemoryBarrier;

import java.nio.FloatBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public final class DepthSnapshot {
    private static final int MAX_GRID_WIDTH = 96;
    private static final int SLOT_BYTES = 65_536;
    private static final int MAX_TEXELS = SLOT_BYTES / Float.BYTES;
    private static final int STALE_FRAMES = 4;

    private static ReadbackBuffer[] ringBuffers;
    private static FloatBuffer[] mappedViews;
    private static Pending[] pendings;
    private static Grid[] grids;
    private static long frameCounter;
    private static volatile Grid published;

    private DepthSnapshot() {
    }

    private static final class Pending {
        final float[] viewProj = new float[16];
        final double[] camPos = new double[3];
        int width;
        int height;
        long frame;
        boolean recorded;
    }

    private static final class Grid {
        float[] depth;
        final float[] viewProj = new float[16];
        final double[] camPos = new double[3];
        int width;
        int height;
        long frame;
        boolean valid;
    }

    public static void beginFrame() {
        frameCounter++;
        if (pendings == null) {
            return;
        }
        int frame = Renderer.getCurrentFrame();
        if (frame < 0 || frame >= pendings.length) {
            return;
        }
        Pending pending = pendings[frame];
        if (!pending.recorded) {
            return;
        }
        pending.recorded = false;

        int texels = pending.width * pending.height;
        if (texels <= 0 || texels > MAX_TEXELS) {
            return;
        }

        Grid grid = grids[frame];
        if (grid.depth == null || grid.depth.length != texels) {
            grid.depth = new float[texels];
        }
        mappedViews[frame].get(0, grid.depth, 0, texels);
        System.arraycopy(pending.viewProj, 0, grid.viewProj, 0, 16);
        System.arraycopy(pending.camPos, 0, grid.camPos, 0, 3);
        grid.width = pending.width;
        grid.height = pending.height;
        grid.frame = pending.frame;
        grid.valid = true;
        published = grid;
    }

    public static void record(VkCommandBuffer commandBuffer) {
        if (commandBuffer == null || !HiZPyramid.isReady()) {
            return;
        }
        VulkanImage pyramid = HiZPyramid.getSampleImage();
        Vec3 cameraPos = WorldRenderer.getCameraPos();
        if (pyramid == null || cameraPos == null) {
            return;
        }

        int mipCount = HiZPyramid.getMipCount();
        int level = 0;
        int width = pyramid.width;
        int height = pyramid.height;
        while (level < mipCount - 1 && (width > MAX_GRID_WIDTH || width * height > MAX_TEXELS)) {
            level++;
            width = Math.max(1, pyramid.width >> level);
            height = Math.max(1, pyramid.height >> level);
        }
        if (width > MAX_GRID_WIDTH || width * height > MAX_TEXELS) {
            return;
        }

        ensureRing();
        int frame = Renderer.getCurrentFrame();
        if (ringBuffers == null || frame < 0 || frame >= ringBuffers.length) {
            return;
        }

        try (MemoryStack stack = stackPush()) {
            recordCopy(stack, commandBuffer, pyramid, level, width, height, ringBuffers[frame]);
        }

        MappedBuffer mvp = VRenderSystem.getExternalLodMVP();
        Pending pending = pendings[frame];
        for (int i = 0; i < 16; i++) {
            pending.viewProj[i] = mvp.getFloat(i * 4);
        }
        pending.camPos[0] = cameraPos.x();
        pending.camPos[1] = cameraPos.y();
        pending.camPos[2] = cameraPos.z();
        pending.width = width;
        pending.height = height;
        pending.frame = frameCounter;
        pending.recorded = true;
    }

    public static boolean available() {
        return current() != null;
    }

    public static int gridWidth() {
        Grid grid = current();
        return grid != null ? grid.width : 0;
    }

    public static int gridHeight() {
        Grid grid = current();
        return grid != null ? grid.height : 0;
    }

    public static float[] depthGrid() {
        Grid grid = current();
        return grid != null ? grid.depth : null;
    }

    public static float[] viewProj() {
        Grid grid = current();
        return grid != null ? grid.viewProj : null;
    }

    public static double[] cameraPos() {
        Grid grid = current();
        return grid != null ? grid.camPos : null;
    }

    private static Grid current() {
        if (!Initializer.CONFIG.lodDepthSnapshot) {
            return null;
        }
        Grid grid = published;
        if (grid == null || !grid.valid || grid.depth == null) {
            return null;
        }
        long age = frameCounter - grid.frame;
        int framesNum = Renderer.getFramesNum();
        if (age < framesNum || age > framesNum + STALE_FRAMES) {
            return null;
        }
        return grid;
    }

    private static void ensureRing() {
        int framesNum = Renderer.getFramesNum();
        if (ringBuffers != null && ringBuffers.length == framesNum) {
            return;
        }
        if (ringBuffers != null) {
            for (ReadbackBuffer buffer : ringBuffers) {
                buffer.freeBuffer();
            }
        }
        ringBuffers = new ReadbackBuffer[framesNum];
        mappedViews = new FloatBuffer[framesNum];
        pendings = new Pending[framesNum];
        grids = new Grid[framesNum];
        for (int i = 0; i < framesNum; i++) {
            ringBuffers[i] = new ReadbackBuffer(SLOT_BYTES);
            mappedViews[i] = ringBuffers[i].getByteBuffer().asFloatBuffer();
            pendings[i] = new Pending();
            grids[i] = new Grid();
        }
        published = null;
    }

    private static void recordCopy(MemoryStack stack, VkCommandBuffer commandBuffer, VulkanImage pyramid,
                                   int level, int width, int height, ReadbackBuffer target) {
        VkImageMemoryBarrier.Buffer toTransfer = VkImageMemoryBarrier.calloc(1, stack);
        toTransfer.sType$Default();
        toTransfer.oldLayout(VK_IMAGE_LAYOUT_GENERAL);
        toTransfer.newLayout(VK_IMAGE_LAYOUT_GENERAL);
        toTransfer.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        toTransfer.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        toTransfer.image(pyramid.getId());
        toTransfer.srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT);
        toTransfer.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
        toTransfer.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        toTransfer.subresourceRange().baseMipLevel(level);
        toTransfer.subresourceRange().levelCount(1);
        toTransfer.subresourceRange().baseArrayLayer(0);
        toTransfer.subresourceRange().layerCount(1);
        vkCmdPipelineBarrier(commandBuffer,
                VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                0, null, null, toTransfer);

        VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
        region.bufferOffset(0);
        region.bufferRowLength(0);
        region.bufferImageHeight(0);
        region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        region.imageSubresource().mipLevel(level);
        region.imageSubresource().baseArrayLayer(0);
        region.imageSubresource().layerCount(1);
        region.imageOffset().set(0, 0, 0);
        region.imageExtent().set(width, height, 1);
        vkCmdCopyImageToBuffer(commandBuffer, pyramid.getId(), VK_IMAGE_LAYOUT_GENERAL, target.getId(), region);

        VkBufferMemoryBarrier.Buffer toHost = VkBufferMemoryBarrier.calloc(1, stack);
        toHost.sType$Default();
        toHost.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        toHost.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        toHost.buffer(target.getId());
        toHost.offset(0);
        toHost.size((long) width * height * Float.BYTES);
        toHost.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
        toHost.dstAccessMask(VK_ACCESS_HOST_READ_BIT);

        VkImageMemoryBarrier.Buffer toCompute = VkImageMemoryBarrier.calloc(1, stack);
        toCompute.sType$Default();
        toCompute.oldLayout(VK_IMAGE_LAYOUT_GENERAL);
        toCompute.newLayout(VK_IMAGE_LAYOUT_GENERAL);
        toCompute.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        toCompute.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
        toCompute.image(pyramid.getId());
        toCompute.srcAccessMask(0);
        toCompute.dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT | VK_ACCESS_SHADER_READ_BIT);
        toCompute.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
        toCompute.subresourceRange().baseMipLevel(level);
        toCompute.subresourceRange().levelCount(1);
        toCompute.subresourceRange().baseArrayLayer(0);
        toCompute.subresourceRange().layerCount(1);

        vkCmdPipelineBarrier(commandBuffer,
                VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK_PIPELINE_STAGE_HOST_BIT | VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                0, null, toHost, toCompute);
    }
}
