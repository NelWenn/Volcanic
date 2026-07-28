package net.vulkanmod.api;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.compat.external.ExternalTerrainRenderBridge;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.IndexBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.VertexBuffer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public final class CalderaBridge {

    private static final int INPUT_VERTEX_SIZE_BYTES = 16;
    private static final int INTERNAL_VERTEX_SIZE_BYTES = CustomVertexFormat.EXTERNAL_LOD.getVertexSize();

    private static final int INPUT_TEXTURED_VERTEX_SIZE_BYTES = 40;
    private static final int INTERNAL_TEXTURED_VERTEX_SIZE_BYTES = CustomVertexFormat.EXTERNAL_LOD_TEXTURED.getVertexSize();

    private static final int Y_BIAS = 2048;
    private static final int POSITION_MAX = 0xFFFF;

    private static final Matrix4f COMBINED_MATRIX_SCRATCH = new Matrix4f();
    private static final int LIGHTMAP_TEXTURE_SLOT = 0;
    private static final int LIGHTMAP_SOURCE_SLOT = 2;
    private static final int BLOCK_ATLAS_TEXTURE_SLOT = 3;

    private static final Int2ObjectOpenHashMap<MeshHandle> HANDLES = new Int2ObjectOpenHashMap<>();
    private static int nextHandle = 1;

    private static boolean indexBoundsWarned = false;

    private CalderaBridge() {
    }

    private record MeshHandle(VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int indexCount) {
    }

    public static boolean isReady() {
        try {
            return Renderer.getInstance() != null && Renderer.isRecording();
        } catch (Throwable t) {
            return false;
        }
    }

    public static int uploadMesh(ByteBuffer vertices, int vertexCount, ByteBuffer indices, int indexCount, boolean intIndices) {
        try {
            return uploadMeshInternal(vertices, vertexCount, indices, indexCount, intIndices);
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
                mesh.vertexBuffer().freeBuffer();
                mesh.indexBuffer().freeBuffer();
            }
        } catch (Throwable t) {
            Initializer.LOGGER.error("[Caldera] freeMesh failed", t);
        }
    }

    public static int uploadTexturedMesh(ByteBuffer vertices, int vertexCount, ByteBuffer indices, int indexCount, boolean intIndices) {
        try {
            return uploadTexturedMeshInternal(vertices, vertexCount, indices, indexCount, intIndices);
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
            return uploadTexturedMeshInternal(vertices, vertexCount, indices, indexCount, intIndices);
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
            return uploadMeshInternal(vertices, vertexCount, indices, indexCount, intIndices);
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

    private static int uploadMeshInternal(ByteBuffer vertices, int vertexCount, ByteBuffer indices, int indexCount, boolean intIndices) {
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

        VertexBuffer vertexBuffer;
        IndexBuffer indexBuffer;

        ByteBuffer internalVertices = MemoryUtil.memAlloc(vertexCount * INTERNAL_VERTEX_SIZE_BYTES);
        try {
            internalVertices.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < vertexCount; i++) {
                float x = vertexSrc.getFloat();
                float y = vertexSrc.getFloat();
                float z = vertexSrc.getFloat();
                int c = vertexSrc.getInt();
                int light = (c >>> 24) & 0xFF;
                byte r = (byte) ((c >> 16) & 0xFF);
                byte g = (byte) ((c >>  8) & 0xFF);
                byte b = (byte) ( c        & 0xFF);

                internalVertices.putShort((short) quantizeXZ(x));
                internalVertices.putShort((short) quantizeY(y));
                internalVertices.putShort((short) quantizeXZ(z));
                internalVertices.putShort((short) light);
                internalVertices.put(r).put(g).put(b).put((byte) 0xFF);
                internalVertices.putInt(0);
            }
            internalVertices.position(0);

            byte[] indexBytes = new byte[indexBytesNeeded];
            indexSrc.get(indexBytes);

            ByteBuffer internalIndices = MemoryUtil.memAlloc(indexBytesNeeded);
            try {
                internalIndices.put(indexBytes);
                internalIndices.position(0);

                vertexBuffer = new VertexBuffer(vertexCount * INTERNAL_VERTEX_SIZE_BYTES, MemoryTypes.HOST_MEM);
                vertexBuffer.copyToVertexBuffer(INTERNAL_VERTEX_SIZE_BYTES, vertexCount, internalVertices);

                IndexBuffer.IndexType indexType = intIndices ? IndexBuffer.IndexType.INT : IndexBuffer.IndexType.SHORT;
                indexBuffer = new IndexBuffer(indexBytesNeeded, MemoryTypes.HOST_MEM, indexType);
                indexBuffer.copyBuffer(internalIndices);
            } finally {
                MemoryUtil.memFree(internalIndices);
            }
        } finally {
            MemoryUtil.memFree(internalVertices);
        }

        int handle = nextHandle++;
        if (nextHandle == 0) {
            nextHandle = 1;
        }
        HANDLES.put(handle, new MeshHandle(vertexBuffer, indexBuffer, indexCount));
        return handle;
    }

    private static void drawMeshInternal(int handle, double cellOriginX, double cellOriginZ) {
        if (!isReady()) {
            return;
        }

        MeshHandle mesh = HANDLES.get(handle);
        if (mesh == null) {
            return;
        }

        GraphicsPipeline pipeline = PipelineManager.getExternalLodPipeline();
        if (pipeline == null) {
            return;
        }

        writeUniforms(cellOriginX, cellOriginZ);

        final VulkanImage prevSlot0 = VTextureSelector.getImage(LIGHTMAP_TEXTURE_SLOT);
        bindLightmap();

        try {
            Renderer renderer = Renderer.getInstance();
            if (!renderer.bindGraphicsPipeline(pipeline)) {
                return;
            }
            renderer.uploadAndBindUBOs(pipeline);
            Renderer.getDrawer().drawIndexed(mesh.vertexBuffer(), mesh.indexBuffer(), mesh.indexCount());
        } finally {
            VTextureSelector.bindTexture(LIGHTMAP_TEXTURE_SLOT, prevSlot0);
        }
    }

    private static int uploadTexturedMeshInternal(ByteBuffer vertices, int vertexCount, ByteBuffer indices, int indexCount, boolean intIndices) {
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

        VertexBuffer vertexBuffer;
        IndexBuffer indexBuffer;

        ByteBuffer internalVertices = MemoryUtil.memAlloc(vertexCount * INTERNAL_TEXTURED_VERTEX_SIZE_BYTES);
        try {
            internalVertices.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < vertexCount; i++) {
                float x = vertexSrc.getFloat();
                float y = vertexSrc.getFloat();
                float z = vertexSrc.getFloat();
                int c = vertexSrc.getInt();
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

            byte[] indexBytes = new byte[indexBytesNeeded];
            indexSrc.get(indexBytes);

            ByteBuffer internalIndices = MemoryUtil.memAlloc(indexBytesNeeded);
            try {
                internalIndices.put(indexBytes);
                internalIndices.position(0);

                vertexBuffer = new VertexBuffer(vertexCount * INTERNAL_TEXTURED_VERTEX_SIZE_BYTES, MemoryTypes.HOST_MEM);
                vertexBuffer.copyToVertexBuffer(INTERNAL_TEXTURED_VERTEX_SIZE_BYTES, vertexCount, internalVertices);

                IndexBuffer.IndexType indexType = intIndices ? IndexBuffer.IndexType.INT : IndexBuffer.IndexType.SHORT;
                indexBuffer = new IndexBuffer(indexBytesNeeded, MemoryTypes.HOST_MEM, indexType);
                indexBuffer.copyBuffer(internalIndices);
            } finally {
                MemoryUtil.memFree(internalIndices);
            }
        } finally {
            MemoryUtil.memFree(internalVertices);
        }

        int handle = nextHandle++;
        if (nextHandle == 0) {
            nextHandle = 1;
        }
        HANDLES.put(handle, new MeshHandle(vertexBuffer, indexBuffer, indexCount));
        return handle;
    }

    private static void drawTexturedMeshInternal(int handle, double cellOriginX, double cellOriginZ) {
        if (!isReady()) {
            return;
        }

        MeshHandle mesh = HANDLES.get(handle);
        if (mesh == null) {
            return;
        }

        GraphicsPipeline pipeline = PipelineManager.getExternalLodTexturedPipeline();
        if (pipeline == null) {
            return;
        }

        writeUniforms(cellOriginX, cellOriginZ);

        final VulkanImage prevSlot0 = VTextureSelector.getImage(LIGHTMAP_TEXTURE_SLOT);
        final VulkanImage prevSlot3 = VTextureSelector.getImage(BLOCK_ATLAS_TEXTURE_SLOT);
        bindLightmap();

        try {
            if (!bindAtlas()) {
                return;
            }

            Renderer renderer = Renderer.getInstance();
            if (!renderer.bindGraphicsPipeline(pipeline)) {
                return;
            }
            renderer.uploadAndBindUBOs(pipeline);
            Renderer.getDrawer().drawIndexed(mesh.vertexBuffer(), mesh.indexBuffer(), mesh.indexCount());
        } finally {
            VTextureSelector.bindTexture(LIGHTMAP_TEXTURE_SLOT, prevSlot0);
            VTextureSelector.bindTexture(BLOCK_ATLAS_TEXTURE_SLOT, prevSlot3);
        }
    }

    private static void drawTexturedWaterMeshInternal(int handle, double cellOriginX, double cellOriginZ) {
        if (!isReady()) {
            return;
        }

        MeshHandle mesh = HANDLES.get(handle);
        if (mesh == null) {
            return;
        }

        GraphicsPipeline pipeline = PipelineManager.getExternalLodWaterTexturedPipeline();
        if (pipeline == null) {
            return;
        }

        writeUniforms(cellOriginX, cellOriginZ);

        final VulkanImage prevSlot0 = VTextureSelector.getImage(LIGHTMAP_TEXTURE_SLOT);
        final VulkanImage prevSlot3 = VTextureSelector.getImage(BLOCK_ATLAS_TEXTURE_SLOT);
        bindLightmap();

        final boolean sDepthTest = VRenderSystem.depthTest, sDepthMask = VRenderSystem.depthMask;
        final PipelineState.BlendInfo bi = PipelineState.blendInfo;
        final boolean sBlendEnabled = bi.enabled;
        final int sSrcRgb = bi.srcRgbFactor, sDstRgb = bi.dstRgbFactor, sSrcA = bi.srcAlphaFactor, sDstA = bi.dstAlphaFactor;
        final int sBlendOp = bi.blendOp, sBlendOpRgb = bi.blendOpRgb, sBlendOpAlpha = bi.blendOpAlpha;

        VRenderSystem.depthTest = true;
        VRenderSystem.depthMask = false;
        bi.enabled = true;
        bi.srcRgbFactor = VK10.VK_BLEND_FACTOR_SRC_ALPHA;
        bi.dstRgbFactor = VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
        bi.srcAlphaFactor = VK10.VK_BLEND_FACTOR_ONE;
        bi.dstAlphaFactor = VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
        bi.blendOp = bi.blendOpRgb = bi.blendOpAlpha = VK10.VK_BLEND_OP_ADD;

        try {
            if (bindAtlas()) {
                Renderer renderer = Renderer.getInstance();
                if (renderer.bindGraphicsPipeline(pipeline)) {
                    renderer.uploadAndBindUBOs(pipeline);
                    Renderer.getDrawer().drawIndexed(mesh.vertexBuffer(), mesh.indexBuffer(), mesh.indexCount());
                }
            }
        } finally {
            VRenderSystem.depthTest = sDepthTest;
            VRenderSystem.depthMask = sDepthMask;
            bi.enabled = sBlendEnabled;
            bi.srcRgbFactor = sSrcRgb;
            bi.dstRgbFactor = sDstRgb;
            bi.srcAlphaFactor = sSrcA;
            bi.dstAlphaFactor = sDstA;
            bi.blendOp = sBlendOp;
            bi.blendOpRgb = sBlendOpRgb;
            bi.blendOpAlpha = sBlendOpAlpha;
            VTextureSelector.bindTexture(LIGHTMAP_TEXTURE_SLOT, prevSlot0);
            VTextureSelector.bindTexture(BLOCK_ATLAS_TEXTURE_SLOT, prevSlot3);
        }
    }

    private static void drawWaterMeshInternal(int handle, double cellOriginX, double cellOriginZ) {
        if (!isReady()) {
            return;
        }

        MeshHandle mesh = HANDLES.get(handle);
        if (mesh == null) {
            return;
        }

        GraphicsPipeline pipeline = PipelineManager.getExternalLodWaterPipeline();
        if (pipeline == null) {
            return;
        }

        writeUniforms(cellOriginX, cellOriginZ);

        final VulkanImage prevSlot0 = VTextureSelector.getImage(LIGHTMAP_TEXTURE_SLOT);
        bindLightmap();

        final boolean sDepthTest = VRenderSystem.depthTest, sDepthMask = VRenderSystem.depthMask;
        final PipelineState.BlendInfo bi = PipelineState.blendInfo;
        final boolean sBlendEnabled = bi.enabled;
        final int sSrcRgb = bi.srcRgbFactor, sDstRgb = bi.dstRgbFactor, sSrcA = bi.srcAlphaFactor, sDstA = bi.dstAlphaFactor;
        final int sBlendOp = bi.blendOp, sBlendOpRgb = bi.blendOpRgb, sBlendOpAlpha = bi.blendOpAlpha;

        VRenderSystem.depthTest = true;
        VRenderSystem.depthMask = false;
        bi.enabled = true;
        bi.srcRgbFactor = VK10.VK_BLEND_FACTOR_SRC_ALPHA;
        bi.dstRgbFactor = VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
        bi.srcAlphaFactor = VK10.VK_BLEND_FACTOR_ONE;
        bi.dstAlphaFactor = VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
        bi.blendOp = bi.blendOpRgb = bi.blendOpAlpha = VK10.VK_BLEND_OP_ADD;

        try {
            Renderer renderer = Renderer.getInstance();
            if (renderer.bindGraphicsPipeline(pipeline)) {
                renderer.uploadAndBindUBOs(pipeline);
                Renderer.getDrawer().drawIndexed(mesh.vertexBuffer(), mesh.indexBuffer(), mesh.indexCount());
            }
        } finally {
            VRenderSystem.depthTest = sDepthTest;
            VRenderSystem.depthMask = sDepthMask;
            bi.enabled = sBlendEnabled;
            bi.srcRgbFactor = sSrcRgb;
            bi.dstRgbFactor = sDstRgb;
            bi.srcAlphaFactor = sSrcA;
            bi.dstAlphaFactor = sDstA;
            bi.blendOp = sBlendOp;
            bi.blendOpRgb = sBlendOpRgb;
            bi.blendOpAlpha = sBlendOpAlpha;
            VTextureSelector.bindTexture(LIGHTMAP_TEXTURE_SLOT, prevSlot0);
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

    private static void writeUniforms(double cellOriginX, double cellOriginZ) {
        Vec3 liveCam = WorldRenderer.getCameraPos();
        double camX = liveCam != null ? liveCam.x() : 0.0;
        double camY = liveCam != null ? liveCam.y() : 0.0;
        double camZ = liveCam != null ? liveCam.z() : 0.0;

        FloatBuffer mvpSrc = VRenderSystem.getExternalLodMVP().buffer.asFloatBuffer();
        mvpSrc.position(0);
        COMBINED_MATRIX_SCRATCH.set(mvpSrc);

        FloatBuffer combinedDst = ExternalTerrainRenderBridge.getCombinedMatrix().buffer.asFloatBuffer();
        combinedDst.position(0);
        COMBINED_MATRIX_SCRATCH.get(combinedDst);

        MappedBuffer modelOffset = ExternalTerrainRenderBridge.getModelOffsetAndYOffset();
        modelOffset.putFloat(0, (float) (cellOriginX - camX));
        modelOffset.putFloat(4, (float) (-camY - Y_BIAS));
        modelOffset.putFloat(8, (float) (cellOriginZ - camZ));
        modelOffset.putFloat(12, 0.0f);

        MappedBuffer renderParams = ExternalTerrainRenderBridge.getRenderParams();
        renderParams.putFloat(0, 0.0f);
        renderParams.putFloat(4, 0.0f);
        renderParams.putFloat(8, 0.0f);
        renderParams.putFloat(12, 0.0f);
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
