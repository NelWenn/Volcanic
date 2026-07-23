package net.vulkanmod.api;

import net.vulkanmod.Initializer;
import net.vulkanmod.compat.external.ExternalTerrainRenderBridge;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.IndexBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.VertexBuffer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public final class CalderaBridge {

    private static final int INPUT_VERTEX_SIZE_BYTES = 16;
    private static final int INTERNAL_VERTEX_SIZE_BYTES = CustomVertexFormat.EXTERNAL_LOD.getVertexSize();

    private static final int Y_BIAS = 2048;
    private static final int POSITION_MAX = 0xFFFF;

    private static final int FULL_BRIGHT_LIGHT_META = 0xFF;

    private static final Matrix4f COMBINED_MATRIX_SCRATCH = new Matrix4f();
    private static final int LIGHTMAP_TEXTURE_SLOT = 0;
    private static final int LIGHTMAP_SOURCE_SLOT = 2;

    private static VertexBuffer cachedVertexBuffer;
    private static IndexBuffer cachedIndexBuffer;

    private static int DIAG = 0;
    private static boolean indexBoundsWarned = false;

    private CalderaBridge() {
    }

    public static boolean isReady() {
        try {
            return Renderer.getInstance() != null && Renderer.isRecording();
        } catch (Throwable t) {
            return false;
        }
    }

    public static void submitMesh(ByteBuffer vertices, int vertexCount, ByteBuffer indices, int indexCount, boolean intIndices,
                                  double cellOriginX, double cellOriginZ, double camX, double camY, double camZ) {
        try {
            submitMeshInternal(vertices, vertexCount, indices, indexCount, intIndices, cellOriginX, cellOriginZ, camX, camY, camZ);
        } catch (Throwable t) {
            Initializer.LOGGER.error("[Caldera] submitMesh failed", t);
        }
    }

    private static void submitMeshInternal(ByteBuffer vertices, int vertexCount, ByteBuffer indices, int indexCount, boolean intIndices,
                                           double cellOriginX, double cellOriginZ, double camX, double camY, double camZ) {
        boolean diag = DIAG < 4;
        if (diag) {
            DIAG++;
            Initializer.LOGGER.info("[Caldera] submitMesh entry: ready={} vCount={} iCount={} pipelineNull={}",
                    isReady(), vertexCount, indexCount, PipelineManager.getExternalLodPipeline() == null);
        }
        if (!isReady()) {
            return;
        }

        if (vertices == null || indices == null || vertexCount <= 0 || indexCount <= 0 || indexCount % 3 != 0) {
            return;
        }

        GraphicsPipeline pipeline = PipelineManager.getExternalLodPipeline();
        if (pipeline == null) {
            return;
        }

        ByteBuffer vertexSrc = vertices.duplicate();
        vertexSrc.order(ByteOrder.LITTLE_ENDIAN);
        if (vertexSrc.remaining() < vertexCount * INPUT_VERTEX_SIZE_BYTES) {
            return;
        }

        int indexSize = intIndices ? 4 : 2;
        int indexBytesNeeded = indexCount * indexSize;
        ByteBuffer indexSrc = indices.duplicate();
        if (indexSrc.remaining() < indexBytesNeeded) {
            return;
        }

        ByteBuffer indexScan = indexSrc.duplicate();
        indexScan.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < indexCount; i++) {
            int idx = intIndices ? indexScan.getInt() : (indexScan.getShort() & 0xFFFF);
            if (idx < 0 || idx >= vertexCount) {
                if (!indexBoundsWarned) {
                    indexBoundsWarned = true;
                    Initializer.LOGGER.error("[Caldera] submitMesh: index {} out of bounds (vertexCount={})", idx, vertexCount);
                }
                return;
            }
        }

        ByteBuffer internalVertices = MemoryUtil.memAlloc(vertexCount * INTERNAL_VERTEX_SIZE_BYTES);
        try {
            internalVertices.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < vertexCount; i++) {
                float x = vertexSrc.getFloat();
                float y = vertexSrc.getFloat();
                float z = vertexSrc.getFloat();
                int c = vertexSrc.getInt();
                byte r = (byte) ((c >> 16) & 0xFF);
                byte g = (byte) ((c >>  8) & 0xFF);
                byte b = (byte) ( c        & 0xFF);
                byte a = (byte) ((c >>> 24) & 0xFF);

                internalVertices.putShort((short) quantizeXZ(x));
                internalVertices.putShort((short) quantizeY(y));
                internalVertices.putShort((short) quantizeXZ(z));
                internalVertices.putShort((short) FULL_BRIGHT_LIGHT_META);
                internalVertices.put(r).put(g).put(b).put(a);
                internalVertices.putInt(0);
            }
            internalVertices.position(0);

            byte[] indexBytes = new byte[indexBytesNeeded];
            indexSrc.get(indexBytes);

            ByteBuffer internalIndices = MemoryUtil.memAlloc(indexBytesNeeded);
            try {
                internalIndices.put(indexBytes);
                internalIndices.position(0);

                if (cachedVertexBuffer != null) {
                    cachedVertexBuffer.freeBuffer();
                }
                cachedVertexBuffer = new VertexBuffer(vertexCount * INTERNAL_VERTEX_SIZE_BYTES, MemoryTypes.HOST_MEM);
                cachedVertexBuffer.copyToVertexBuffer(INTERNAL_VERTEX_SIZE_BYTES, vertexCount, internalVertices);

                IndexBuffer.IndexType indexType = intIndices ? IndexBuffer.IndexType.INT : IndexBuffer.IndexType.SHORT;
                if (cachedIndexBuffer != null) {
                    cachedIndexBuffer.freeBuffer();
                }
                cachedIndexBuffer = new IndexBuffer(indexBytesNeeded, MemoryTypes.HOST_MEM, indexType);
                cachedIndexBuffer.copyBuffer(internalIndices);
            } finally {
                MemoryUtil.memFree(internalIndices);
            }
        } finally {
            MemoryUtil.memFree(internalVertices);
        }

        writeUniforms(cellOriginX, cellOriginZ, camX, camY, camZ);
        bindLightmap();

        Renderer renderer = Renderer.getInstance();
        if (!renderer.bindGraphicsPipeline(pipeline)) {
            if (diag) Initializer.LOGGER.info("[Caldera] submitMesh: bindGraphicsPipeline FAILED");
            return;
        }
        renderer.uploadAndBindUBOs(pipeline);
        Renderer.getDrawer().drawIndexed(cachedVertexBuffer, cachedIndexBuffer, indexCount);
        if (diag) Initializer.LOGGER.info("[Caldera] submitMesh: DREW {} indices", indexCount);
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

    private static void writeUniforms(double cellOriginX, double cellOriginZ, double camX, double camY, double camZ) {
        FloatBuffer mvpSrc = VRenderSystem.getMVP().buffer.asFloatBuffer();
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
}
