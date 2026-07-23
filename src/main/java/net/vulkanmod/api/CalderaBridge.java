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

    private static final float POSITION_QUANTIZATION_SCALE = 1.0f / 16.0f;
    private static final float POSITION_QUANTIZATION_INV_SCALE = 16.0f;
    private static final int POSITION_BIAS = 32768;
    private static final int POSITION_MAX = 0xFFFF;

    private static final int FULL_BRIGHT_LIGHT_META = 0xFF;

    private static final Matrix4f COMBINED_MATRIX_SCRATCH = new Matrix4f();
    private static final int LIGHTMAP_TEXTURE_SLOT = 0;
    private static final int LIGHTMAP_SOURCE_SLOT = 2;

    private static VertexBuffer cachedVertexBuffer;
    private static IndexBuffer cachedIndexBuffer;

    private CalderaBridge() {
    }

    public static boolean isReady() {
        try {
            return Renderer.getInstance() != null && Renderer.isRecording();
        } catch (Throwable t) {
            return false;
        }
    }

    public static void submitMesh(ByteBuffer vertices, int vertexCount, ByteBuffer indices, int indexCount, boolean intIndices) {
        try {
            submitMeshInternal(vertices, vertexCount, indices, indexCount, intIndices);
        } catch (Throwable t) {
            Initializer.LOGGER.error("[Caldera] submitMesh failed", t);
        }
    }

    private static void submitMeshInternal(ByteBuffer vertices, int vertexCount, ByteBuffer indices, int indexCount, boolean intIndices) {
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

        ByteBuffer internalVertices = MemoryUtil.memAlloc(vertexCount * INTERNAL_VERTEX_SIZE_BYTES);
        try {
            internalVertices.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < vertexCount; i++) {
                float x = vertexSrc.getFloat();
                float y = vertexSrc.getFloat();
                float z = vertexSrc.getFloat();
                byte r = vertexSrc.get();
                byte g = vertexSrc.get();
                byte b = vertexSrc.get();
                byte a = vertexSrc.get();

                internalVertices.putShort((short) quantize(x));
                internalVertices.putShort((short) quantize(y));
                internalVertices.putShort((short) quantize(z));
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

        writeUniforms();
        bindLightmap();

        Renderer renderer = Renderer.getInstance();
        if (!renderer.bindGraphicsPipeline(pipeline)) {
            return;
        }
        renderer.uploadAndBindUBOs(pipeline);
        Renderer.getDrawer().drawIndexed(cachedVertexBuffer, cachedIndexBuffer, indexCount);
    }

    private static int quantize(float value) {
        long raw = Math.round(value * POSITION_QUANTIZATION_INV_SCALE) + POSITION_BIAS;
        if (raw < 0) {
            raw = 0;
        } else if (raw > POSITION_MAX) {
            raw = POSITION_MAX;
        }
        return (int) raw;
    }

    private static void writeUniforms() {
        FloatBuffer mvpSrc = VRenderSystem.getMVP().buffer.asFloatBuffer();
        mvpSrc.position(0);
        COMBINED_MATRIX_SCRATCH.set(mvpSrc);
        COMBINED_MATRIX_SCRATCH.scale(POSITION_QUANTIZATION_SCALE);

        FloatBuffer combinedDst = ExternalTerrainRenderBridge.getCombinedMatrix().buffer.asFloatBuffer();
        combinedDst.position(0);
        COMBINED_MATRIX_SCRATCH.get(combinedDst);

        MappedBuffer modelOffset = ExternalTerrainRenderBridge.getModelOffsetAndYOffset();
        modelOffset.putFloat(0, -POSITION_BIAS);
        modelOffset.putFloat(4, -POSITION_BIAS);
        modelOffset.putFloat(8, -POSITION_BIAS);
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
