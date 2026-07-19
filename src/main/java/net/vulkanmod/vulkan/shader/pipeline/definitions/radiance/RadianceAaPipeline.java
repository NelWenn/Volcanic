package net.vulkanmod.vulkan.shader.pipeline.definitions.radiance;

import net.vulkanmod.vulkan.shader.pipeline.GfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.Sampler;
import net.vulkanmod.vulkan.shader.pipeline.Stage;
import net.vulkanmod.vulkan.shader.pipeline.Ubo;
import net.vulkanmod.vulkan.shader.pipeline.VertexFormatRef;
import org.joml.Matrix4f;

@GfxPipeline(basePath = "radiance_aa", vertex = "radiance_aa", fragment = "radiance_aa", vertexFormat = VertexFormatRef.NONE)
public final class RadianceAaPipeline implements PipelineDefinition {
    @Ubo(stage = Stage.FRAGMENT, binding = 0)
    static class FragUbo {
        Matrix4f FogInvMVPMat;
        float AaMode;
    }

    @Sampler(binding = 1) int Sampler0;
    @Sampler(binding = 2) int Sampler1;
    @Sampler(binding = 3) int Sampler2;
}
