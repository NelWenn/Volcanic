package net.vulkanmod.rendergraph.core.plugin.pipeline;

import net.vulkanmod.vulkan.shader.pipeline.GfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.Sampler;
import net.vulkanmod.vulkan.shader.pipeline.VertexFormatRef;

@GfxPipeline(basePath = "vtu_present", vertex = "vtu_present", fragment = "vtu_present", vertexFormat = VertexFormatRef.NONE)
public final class VtuPresentPipeline implements PipelineDefinition {
    @Sampler(binding = 0) int Sampler0;
}
