package net.vulkanmod.vulkan.shader.pipeline.definitions;

import net.vulkanmod.vulkan.shader.pipeline.GfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.VertexFormatRef;

@GfxPipeline(basePath = "terrain", vertex = "terrain", fragment = "terrain_z", vertexFormat = VertexFormatRef.TERRAIN)
public final class TerrainEarlyZPipeline extends TerrainPipelineBase implements PipelineDefinition {
}
