package net.vulkanmod.rendergraph.core.plugin.pipeline.lod;

import net.vulkanmod.vulkan.shader.pipeline.*;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@GfxPipeline(basePath = "external_lod", vertex = "lod", fragment = "lod", vertexFormat = VertexFormatRef.EXTERNAL_LOD)
public final class ExternalLodPipeline implements PipelineDefinition {

    @Ubo(stage = Stage.ALL, binding = 0)
    static class LodUbo {
        Matrix4f ExternalLodCombinedMatrix;
        Vector4f ExternalLodRenderParams;
        Vector4f ExternalLodFogColor;
        Vector4f ExternalLodFogParams;
        @Uniform(count = 4096)
        float[] ExternalLodCellOrigins;
    }

    @PushConstantBlock
    static class PushConstantsBlock {
        Vector4f ExternalLodModelOffsetAndYOffset;
    }

    @Sampler(binding = 1) int uLightMap;
}
