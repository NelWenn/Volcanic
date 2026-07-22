package net.vulkanmod.vulkan.shader.pipeline.definitions;

import net.vulkanmod.vulkan.shader.pipeline.GfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.Sampler;
import net.vulkanmod.vulkan.shader.pipeline.VertexFormatRef;

@GfxPipeline(basePath = "terrain", vertex = "terrain", fragment = "terrain", vertexFormat = VertexFormatRef.TERRAIN)
public final class TerrainPipeline extends TerrainPipelineBase implements PipelineDefinition {
    @Sampler(binding = 4) int Sampler4;
}
