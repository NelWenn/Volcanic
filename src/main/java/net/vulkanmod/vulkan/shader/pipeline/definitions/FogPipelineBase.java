package net.vulkanmod.vulkan.shader.pipeline.definitions;

import net.vulkanmod.vulkan.shader.pipeline.Sampler;

/** Fog pass ... */
abstract class FogPipelineBase {
    @Sampler(binding = 1) int Sampler0;
    @Sampler(binding = 2) int Sampler1;
    @Sampler(binding = 3) int Sampler2;
    @Sampler(binding = 4) int Sampler3;
    @Sampler(binding = 5) int Sampler4;
}
