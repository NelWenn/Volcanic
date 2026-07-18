package net.vulkanmod.vulkan.shader.pipeline.definitions;

import net.vulkanmod.vulkan.shader.pipeline.GfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.VertexFormatRef;

@GfxPipeline(basePath = "shadow_terrain", vertex = "shadow_terrain", fragment = "shadow_terrain_rsm", vertexFormat = VertexFormatRef.TERRAIN)
public final class ShadowTerrainRsmPipeline extends ShadowTerrainPipelineBase implements PipelineDefinition {
}
