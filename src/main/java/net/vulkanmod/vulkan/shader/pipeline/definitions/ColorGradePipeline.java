package net.vulkanmod.vulkan.shader.pipeline.definitions;

import net.vulkanmod.vulkan.shader.pipeline.GfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.Sampler;
import net.vulkanmod.vulkan.shader.pipeline.Stage;
import net.vulkanmod.vulkan.shader.pipeline.Ubo;
import net.vulkanmod.vulkan.shader.pipeline.VertexFormatRef;

@GfxPipeline(basePath = "post_color_grade", vertex = "post_color_grade", fragment = "post_color_grade", vertexFormat = VertexFormatRef.NONE)
public final class ColorGradePipeline implements PipelineDefinition {

    @Ubo(stage = Stage.FRAGMENT, binding = 0)
    static class CgUbo {
        float CgExposure;
        float CgContrast;
        float CgSaturation;
        float CgTemperature;
    }

    @Sampler(binding = 1) int Sampler0;
}
