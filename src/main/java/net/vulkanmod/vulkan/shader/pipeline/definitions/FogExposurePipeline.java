package net.vulkanmod.vulkan.shader.pipeline.definitions;

import net.vulkanmod.vulkan.shader.pipeline.GfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.Stage;
import net.vulkanmod.vulkan.shader.pipeline.Ubo;
import net.vulkanmod.vulkan.shader.pipeline.VertexFormatRef;

/** auto exposure accumulation pass. */
@GfxPipeline(basePath = "post_exposure", vertex = "post_exposure", fragment = "post_exposure", vertexFormat = VertexFormatRef.NONE)
public final class FogExposurePipeline extends FogPipelineBase implements PipelineDefinition {
    @Ubo(stage = Stage.FRAGMENT, binding = 0)
    static class FogUbo extends FogUniformSets.Full {
    }
}
