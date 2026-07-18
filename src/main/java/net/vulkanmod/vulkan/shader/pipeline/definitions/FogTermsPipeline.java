package net.vulkanmod.vulkan.shader.pipeline.definitions;

import net.vulkanmod.vulkan.shader.pipeline.GfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.Stage;
import net.vulkanmod.vulkan.shader.pipeline.Ubo;
import net.vulkanmod.vulkan.shader.pipeline.VertexFormatRef;

/** Half-res shadow/fog/godray terms pass */
@GfxPipeline(basePath = "post_fog_terms", vertex = "post_fog_terms", fragment = "post_fog_terms", vertexFormat = VertexFormatRef.NONE)
public final class FogTermsPipeline extends FogPipelineBase implements PipelineDefinition {
    @Ubo(stage = Stage.FRAGMENT, binding = 0)
    static class FogUbo extends FogUniformSets.Core {
    }
}
