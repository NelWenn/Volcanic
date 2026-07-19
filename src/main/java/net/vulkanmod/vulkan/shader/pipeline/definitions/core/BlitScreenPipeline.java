package net.vulkanmod.vulkan.shader.pipeline.definitions.core;

import net.vulkanmod.vulkan.shader.pipeline.CoreGfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.Sampler;

@CoreGfxPipeline(name = "blit_screen")
public final class BlitScreenPipeline implements PipelineDefinition {
    @Sampler(binding = 0) int DiffuseSampler;
}
