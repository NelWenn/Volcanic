package net.vulkanmod.vulkan.shader.pipeline.definitions;

import net.vulkanmod.vulkan.shader.pipeline.GfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.Sampler;
import net.vulkanmod.vulkan.shader.pipeline.VertexFormatRef;

@GfxPipeline(basePath = "render_scale_blit", vertex = "render_scale_blit", fragment = "render_scale_blit", vertexFormat = VertexFormatRef.NONE)
public final class RenderScaleBlitPipeline implements PipelineDefinition {
    @Sampler(binding = 0) int Sampler0;
}
