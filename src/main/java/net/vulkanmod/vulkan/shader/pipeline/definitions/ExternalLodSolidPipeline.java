package net.vulkanmod.vulkan.shader.pipeline.definitions;

import net.vulkanmod.vulkan.shader.pipeline.GfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.PushConstantBlock;
import net.vulkanmod.vulkan.shader.pipeline.Sampler;
import net.vulkanmod.vulkan.shader.pipeline.Stage;
import net.vulkanmod.vulkan.shader.pipeline.Ubo;
import net.vulkanmod.vulkan.shader.pipeline.Uniform;
import net.vulkanmod.vulkan.shader.pipeline.VertexFormatRef;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@GfxPipeline(basePath = "external_lod_solid", vertex = "lod_solid", fragment = "lod_solid", vertexFormat = VertexFormatRef.EXTERNAL_LOD)
public final class ExternalLodSolidPipeline implements PipelineDefinition {

    @Ubo(stage = Stage.ALL, binding = 0)
    static class LodUbo {
        Matrix4f ExternalLodCombinedMatrix;
        Vector4f ExternalLodRenderParams;
        @Uniform(count = 4096)
        float[] ExternalLodCellOrigins;
    }

    @PushConstantBlock
    static class PushConstantsBlock {
        Vector4f ExternalLodModelOffsetAndYOffset;
    }

    @Sampler(binding = 1) int uLightMap;
}
