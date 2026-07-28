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

@GfxPipeline(basePath = "external_lod_water_tex", vertex = "lod_water_tex", fragment = "lod_water_tex", vertexFormat = VertexFormatRef.EXTERNAL_LOD_TEXTURED)
public final class ExternalLodWaterTexturedPipeline implements PipelineDefinition {

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
    @Sampler(binding = 2) int uBlockAtlas;
}
