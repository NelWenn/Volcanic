package net.vulkanmod.vulkan.shader.pipeline.definitions;

import net.vulkanmod.vulkan.shader.pipeline.GfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.Sampler;
import net.vulkanmod.vulkan.shader.pipeline.Stage;
import net.vulkanmod.vulkan.shader.pipeline.Ubo;
import net.vulkanmod.vulkan.shader.pipeline.VertexFormatRef;

/** Bilateral upsample */
@GfxPipeline(basePath = "post_fog_composite", vertex = "post_fog_composite", fragment = "post_fog_composite", vertexFormat = VertexFormatRef.NONE)
public final class FogCompositePipeline extends FogPipelineBase implements PipelineDefinition {
    @Ubo(stage = Stage.FRAGMENT, binding = 0)
    static class FogUbo extends FogUniformSets.Full {
    }

    @Sampler(binding = 6) int Sampler5;
}
