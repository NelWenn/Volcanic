package net.vulkanmod.api;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.compat.external.ExternalTerrainRenderBridge;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.render.HiZPyramid;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.buffer.AreaBuffer;
import net.vulkanmod.render.chunk.buffer.DrawBuffers;
import net.vulkanmod.render.chunk.buffer.UploadManager;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public final class CalderaBridge {

    public static final int PASS_FLAT_OPAQUE = 0;
    public static final int PASS_TEX_OPAQUE = 1;
    public static final int PASS_FLAT_WATER = 2;
    public static final int PASS_TEX_WATER = 3;
    public static final int PASS_FLAT_OPAQUE_SOLID = 4;
    public static final int PASS_TEX_OPAQUE_SOLID = 5;

    private static final int INPUT_VERTEX_SIZE_BYTES = 16;
    private static final int INTERNAL_VERTEX_SIZE_BYTES = CustomVertexFormat.EXTERNAL_LOD.getVertexSize();

    private static final int INPUT_TEXTURED_VERTEX_SIZE_BYTES = 40;
    private static final int INTERNAL_TEXTURED_VERTEX_SIZE_BYTES = CustomVertexFormat.EXTERNAL_LOD_TEXTURED.getVertexSize();

    private static final int Y_BIAS = 2048;
    private static final int POSITION_MAX = 0xFFFF;

    private static final int ARENA_INDEX_SIZE_BYTES = 4;
    private static final int OPAQUE_ARENA_VERTEX_BYTES = 4_194_304;
    private static final int OPAQUE_ARENA_INDEX_BYTES = 2_097_152;
    private static final int WATER_ARENA_VERTEX_BYTES = 1_048_576;
    private static final int WATER_ARENA_INDEX_BYTES = 524_288;

    private static final int ORIGIN_SLOTS = 1024;
    private static final int ORIGIN_STRIDE_BYTES = 16;
    private static final int MAX_BATCH_CELLS = ORIGIN_SLOTS - 1;
    private static final int INDIRECT_COMMAND_BYTES = 20;
    private static final int INDIRECT_BUFFER_BYTES = 262_144;

    private static final Matrix4f COMBINED_MATRIX_SCRATCH = new Matrix4f();
    private static final int LIGHTMAP_TEXTURE_SLOT = 0;
    private static final int LIGHTMAP_SOURCE_SLOT = 2;
    private static final int BLOCK_ATLAS_TEXTURE_SLOT = 3;

    private static final Int2ObjectOpenHashMap<MeshHandle> HANDLES = new Int2ObjectOpenHashMap<>();
    private static int nextHandle = 1;

    private static final LodArena[] ARENAS = new LodArena[4];

    private static final MappedBuffer CELL_ORIGINS = new MappedBuffer(ORIGIN_SLOTS * ORIGIN_STRIDE_BYTES);
    private static final MappedBuffer FOG_COLOR = new MappedBuffer(4 * Float.BYTES);
    private static final MappedBuffer FOG_PARAMS = new MappedBuffer(4 * Float.BYTES);
    private static final ByteBuffer BATCH_COMMANDS = MemoryUtil.memAlloc(MAX_BATCH_CELLS * INDIRECT_COMMAND_BYTES);
    private static final long BATCH_COMMANDS_PTR = MemoryUtil.memAddress0(BATCH_COMMANDS);
    private static final ByteBuffer BATCH_AABBS = MemoryUtil.memAlloc(MAX_BATCH_CELLS * LodCulling.AABB_STRIDE_BYTES);
    private static final long BATCH_AABBS_PTR = MemoryUtil.memAddress0(BATCH_AABBS);
    private static final Matrix4f CULL_MATRIX_SCRATCH = new Matrix4f();

    static {
        MemoryUtil.memSet(CELL_ORIGINS.ptr, 0, ORIGIN_SLOTS * ORIGIN_STRIDE_BYTES);
        writeFogUniforms();
    }

    private static IndirectBuffer[] indirectBuffers;
    private static int lastIndirectFrame = -1;
    private static boolean arenaUploadsPending;

    private static boolean indexBoundsWarned = false;

    private static volatile float clipDistance = 0.0f;

    private static volatile float fogStartBlocks = Float.MAX_VALUE;
    private static volatile float fogEndBlocks = Float.MAX_VALUE;
    private static volatile float fadeBandStart = 0.0f;
    private static volatile float fadeBandEnd = 0.0f;
    private static volatile float fogRed = 0.0f;
    private static volatile float fogGreen = 0.0f;
    private static volatile float fogBlue = 0.0f;

    private static boolean lodSessionActive;
    private static boolean pyramidReady;

    private static GraphicsPipeline batchPipeline;
    private static boolean batchIndirect;
    private static boolean batchCull;
    private static int batchDrawCount;
    private static int batchBoundArena = -1;
    private static boolean batchTexturesSaved;
    private static boolean batchAtlasSaved;
    private static boolean batchWaterStateSaved;
    private static VulkanImage batchPrevSlot0;
    private static VulkanImage batchPrevSlot3;
    private static boolean batchPrevDepthTest;
    private static boolean batchPrevDepthMask;
    private static boolean batchPrevBlendEnabled;
    private static int batchPrevSrcRgb;
    private static int batchPrevDstRgb;
    private static int batchPrevSrcAlpha;
    private static int batchPrevDstAlpha;
    private static int batchPrevBlendOp;
    private static int batchPrevBlendOpRgb;
    private static int batchPrevBlendOpAlpha;
    private static double batchCamX;
    private static double batchCamY;
    private static double batchCamZ;

    private CalderaBridge() {
    }

    private record MeshHandle(int arena, int indexCount, int firstIndex, int vertexOffset,
                              float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                              int surfaceIndexCount) {
    }

    private static volatile boolean drawCaves = true;

    private static final class LodArena {
        final AreaBuffer vertexBuffer;
        final AreaBuffer indexBuffer;
        final int vertexSize;

        LodArena(int vertexSize, int vertexBytes, int indexBytes) {
            this.vertexSize = vertexSize;
            this.vertexBuffer = new AreaBuffer(AreaBuffer.Usage.VERTEX, vertexBytes / vertexSize, vertexSize);
            this.indexBuffer = new AreaBuffer(AreaBuffer.Usage.INDEX, indexBytes / ARENA_INDEX_SIZE_BYTES, ARENA_INDEX_SIZE_BYTES);
        }
    }

    public static MappedBuffer getCellOrigins() {
        return CELL_ORIGINS;
    }

    public static MappedBuffer getFogColor() {
        return FOG_COLOR;
    }

    public static MappedBuffer getFogParams() {
        return FOG_PARAMS;
    }

    public static void onFrameBegin(VkCommandBuffer commandBuffer) {
        pyramidReady = false;

        if (!lodSessionActive || commandBuffer == null
                || !Initializer.CONFIG.lodGpuCulling
                || !Initializer.CONFIG.indirectDraw
                || !DeviceManager.supportsFastIndirectDraw()
                || !LodCulling.isAvailable()) {
            return;
        }

        try {
            pyramidReady = HiZPyramid.build(commandBuffer);
        } catch (Throwable t) {
            pyramidReady = false;
            Initializer.LOGGER.error("[Caldera] HiZ pyramid build failed", t);
        }
    }

    private static LodArena arenaFor(int pass) {
        LodArena arena = ARENAS[pass];
        if (arena == null) {
            boolean textured = pass == PASS_TEX_OPAQUE || pass == PASS_TEX_WATER;
            boolean water = pass == PASS_FLAT_WATER || pass == PASS_TEX_WATER;
            int vertexSize = textured ? INTERNAL_TEXTURED_VERTEX_SIZE_BYTES : INTERNAL_VERTEX_SIZE_BYTES;
            int vertexBytes = water ? WATER_ARENA_VERTEX_BYTES : OPAQUE_ARENA_VERTEX_BYTES;
            int indexBytes = water ? WATER_ARENA_INDEX_BYTES : OPAQUE_ARENA_INDEX_BYTES;
            arena = new LodArena(vertexSize, vertexBytes, indexBytes);
            ARENAS[pass] = arena;
        }
        return arena;
    }

    public static boolean isReady() {
        try {
            return Renderer.getInstance() != null && Renderer.isRecording();
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setClipDistance(float blocks) {
        clipDistance = blocks > 0.0f ? blocks : 0.0f;
    }

    public static void setLodFog(float startBlocks, float endBlocks, float r, float g, float b) {
        fogStartBlocks = startBlocks;
        fogEndBlocks = endBlocks;
        fogRed = r;
        fogGreen = g;
        fogBlue = b;
    }

    public static void setDrawCaves(boolean value) {
        drawCaves = value;
    }

    public static void setLodFadeBand(float startBlocks, float endBlocks) {
        fadeBandStart = startBlocks;
        fadeBandEnd = endBlocks;
    }

    public static void setSurfaceIndexCount(int handle, int surfaceIndexCount) {
        MeshHandle mesh = HANDLES.get(handle);
        if (mesh == null) {
            return;
        }
        int clamped = Math.max(0, Math.min(surfaceIndexCount, mesh.indexCount()));
        HANDLES.put(handle, new MeshHandle(mesh.arena(), mesh.indexCount(), mesh.firstIndex(), mesh.vertexOffset(),
                mesh.minX(), mesh.minY(), mesh.minZ(), mesh.maxX(), mesh.maxY(), mesh.maxZ(), clamped));
    }

    public static int uploadMesh(ByteBuffer vertices, int vertexCount, ByteBuffer indices, int indexCount, boolean intIndices) {
        try {
            return uploadMeshInternal(vertices, vertexCount, indices, indexCount, intIndices, PASS_FLAT_OPAQUE);
        } catch (Throwable t) {
            Initializer.LOGGER.error("[Caldera] uploadMesh failed", t);
            return 0;
        }
    }

    public static void drawMesh(int handle, double cellOriginX, double cellOriginZ) {
        try {
            drawMeshInternal(handle, cellOriginX, cellOriginZ);
        } catch (Throwable t) {
            Initializer.LOGGER.error("[Caldera] drawMesh failed", t);
        }
    }

    public static void freeMesh(int handle) {
        try {
            MeshHandle mesh = HANDLES.remove(handle);
            if (mesh != null) {
                LodArena arena = ARENAS[mesh.arena()];
                if (arena != null) {
                    MemoryManager.getInstance().addToFreeSegment(arena.vertexBuffer, mesh.vertexOffset());
                    MemoryManager.getInstance().addToFreeSegment(arena.indexBuffer, mesh.firstIndex());
                }
            }
        } catch (Throwable t) {
            Initializer.LOGGER.error("[Caldera] freeMesh failed", t);
        }
    }

    public static int uploadTexturedMesh(ByteBuffer vertices, int vertexCount, ByteBuffer indices, int indexCount, boolean intIndices) {
        try {
            return uploadTexturedMeshInternal(vertices, vertexCount, indices, indexCount, intIndices, PASS_TEX_OPAQUE);
        } catch (Throwable t) {
            Initializer.LOGGER.error("[Caldera] uploadTexturedMesh failed", t);
            return 0;
        }
    }

    public static void drawTexturedMesh(int handle, double cellOriginX, double cellOriginZ) {
        try {
            drawTexturedMeshInternal(handle, cellOriginX, cellOriginZ);
        } catch (Throwable t) {
            Initializer.LOGGER.error("[Caldera] drawTexturedMesh failed", t);
        }
    }

    public static int uploadTexturedWaterMesh(ByteBuffer vertices, int vertexCount, ByteBuffer indices, int indexCount, boolean intIndices) {
        try {
            return uploadTexturedMeshInternal(vertices, vertexCount, indices, indexCount, intIndices, PASS_TEX_WATER);
        } catch (Throwable t) {
            Initializer.LOGGER.error("[Caldera] uploadTexturedWaterMesh failed", t);
            return 0;
        }
    }

    public static void drawTexturedWaterMesh(int handle, double cellOriginX, double cellOriginZ) {
        try {
            drawTexturedWaterMeshInternal(handle, cellOriginX, cellOriginZ);
        } catch (Throwable t) {
            Initializer.LOGGER.error("[Caldera] drawTexturedWaterMesh failed", t);
        }
    }

    public static int uploadWaterMesh(ByteBuffer vertices, int vertexCount, ByteBuffer indices, int indexCount, boolean intIndices) {
        try {
            return uploadMeshInternal(vertices, vertexCount, indices, indexCount, intIndices, PASS_FLAT_WATER);
        } catch (Throwable t) {
            Initializer.LOGGER.error("[Caldera] uploadWaterMesh failed", t);
            return 0;
        }
    }

    public static void drawWaterMesh(int handle, double cellOriginX, double cellOriginZ) {
        try {
            drawWaterMeshInternal(handle, cellOriginX, cellOriginZ);
        } catch (Throwable t) {
            Initializer.LOGGER.error("[Caldera] drawWaterMesh failed", t);
        }
    }

    public static boolean beginLodDraw(int pass) {
        try {
            return beginLodDrawInternal(pass);
        } catch (Throwable t) {
            Initializer.LOGGER.error("[Caldera] beginLodDraw failed", t);
            try {
                endLodDrawInternal();
            } catch (Throwable ignored) {
            }
            return false;
        }
    }

    public static void drawLodCell(int handle, double cellOriginX, double cellOriginZ) {
        try {
            drawLodCellInternal(handle, cellOriginX, cellOriginZ);
        } catch (Throwable t) {
            Initializer.LOGGER.error("[Caldera] drawLodCell failed", t);
        }
    }

    public static void endLodDraw() {
        try {
            endLodDrawInternal();
        } catch (Throwable t) {
            Initializer.LOGGER.error("[Caldera] endLodDraw failed", t);
        }
    }

    private static boolean beginLodDrawInternal(int pass) {
        if (batchPipeline != null || batchTexturesSaved || batchWaterStateSaved) {
            batchDrawCount = 0;
            endLodDrawInternal();
        }
        if (!isReady()) {
            return false;
        }

        GraphicsPipeline pipeline = switch (pass) {
            case PASS_FLAT_OPAQUE -> PipelineManager.getExternalLodPipeline();
            case PASS_TEX_OPAQUE -> PipelineManager.getExternalLodTexturedPipeline();
            case PASS_FLAT_WATER -> PipelineManager.getExternalLodWaterPipeline();
            case PASS_TEX_WATER -> PipelineManager.getExternalLodWaterTexturedPipeline();
            case PASS_FLAT_OPAQUE_SOLID -> PipelineManager.getExternalLodSolidPipeline();
            case PASS_TEX_OPAQUE_SOLID -> PipelineManager.getExternalLodTexturedSolidPipeline();
            default -> null;
        };
        if (pipeline == null) {
            return false;
        }

        boolean textured = pass == PASS_TEX_OPAQUE || pass == PASS_TEX_WATER || pass == PASS_TEX_OPAQUE_SOLID;
        boolean water = pass == PASS_FLAT_WATER || pass == PASS_TEX_WATER;

        if (arenaUploadsPending) {
            UploadManager.INSTANCE.submitUploads();
            arenaUploadsPending = false;
        }

        Vec3 liveCam = WorldRenderer.getCameraPos();
        batchCamX = liveCam != null ? liveCam.x() : 0.0;
        batchCamY = liveCam != null ? liveCam.y() : 0.0;
        batchCamZ = liveCam != null ? liveCam.z() : 0.0;

        writeSharedUniforms();

        batchPrevSlot0 = VTextureSelector.getImage(LIGHTMAP_TEXTURE_SLOT);
        batchPrevSlot3 = textured ? VTextureSelector.getImage(BLOCK_ATLAS_TEXTURE_SLOT) : null;
        batchTexturesSaved = true;
        batchAtlasSaved = textured;
        bindLightmap();

        if (textured && !bindAtlas()) {
            endLodDrawInternal();
            return false;
        }

        if (water) {
            batchPrevDepthTest = VRenderSystem.depthTest;
            batchPrevDepthMask = VRenderSystem.depthMask;
            PipelineState.BlendInfo bi = PipelineState.blendInfo;
            batchPrevBlendEnabled = bi.enabled;
            batchPrevSrcRgb = bi.srcRgbFactor;
            batchPrevDstRgb = bi.dstRgbFactor;
            batchPrevSrcAlpha = bi.srcAlphaFactor;
            batchPrevDstAlpha = bi.dstAlphaFactor;
            batchPrevBlendOp = bi.blendOp;
            batchPrevBlendOpRgb = bi.blendOpRgb;
            batchPrevBlendOpAlpha = bi.blendOpAlpha;
            batchWaterStateSaved = true;

            VRenderSystem.depthTest = true;
            VRenderSystem.depthMask = false;
            bi.enabled = true;
            bi.srcRgbFactor = VK10.VK_BLEND_FACTOR_SRC_ALPHA;
            bi.dstRgbFactor = VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            bi.srcAlphaFactor = VK10.VK_BLEND_FACTOR_ONE;
            bi.dstAlphaFactor = VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            bi.blendOp = bi.blendOpRgb = bi.blendOpAlpha = VK10.VK_BLEND_OP_ADD;
        }

        Renderer renderer = Renderer.getInstance();
        if (!renderer.bindGraphicsPipeline(pipeline)) {
            endLodDrawInternal();
            return false;
        }

        boolean indirect = Initializer.CONFIG.indirectDraw && DeviceManager.supportsFastIndirectDraw();
        if (indirect) {
            prepareIndirectFrame();
            pushOrigin(pipeline, 0.0f, (float) (-batchCamY - Y_BIAS), 0.0f);
            batchDrawCount = 0;
            batchCull = Initializer.CONFIG.lodGpuCulling && LodCulling.isAvailable();
        } else {
            renderer.uploadAndBindUBOs(pipeline);
            batchCull = false;
        }

        batchIndirect = indirect;
        batchBoundArena = -1;
        batchPipeline = pipeline;
        return true;
    }

    private static void drawLodCellInternal(int handle, double cellOriginX, double cellOriginZ) {
        GraphicsPipeline pipeline = batchPipeline;
        if (pipeline == null) {
            return;
        }

        MeshHandle mesh = HANDLES.get(handle);
        if (mesh == null) {
            return;
        }

        int drawIndexCount = drawCaves ? mesh.indexCount() : mesh.surfaceIndexCount();
        if (drawIndexCount <= 0) {
            return;
        }

        LodArena arena = ARENAS[mesh.arena()];
        if (arena == null) {
            return;
        }

        if (batchIndirect) {
            if (mesh.arena() != batchBoundArena) {
                flushBatch();
                bindArena(arena);
                batchBoundArena = mesh.arena();
            } else if (batchDrawCount == MAX_BATCH_CELLS) {
                flushBatch();
            }

            int slot = batchDrawCount + 1;
            int originOffset = slot * ORIGIN_STRIDE_BYTES;
            CELL_ORIGINS.putFloat(originOffset, (float) (cellOriginX - batchCamX));
            CELL_ORIGINS.putFloat(originOffset + 4, 0.0f);
            CELL_ORIGINS.putFloat(originOffset + 8, (float) (cellOriginZ - batchCamZ));
            CELL_ORIGINS.putFloat(originOffset + 12, 0.0f);

            long ptr = BATCH_COMMANDS_PTR + (long) batchDrawCount * INDIRECT_COMMAND_BYTES;
            MemoryUtil.memPutInt(ptr, drawIndexCount);
            MemoryUtil.memPutInt(ptr + 4, 1);
            MemoryUtil.memPutInt(ptr + 8, mesh.firstIndex());
            MemoryUtil.memPutInt(ptr + 12, mesh.vertexOffset());
            MemoryUtil.memPutInt(ptr + 16, slot);

            if (batchCull) {
                long aabbPtr = BATCH_AABBS_PTR + (long) batchDrawCount * LodCulling.AABB_STRIDE_BYTES;
                float baseX = (float) (cellOriginX - batchCamX);
                float baseZ = (float) (cellOriginZ - batchCamZ);
                MemoryUtil.memPutFloat(aabbPtr, baseX + mesh.minX());
                MemoryUtil.memPutFloat(aabbPtr + 4, (float) (mesh.minY() - batchCamY));
                MemoryUtil.memPutFloat(aabbPtr + 8, baseZ + mesh.minZ());
                MemoryUtil.memPutFloat(aabbPtr + 12, 0.0f);
                MemoryUtil.memPutFloat(aabbPtr + 16, baseX + mesh.maxX());
                MemoryUtil.memPutFloat(aabbPtr + 20, (float) (mesh.maxY() - batchCamY));
                MemoryUtil.memPutFloat(aabbPtr + 24, baseZ + mesh.maxZ());
                MemoryUtil.memPutFloat(aabbPtr + 28, 0.0f);
            }

            batchDrawCount++;
            return;
        }

        if (mesh.arena() != batchBoundArena) {
            bindArena(arena);
            batchBoundArena = mesh.arena();
        }

        pushOrigin(pipeline, (float) (cellOriginX - batchCamX), (float) (-batchCamY - Y_BIAS), (float) (cellOriginZ - batchCamZ));
        VK10.vkCmdDrawIndexed(Renderer.getCommandBuffer(), drawIndexCount, 1, mesh.firstIndex(), mesh.vertexOffset(), 0);
    }

    private static void pushOrigin(GraphicsPipeline pipeline, float x, float y, float z) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pushData = stack.malloc(16);
            pushData.putFloat(0, x);
            pushData.putFloat(4, y);
            pushData.putFloat(8, z);
            pushData.putFloat(12, 0.0f);
            VK10.vkCmdPushConstants(Renderer.getCommandBuffer(), pipeline.getLayout(), VK10.VK_SHADER_STAGE_VERTEX_BIT, 0, pushData);
        }
    }

    private static void bindArena(LodArena arena) {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VK10.nvkCmdBindVertexBuffers(commandBuffer, 0, 1, stack.npointer(arena.vertexBuffer.getId()), stack.npointer(0L));
        }
        VK10.vkCmdBindIndexBuffer(commandBuffer, arena.indexBuffer.getId(), 0, VK10.VK_INDEX_TYPE_UINT32);
    }

    private static void prepareIndirectFrame() {
        int framesNum = Renderer.getFramesNum();
        if (indirectBuffers == null || indirectBuffers.length != framesNum) {
            if (indirectBuffers != null) {
                for (IndirectBuffer buffer : indirectBuffers) {
                    buffer.freeBuffer();
                }
            }
            indirectBuffers = new IndirectBuffer[framesNum];
            for (int i = 0; i < framesNum; i++) {
                indirectBuffers[i] = new IndirectBuffer(INDIRECT_BUFFER_BYTES, MemoryTypes.HOST_MEM);
            }
            lastIndirectFrame = -1;
        }

        int frame = Renderer.getCurrentFrame();
        if (frame != lastIndirectFrame) {
            indirectBuffers[frame].reset();
            lastIndirectFrame = frame;
        }
    }

    private static void flushBatch() {
        if (batchDrawCount == 0) {
            return;
        }

        Renderer.getInstance().uploadAndBindUBOs(batchPipeline);

        IndirectBuffer indirectBuffer = indirectBuffers[Renderer.getCurrentFrame()];
        BATCH_COMMANDS.position(0);
        BATCH_COMMANDS.limit(batchDrawCount * INDIRECT_COMMAND_BYTES);
        indirectBuffer.recordCopyCmd(BATCH_COMMANDS);
        BATCH_COMMANDS.clear();

        long indirectOffset = indirectBuffer.getOffset();
        if (dispatchCull(indirectBuffer, indirectOffset, batchDrawCount)) {
            pushOrigin(batchPipeline, 0.0f, (float) (-batchCamY - Y_BIAS), 0.0f);
        }

        VK10.vkCmdDrawIndexedIndirect(Renderer.getCommandBuffer(), indirectBuffer.getId(), indirectOffset, batchDrawCount, INDIRECT_COMMAND_BYTES);
        batchDrawCount = 0;
    }

    private static boolean dispatchCull(IndirectBuffer indirectBuffer, long indirectOffset, int drawCount) {
        if (!batchCull) {
            return false;
        }

        try {
            BATCH_AABBS.position(0);
            BATCH_AABBS.limit(drawCount * LodCulling.AABB_STRIDE_BYTES);
            long aabbOffset = LodCulling.uploadAabbs(BATCH_AABBS);
            BATCH_AABBS.clear();

            FloatBuffer mvpSrc = VRenderSystem.getExternalLodMVP().buffer.asFloatBuffer();
            mvpSrc.position(0);
            CULL_MATRIX_SCRATCH.set(mvpSrc);

            int flags = pyramidReady ? LodCulling.FLAG_OCCLUSION : 0;
            return LodCulling.prepare(Renderer.getCommandBuffer(), LodCulling.getAabbBuffer(), aabbOffset,
                    indirectBuffer, indirectOffset, drawCount, CULL_MATRIX_SCRATCH, flags);
        } catch (Throwable t) {
            batchCull = false;
            Initializer.LOGGER.error("[Caldera] LOD cull dispatch failed", t);
            return false;
        }
    }

    private static void endLodDrawInternal() {
        if (batchIndirect) {
            flushBatch();
            batchIndirect = false;
        }
        batchCull = false;
        batchBoundArena = -1;
        batchPipeline = null;
        if (batchWaterStateSaved) {
            batchWaterStateSaved = false;
            VRenderSystem.depthTest = batchPrevDepthTest;
            VRenderSystem.depthMask = batchPrevDepthMask;
            PipelineState.BlendInfo bi = PipelineState.blendInfo;
            bi.enabled = batchPrevBlendEnabled;
            bi.srcRgbFactor = batchPrevSrcRgb;
            bi.dstRgbFactor = batchPrevDstRgb;
            bi.srcAlphaFactor = batchPrevSrcAlpha;
            bi.dstAlphaFactor = batchPrevDstAlpha;
            bi.blendOp = batchPrevBlendOp;
            bi.blendOpRgb = batchPrevBlendOpRgb;
            bi.blendOpAlpha = batchPrevBlendOpAlpha;
        }
        if (batchTexturesSaved) {
            batchTexturesSaved = false;
            VTextureSelector.bindTexture(LIGHTMAP_TEXTURE_SLOT, batchPrevSlot0);
            if (batchAtlasSaved) {
                batchAtlasSaved = false;
                VTextureSelector.bindTexture(BLOCK_ATLAS_TEXTURE_SLOT, batchPrevSlot3);
            }
            batchPrevSlot0 = null;
            batchPrevSlot3 = null;
        }
    }

    private static int uploadMeshInternal(ByteBuffer vertices, int vertexCount, ByteBuffer indices, int indexCount, boolean intIndices, int pass) {
        if (vertices == null || indices == null || vertexCount <= 0 || indexCount <= 0 || indexCount % 3 != 0) {
            return 0;
        }

        ByteBuffer vertexSrc = vertices.duplicate();
        vertexSrc.order(ByteOrder.LITTLE_ENDIAN);
        if (vertexSrc.remaining() < vertexCount * INPUT_VERTEX_SIZE_BYTES) {
            return 0;
        }

        int indexSize = intIndices ? 4 : 2;
        int indexBytesNeeded = indexCount * indexSize;
        ByteBuffer indexSrc = indices.duplicate();
        if (indexSrc.remaining() < indexBytesNeeded) {
            return 0;
        }

        ByteBuffer indexScan = indexSrc.duplicate();
        indexScan.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < indexCount; i++) {
            int idx = intIndices ? indexScan.getInt() : (indexScan.getShort() & 0xFFFF);
            if (idx < 0 || idx >= vertexCount) {
                if (!indexBoundsWarned) {
                    indexBoundsWarned = true;
                    Initializer.LOGGER.error("[Caldera] uploadMesh: index {} out of bounds (vertexCount={})", idx, vertexCount);
                }
                return 0;
            }
        }

        int vertexOffset;
        int firstIndex;

        float boundMinX = Float.POSITIVE_INFINITY;
        float boundMinY = Float.POSITIVE_INFINITY;
        float boundMinZ = Float.POSITIVE_INFINITY;
        float boundMaxX = Float.NEGATIVE_INFINITY;
        float boundMaxY = Float.NEGATIVE_INFINITY;
        float boundMaxZ = Float.NEGATIVE_INFINITY;

        ByteBuffer internalVertices = MemoryUtil.memAlloc(vertexCount * INTERNAL_VERTEX_SIZE_BYTES);
        try {
            internalVertices.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < vertexCount; i++) {
                float x = vertexSrc.getFloat();
                float y = vertexSrc.getFloat();
                float z = vertexSrc.getFloat();
                int c = vertexSrc.getInt();
                boundMinX = Math.min(boundMinX, x);
                boundMinY = Math.min(boundMinY, y);
                boundMinZ = Math.min(boundMinZ, z);
                boundMaxX = Math.max(boundMaxX, x);
                boundMaxY = Math.max(boundMaxY, y);
                boundMaxZ = Math.max(boundMaxZ, z);
                int light = (c >>> 24) & 0xFF;
                byte r = (byte) ((c >> 16) & 0xFF);
                byte g = (byte) ((c >>  8) & 0xFF);
                byte b = (byte) ( c        & 0xFF);

                internalVertices.putShort((short) quantizeXZ(x));
                internalVertices.putShort((short) quantizeY(y));
                internalVertices.putShort((short) quantizeXZ(z));
                internalVertices.putShort((short) light);
                internalVertices.put(r).put(g).put(b).put((byte) 0xFF);
            }
            internalVertices.position(0);

            ByteBuffer internalIndices = MemoryUtil.memAlloc(indexCount * ARENA_INDEX_SIZE_BYTES);
            try {
                writeArenaIndices(internalIndices, indexSrc, indexCount, intIndices);
                internalIndices.position(0);

                LodArena arena = arenaFor(pass);
                vertexOffset = arena.vertexBuffer.upload(internalVertices, -1, new DrawBuffers.DrawParameters()).getOffset() / INTERNAL_VERTEX_SIZE_BYTES;
                firstIndex = arena.indexBuffer.upload(internalIndices, -1, new DrawBuffers.DrawParameters()).getOffset() / ARENA_INDEX_SIZE_BYTES;
            } finally {
                MemoryUtil.memFree(internalIndices);
            }
        } finally {
            MemoryUtil.memFree(internalVertices);
        }
        arenaUploadsPending = true;

        int handle = nextHandle++;
        if (nextHandle == 0) {
            nextHandle = 1;
        }
        HANDLES.put(handle, new MeshHandle(pass, indexCount, firstIndex, vertexOffset,
                boundMinX, boundMinY, boundMinZ, boundMaxX, boundMaxY, boundMaxZ, indexCount));
        lodSessionActive = true;
        return handle;
    }

    private static void writeArenaIndices(ByteBuffer dst, ByteBuffer src, int indexCount, boolean intIndices) {
        dst.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer indices = src.duplicate();
        indices.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < indexCount; i++) {
            dst.putInt(intIndices ? indices.getInt() : (indices.getShort() & 0xFFFF));
        }
    }

    private static void drawMeshInternal(int handle, double cellOriginX, double cellOriginZ) {
        if (!beginLodDrawInternal(PASS_FLAT_OPAQUE)) {
            return;
        }
        try {
            drawLodCellInternal(handle, cellOriginX, cellOriginZ);
        } finally {
            endLodDrawInternal();
        }
    }

    private static int uploadTexturedMeshInternal(ByteBuffer vertices, int vertexCount, ByteBuffer indices, int indexCount, boolean intIndices, int pass) {
        if (vertices == null || indices == null || vertexCount <= 0 || indexCount <= 0 || indexCount % 3 != 0) {
            return 0;
        }

        ByteBuffer vertexSrc = vertices.duplicate();
        vertexSrc.order(ByteOrder.LITTLE_ENDIAN);
        if (vertexSrc.remaining() < vertexCount * INPUT_TEXTURED_VERTEX_SIZE_BYTES) {
            return 0;
        }

        int indexSize = intIndices ? 4 : 2;
        int indexBytesNeeded = indexCount * indexSize;
        ByteBuffer indexSrc = indices.duplicate();
        if (indexSrc.remaining() < indexBytesNeeded) {
            return 0;
        }

        ByteBuffer indexScan = indexSrc.duplicate();
        indexScan.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < indexCount; i++) {
            int idx = intIndices ? indexScan.getInt() : (indexScan.getShort() & 0xFFFF);
            if (idx < 0 || idx >= vertexCount) {
                if (!indexBoundsWarned) {
                    indexBoundsWarned = true;
                    Initializer.LOGGER.error("[Caldera] uploadTexturedMesh: index {} out of bounds (vertexCount={})", idx, vertexCount);
                }
                return 0;
            }
        }

        int vertexOffset;
        int firstIndex;

        float boundMinX = Float.POSITIVE_INFINITY;
        float boundMinY = Float.POSITIVE_INFINITY;
        float boundMinZ = Float.POSITIVE_INFINITY;
        float boundMaxX = Float.NEGATIVE_INFINITY;
        float boundMaxY = Float.NEGATIVE_INFINITY;
        float boundMaxZ = Float.NEGATIVE_INFINITY;

        ByteBuffer internalVertices = MemoryUtil.memAlloc(vertexCount * INTERNAL_TEXTURED_VERTEX_SIZE_BYTES);
        try {
            internalVertices.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < vertexCount; i++) {
                float x = vertexSrc.getFloat();
                float y = vertexSrc.getFloat();
                float z = vertexSrc.getFloat();
                int c = vertexSrc.getInt();
                boundMinX = Math.min(boundMinX, x);
                boundMinY = Math.min(boundMinY, y);
                boundMinZ = Math.min(boundMinZ, z);
                boundMaxX = Math.max(boundMaxX, x);
                boundMaxY = Math.max(boundMaxY, y);
                boundMaxZ = Math.max(boundMaxZ, z);
                float u0 = vertexSrc.getFloat();
                float v0 = vertexSrc.getFloat();
                float u1 = vertexSrc.getFloat();
                float v1 = vertexSrc.getFloat();
                float tileU = vertexSrc.getFloat();
                float tileV = vertexSrc.getFloat();

                int light = (c >>> 24) & 0xFF;
                byte r = (byte) ((c >> 16) & 0xFF);
                byte g = (byte) ((c >>  8) & 0xFF);
                byte b = (byte) ( c        & 0xFF);

                internalVertices.putShort((short) quantizeXZ(x));
                internalVertices.putShort((short) quantizeY(y));
                internalVertices.putShort((short) quantizeXZ(z));
                internalVertices.putShort((short) light);
                internalVertices.put(r).put(g).put(b).put((byte) 0xFF);
                internalVertices.putShort(unorm16(u0));
                internalVertices.putShort(unorm16(v0));
                internalVertices.putShort(unorm16(u1));
                internalVertices.putShort(unorm16(v1));
                internalVertices.putShort((short) (Math.round(tileU) + Y_BIAS));
                internalVertices.putShort((short) (Math.round(tileV) + Y_BIAS));
            }
            internalVertices.position(0);

            ByteBuffer internalIndices = MemoryUtil.memAlloc(indexCount * ARENA_INDEX_SIZE_BYTES);
            try {
                writeArenaIndices(internalIndices, indexSrc, indexCount, intIndices);
                internalIndices.position(0);

                LodArena arena = arenaFor(pass);
                vertexOffset = arena.vertexBuffer.upload(internalVertices, -1, new DrawBuffers.DrawParameters()).getOffset() / INTERNAL_TEXTURED_VERTEX_SIZE_BYTES;
                firstIndex = arena.indexBuffer.upload(internalIndices, -1, new DrawBuffers.DrawParameters()).getOffset() / ARENA_INDEX_SIZE_BYTES;
            } finally {
                MemoryUtil.memFree(internalIndices);
            }
        } finally {
            MemoryUtil.memFree(internalVertices);
        }
        arenaUploadsPending = true;

        int handle = nextHandle++;
        if (nextHandle == 0) {
            nextHandle = 1;
        }
        HANDLES.put(handle, new MeshHandle(pass, indexCount, firstIndex, vertexOffset,
                boundMinX, boundMinY, boundMinZ, boundMaxX, boundMaxY, boundMaxZ, indexCount));
        lodSessionActive = true;
        return handle;
    }

    private static void drawTexturedMeshInternal(int handle, double cellOriginX, double cellOriginZ) {
        if (!beginLodDrawInternal(PASS_TEX_OPAQUE)) {
            return;
        }
        try {
            drawLodCellInternal(handle, cellOriginX, cellOriginZ);
        } finally {
            endLodDrawInternal();
        }
    }

    private static void drawTexturedWaterMeshInternal(int handle, double cellOriginX, double cellOriginZ) {
        if (!beginLodDrawInternal(PASS_TEX_WATER)) {
            return;
        }
        try {
            drawLodCellInternal(handle, cellOriginX, cellOriginZ);
        } finally {
            endLodDrawInternal();
        }
    }

    private static void drawWaterMeshInternal(int handle, double cellOriginX, double cellOriginZ) {
        if (!beginLodDrawInternal(PASS_FLAT_WATER)) {
            return;
        }
        try {
            drawLodCellInternal(handle, cellOriginX, cellOriginZ);
        } finally {
            endLodDrawInternal();
        }
    }

    private static short unorm16(float value) {
        float clamped = value < 0.0f ? 0.0f : (value > 1.0f ? 1.0f : value);
        return (short) Math.round(clamped * 65535.0f);
    }

    private static int quantizeXZ(float value) {
        long raw = Math.round(value);
        if (raw < 0) {
            raw = 0;
        } else if (raw > POSITION_MAX) {
            raw = POSITION_MAX;
        }
        return (int) raw;
    }

    private static int quantizeY(float value) {
        long raw = Math.round(value) + Y_BIAS;
        if (raw < 0) {
            raw = 0;
        } else if (raw > POSITION_MAX) {
            raw = POSITION_MAX;
        }
        return (int) raw;
    }

    private static void writeSharedUniforms() {
        FloatBuffer mvpSrc = VRenderSystem.getExternalLodMVP().buffer.asFloatBuffer();
        mvpSrc.position(0);
        COMBINED_MATRIX_SCRATCH.set(mvpSrc);

        FloatBuffer combinedDst = ExternalTerrainRenderBridge.getCombinedMatrix().buffer.asFloatBuffer();
        combinedDst.position(0);
        COMBINED_MATRIX_SCRATCH.get(combinedDst);

        float clip = clipDistance;
        MappedBuffer renderParams = ExternalTerrainRenderBridge.getRenderParams();
        renderParams.putFloat(0, 0.0f);
        renderParams.putFloat(4, clip > 0.0f ? clip : 0.0f);
        renderParams.putFloat(8, 0.0f);
        renderParams.putFloat(12, clip > 0.0f ? 1.0f : 0.0f);

        writeFogUniforms();
    }

    private static void writeFogUniforms() {
        float start = fogStartBlocks;
        float end = fogEndBlocks;
        if (!(end > start)) {
            start = Float.MAX_VALUE * 0.5f;
            end = Float.MAX_VALUE;
        }
        FOG_COLOR.putFloat(0, fogRed);
        FOG_COLOR.putFloat(4, fogGreen);
        FOG_COLOR.putFloat(8, fogBlue);
        FOG_COLOR.putFloat(12, 0.0f);
        FOG_PARAMS.putFloat(0, start);
        FOG_PARAMS.putFloat(4, end);
        FOG_PARAMS.putFloat(8, fadeBandStart);
        FOG_PARAMS.putFloat(12, fadeBandEnd);
    }

    private static void bindLightmap() {
        VulkanImage lightmap = VTextureSelector.getImage(LIGHTMAP_SOURCE_SLOT);
        if (lightmap != null) {
            VTextureSelector.bindTexture(LIGHTMAP_TEXTURE_SLOT, lightmap);
        }
    }

    private static boolean bindAtlas() {
        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        if (atlas == null) {
            return false;
        }

        GlTexture glTexture = GlTexture.getTexture(atlas.getId());
        VulkanImage atlasImage = glTexture != null ? glTexture.getVulkanImage() : null;
        if (atlasImage == null) {
            return false;
        }

        VTextureSelector.bindTexture(BLOCK_ATLAS_TEXTURE_SLOT, atlasImage);
        return true;
    }
}
