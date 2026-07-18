package net.vulkanmod.vulkan.shader.pipeline.definitions;

import net.vulkanmod.vulkan.shader.pipeline.GfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.Stage;
import net.vulkanmod.vulkan.shader.pipeline.Ubo;
import net.vulkanmod.vulkan.shader.pipeline.VertexFormatRef;

/** Fog Gfx pipeline */
@GfxPipeline(basePath = "post_fog", vertex = "post_fog", fragment = "post_fog", vertexFormat = VertexFormatRef.NONE)
public final class FogPipeline extends FogPipelineBase implements PipelineDefinition {
    @Ubo(stage = Stage.FRAGMENT, binding = 0)
    static class FogUbo extends FogUniformSets.Full {
    }
}
