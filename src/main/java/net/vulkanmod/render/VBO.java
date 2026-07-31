package net.vulkanmod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.AutoIndexBuffer;
import net.vulkanmod.vulkan.memory.IndexBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.VertexBuffer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

@OnlyIn(Dist.CLIENT)
public class VBO {
    private VertexBuffer vertexBuffer;
    private IndexBuffer indexBuffer;

    private int indexCount;
    private int vertexCount;
    private VertexFormat.Mode mode;
    private VertexFormat format;

    private boolean autoIndexed = false;

    public VBO() {}

    public void upload(MeshData meshData) {
        MeshData.DrawState parameters = meshData.drawState();

        this.indexCount = parameters.indexCount();
        this.vertexCount = parameters.vertexCount();
        this.mode = parameters.mode();
        this.format = parameters.format();

        this.uploadVertexBuffer(parameters, meshData.vertexBuffer());
        this.uploadIndexBuffer(meshData.indexBuffer());

        meshData.close();
    }

    private void uploadVertexBuffer(MeshData.DrawState parameters, ByteBuffer data) {
        if (data != null) {
            if (this.vertexBuffer != null)
                this.vertexBuffer.freeBuffer();

            this.vertexBuffer = new VertexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
            this.vertexBuffer.copyToVertexBuffer(parameters.format().getVertexSize(), parameters.vertexCount(), data);
        }
    }

    public void uploadIndexBuffer(ByteBuffer data) {
        if (data == null) {

            AutoIndexBuffer autoIndexBuffer;
            switch (this.mode) {
                case TRIANGLE_FAN -> {
                    autoIndexBuffer = Renderer.getDrawer().getTriangleFanIndexBuffer();
                    this.indexCount = AutoIndexBuffer.DrawType.getTriangleStripIndexCount(this.vertexCount);
                }
                case TRIANGLE_STRIP -> {
                    autoIndexBuffer = Renderer.getDrawer().getTriangleStripIndexBuffer();
                    this.indexCount = AutoIndexBuffer.DrawType.getTriangleStripIndexCount(this.vertexCount);
                }
                case LINE_STRIP -> {
                    autoIndexBuffer = Renderer.getDrawer().getTriangleStripIndexBuffer();
                    this.indexCount = AutoIndexBuffer.DrawType.getTriangleStripIndexCount(this.vertexCount);
                }
                case QUADS -> {
                    autoIndexBuffer = Renderer.getDrawer().getQuadsIndexBuffer();
                }
                case LINES -> {
                    autoIndexBuffer = Renderer.getDrawer().getLinesIndexBuffer();
                }
                case DEBUG_LINE_STRIP -> {
                    autoIndexBuffer = Renderer.getDrawer().getDebugLineStripIndexBuffer();
                }
                case TRIANGLES, DEBUG_LINES -> {
                    autoIndexBuffer = null;
                }
                default -> throw new IllegalStateException("Unexpected draw mode: %s".formatted(this.mode));
            }

            if (this.indexBuffer != null && !this.autoIndexed)
                this.indexBuffer.freeBuffer();

            if (autoIndexBuffer != null) {
                autoIndexBuffer.checkCapacity(this.vertexCount);
                this.indexBuffer = autoIndexBuffer.getIndexBuffer();
            }

            this.autoIndexed = true;

        } else {
            if (this.indexBuffer != null)
                this.indexBuffer.freeBuffer();

            this.indexBuffer = new IndexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
            this.indexBuffer.copyBuffer(data);
        }

    }

    public void drawWithShader(Matrix4f MV, Matrix4f P, ShaderInstance shader) {
        if (this.indexCount != 0) {
            RenderSystem.assertOnRenderThread();

            RenderSystem.setShader(() -> shader);

            drawWithShader(MV, P, ((ShaderMixed) shader).getPipeline(this.format));

        }
    }

    public void drawWithShader(Matrix4f MV, Matrix4f P, GraphicsPipeline pipeline) {
        if (this.indexCount != 0) {
            if (pipeline == null) {
                return;
            }
            RenderSystem.assertOnRenderThread();

            VRenderSystem.applyMVP(MV, P);

            VRenderSystem.setPrimitiveTopology(this.mode);

            Renderer renderer = Renderer.getInstance();
            renderer.bindGraphicsPipeline(pipeline);
            VTextureSelector.bindShaderTextures(pipeline);
            renderer.uploadAndBindUBOs(pipeline);

            if (this.indexBuffer != null)
                Renderer.getDrawer().drawIndexed(this.vertexBuffer, this.indexBuffer, this.indexCount);
            else
                Renderer.getDrawer().draw(this.vertexBuffer, this.vertexCount);

            VRenderSystem.applyMVP(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());

        }
    }

    public void draw() {
        if (this.indexCount != 0) {
            RenderSystem.assertOnRenderThread();

            Renderer renderer = Renderer.getInstance();

            ShaderInstance shader = RenderSystem.getShader();
            if (shader != null) {
                GraphicsPipeline shaderPipeline = ((ShaderMixed) shader).getPipeline(this.format);
                boolean blockFormat = this.format == com.mojang.blaze3d.vertex.DefaultVertexFormat.BLOCK;
                if (shaderPipeline != null && (blockFormat || shaderPipeline != renderer.getBoundPipeline())) {
                    Matrix4f MV = uniformMatrix(shader.MODEL_VIEW_MATRIX, RenderSystem.getModelViewMatrix());
                    applyChunkOffset(MV, shader);
                    Matrix4f P = uniformMatrix(shader.PROJECTION_MATRIX, RenderSystem.getProjectionMatrix());
                    this.drawWithShader(MV, P, shaderPipeline);
                    return;
                }
            }

            Pipeline pipeline = renderer.getBoundPipeline();
            renderer.uploadAndBindUBOs(pipeline);

            Renderer.getDrawer().drawIndexed(this.vertexBuffer, this.indexBuffer, this.indexCount);
        }
    }

    private static Matrix4f uniformMatrix(com.mojang.blaze3d.shaders.Uniform uniform, Matrix4f fallback) {
        if (uniform != null) {
            java.nio.FloatBuffer buffer = uniform.getFloatBuffer();
            if (buffer != null && buffer.capacity() >= 16)
                return new Matrix4f().set(buffer);
        }

        return new Matrix4f(fallback);
    }

    private static void applyChunkOffset(Matrix4f modelView, ShaderInstance shader) {
        com.mojang.blaze3d.shaders.Uniform chunkOffset = shader.getUniform("ChunkOffset");
        if (chunkOffset == null)
            return;

        java.nio.FloatBuffer buffer = chunkOffset.getFloatBuffer();
        if (buffer == null || buffer.capacity() < 3)
            return;

        float x = buffer.get(0);
        float y = buffer.get(1);
        float z = buffer.get(2);
        if (x != 0.0f || y != 0.0f || z != 0.0f)
            modelView.translate(x, y, z);
    }

    public void close() {
        if (this.vertexCount <= 0)
            return;

        this.vertexBuffer.freeBuffer();
        this.vertexBuffer = null;

        if (!this.autoIndexed) {
            this.indexBuffer.freeBuffer();
            this.indexBuffer = null;
        }

        this.vertexCount = 0;
        this.indexCount = 0;
        this.format = null;
    }

}
